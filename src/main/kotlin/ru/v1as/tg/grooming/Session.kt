package ru.v1as.tg.grooming

import ru.v1as.tg.starter.model.TgUser
import java.time.LocalDateTime

const val TURN_OVER = "\uD83C\uDCCF\uD83D\uDD04"

const val COFFEE = "☕"

const val WAITING = "⏳"

const val CARD = "\uD83C\uDCCF"

val VALUES = listOf("1", "2", "3", "5", "8", "13", "21", COFFEE, TURN_OVER)

class Session(val title: String, voters: Set<TgUser> = emptySet()) {
    private var votes: MutableMap<TgUser, Vote?> = mutableMapOf()
    var closed = false
    var messageId = -1

    init {
        voters.forEach { votes[it] = null }
    }

    fun voters() = votes.keys

    fun close() {
        if (!closed) {
            closed = true
            votes = votes.filter { it.value != null }.toMutableMap()
        } else {
            throw IllegalStateException("Already closed")
        }
    }

    fun text(): String {
        val votesStr =
            votes
                .map {
                    val vote =
                        if (closed) {
                            ": " + it.value?.value.orEmpty()
                        } else {
                            " " + (it.value?.let { CARD } ?: WAITING)
                        }
                    it.key.usernameOrFullName() + vote
                }
                .joinToString("\n")

        val voteResult =
            if (closed) {
                val avgVote =
                    votes.values
                        .stream()
                        .map { it?.value }
                        .map { it?.toIntOrNull() }
                        .filter { it != null }
                        .mapToInt { it!! }
                        .average()
                        .orElse(.0)
                String.format("Итог: %.2f", avgVote)
            } else {
                ""
            }

        return title + "\n\n" + votesStr + "\n\n" + voteResult
    }

    fun vote(value: String, user: TgUser): Voted {
        if (value == TURN_OVER) {
            close()
            return Voted.CLOSED
        }
        val prev = votes[user]
        return if (prev?.value == value) {
            votes[user] = null
            Voted.CLEARED
        } else {
            votes[user] = Vote(value)
            if (prev?.value == null) {
                Voted.VOTED
            } else {
                Voted.CHANGED
            }
        }
    }
}

data class Vote(val value: String, val time: LocalDateTime = LocalDateTime.now())
