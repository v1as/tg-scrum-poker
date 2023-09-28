package ru.v1as.tg.grooming.model

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import ru.v1as.tg.starter.model.TgUser

const val TURN_OVER = "\uD83C\uDCCF\uD83D\uDD04"

const val COFFEE = "☕"

const val WAITING = "⏳"

const val CARD = "\uD83C\uDCCF"

class Session(
    private val title: String,
    voters: Set<TgUser> = emptySet(),
    val started: LocalDateTime = now()
) {
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
                .toList()
                .sortedWith((compareBy(nullsLast()) { (_, value) -> value }))
                .toMap()
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

        val duration =
            if (closed) {
                Duration.between(started, now())
                    .toMinutes()
                    .takeIf { it > 0 }
                    ?.let { "Голосовали $it минут(ы)\n" }
                    ?: ""
            } else {
                ""
            }

        return title + "\n\n" + votesStr + "\n\n" + duration + voteResult
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

data class Vote(val value: String, val time: LocalDateTime = now()) : Comparable<Vote> {
    override fun compareTo(other: Vote) = this.time.compareTo(other.time)
}
