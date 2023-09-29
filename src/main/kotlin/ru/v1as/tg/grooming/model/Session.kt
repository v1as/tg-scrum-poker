package ru.v1as.tg.grooming.model

import ru.v1as.tg.grooming.update.intVoteValues
import ru.v1as.tg.starter.model.TgUser
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import kotlin.math.abs

const val TURN_OVER = "\uD83C\uDCCF\uD83D\uDD04"

const val COFFEE = "☕"

const val WAITING = "⏳"

const val CARD = "\uD83C\uDCCF"

class Session(
    val title: String,
    voters: Set<TgUser> = emptySet(),
    private val started: LocalDateTime = now()
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
        return listOf("\uD83D\uDCDD $title", votesString(), durationString(), voteResultString())
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    fun needDiscussion(): Boolean {
        val max = voteValueStream().max().orElse(0)
        val min = voteValueStream().min().orElse(0)
        return intVoteValues().count { it in min..max } > 2
    }

    private fun durationString() =
        if (closed) {
            Duration.between(started, now())
                .toMinutes()
                .takeIf { it > 0 }
                ?.let { "⏲ Голосовали $it минут(ы)" }
                .orEmpty()
        } else {
            ""
        }

    private fun voteResultString() =
        if (closed) {
            val avgVote = voteValueStream().average().orElse(.0)
            val template =
                if (voteValueStream().distinct().count() == 1L) {
                    "\uD83C\uDF89 Единогласно: %.2f"
                } else {
                    val bestMatch =
                        intVoteValues()
                            .map { v -> v to abs((v - avgVote)) }
                            .sortedBy { it.second }
                            .reversed()
                            .distinctBy { it.second }
                            .map { it.first }
                            .last()
                    "⚖ Итог: %.1f  ~  $bestMatch"
                }
            String.format(template, avgVote)
        } else {
            ""
        }

    private fun voteValueStream() =
        votes.values
            .stream()
            .map { it?.value?.toIntOrNull() }
            .filter { it != null }
            .mapToInt { it!! }

    private fun votesString() =
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
