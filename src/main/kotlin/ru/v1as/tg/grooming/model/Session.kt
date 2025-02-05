package ru.v1as.tg.grooming.model

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.roundToInt
import ru.v1as.tg.grooming.model.Voted.CHANGED
import ru.v1as.tg.grooming.model.Voted.CLEARED
import ru.v1as.tg.grooming.model.Voted.NONE
import ru.v1as.tg.grooming.model.Voted.VOTED
import ru.v1as.tg.grooming.update.intVoteValues
import ru.v1as.tg.starter.model.TgUser

const val TURN_OVER = "\uD83C\uDCCF\uD83D\uDD04"

const val COFFEE = "☕"

const val WAITING = "⏳"

const val CARD = "\uD83C\uDCCF"

const val COMMENT = "\uD83D\uDCAC"

const val TIMER = "⏱️"

private val idCounter = AtomicInteger()

class Session(
    val title: String,
    val chatId: Long,
    voters: Set<TgUser> = emptySet(),
    private val started: LocalDateTime = now()
) {
    private var votes: MutableMap<TgUser, Vote?> = mutableMapOf()
    private var avg = -1.0
    private var bestMatch = -1
    var closed = false
    var messageId = -1
    val id = idCounter.incrementAndGet()

    init {
        voters.forEach { votes[it] = null }
    }

    fun voters() = votes.keys

    fun resetVotes() {
        if (!closed) {
            votes = votes.mapValues { Vote(WAITING) }.toMutableMap()
        } else {
            throw IllegalStateException("Already closed")
        }
    }

    fun close() {
        if (!closed) {
            closed = true
            votes = votes.filter { it.value != null }.toMutableMap()
            avg = voteValueStream().average().orElse(.0)
            bestMatch =
                intVoteValues()
                    .map { v -> v to abs((v - avg)) }
                    .sortedBy { it.second }
                    .reversed()
                    .distinctBy { it.second }
                    .map { it.first }
                    .last()
        } else {
            throw IllegalStateException("Already closed")
        }
    }

    fun text(): String {
        return listOf(
                "\uD83D\uDCDD $title",
                votersString(),
                durationString(),
                voteResultString(),
                estimationResultString(),
            )
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    fun needDiscussion(): Boolean {
        val max = voteValueStream().max().orElse(0)
        val min = voteValueStream().min().orElse(0)
        return intVoteValues().count { it in min..max } > 3
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
            val template =
                if (voteValueStream().distinct().count() == 0L) {
                    ""
                } else if (voteValueStream().distinct().count() == 1L) {
                    "\uD83C\uDF89 Единогласно: %.1f"
                } else {
                    val bestMatchStr =
                        bestMatch
                            .takeIf { abs(avg - it.toDouble()) > 0.001 }
                            ?.let { "  ~  $it" }
                            .orEmpty()
                    "⚖ Итог: %.1f$bestMatchStr"
                }
            String.format(Locale.US, template, avg)
        } else {
            ""
        }

    private fun estimationResultString() =
        if (closed) {
            votes.values
                .filter { it?.estimation?.toIntOrNull() != null }
                .groupBy { it?.role }
                .mapValues { it.value.map { it?.estimation!! } }
                .entries
                .map {
                    val average = it.value.map { it.toInt() }.average()
                    val bestMatchStr =
                        average
                            .takeIf { abs(it.roundToInt() - it) > 0.001 }
                            ?.roundToInt()
                            ?.let { "  ~  $it" }
                            .orEmpty()
                    it.key.toString() + " %.1f".format(average) + bestMatchStr
                }
                .joinToString("\n")
        } else {
            ""
        }

    private fun voteValueStream() =
        votes.values
            .stream()
            .map { it?.value?.toIntOrNull() }
            .filter { it != null }
            .mapToInt { it!! }

    private fun votersString() =
        votes
            .toList()
            .sortedWith((compareBy(nullsLast()) { (_, value) -> value }))
            .toMap()
            .map { entry -> stringifyVoter(entry.key, entry.value) }
            .filter { it.isNotBlank() }
            .joinToString("\n")

    private fun stringifyVoter(user: TgUser, vote: Vote?): String {
        val voteStr =
            if (closed) {
                if (vote == null || vote.value == null && vote.estimation == null) {
                    return ""
                }
                val estimationSuffix =
                    vote.role?.let { "(" + it.emoji + vote.estimation + ")" }.orEmpty()
                val commentSuffix = needComment(vote).takeIf { it }?.let { " $COMMENT" }.orEmpty()
                ": " + vote.value.orEmpty() + estimationSuffix + commentSuffix
            } else {
                " " +
                    (vote
                        ?.let {
                            it.value?.let { CARD }.orEmpty() +
                                it.estimation?.let { TIMER }.orEmpty()
                        }
                        .orEmpty()
                        .ifEmpty { WAITING })
            }
        return user.usernameOrFullName() + voteStr
    }

    private fun needComment(vote: Vote?): Boolean {
        val voteInt = vote?.value?.toIntOrNull() ?: return false
        val min = voteInt.coerceAtMost(bestMatch)
        val max = voteInt.coerceAtLeast(bestMatch)
        return intVoteValues().count { it in min..max } > 2
    }

    fun vote(value: String, user: TgUser): Voted {
        if (value == TURN_OVER) {
            close()
            return Voted.CLOSED
        }
        val prev = votes[user]
        if (prev?.value == value) {
            prev.value = null
            return CLEARED
        } else {
            val voted = if (prev?.value == null) VOTED else CHANGED
            votes[user] = prev?.also { it.value = value } ?: Vote(value)
            return voted
        }
    }

    fun estimate(role: EstimationRole, estimation: String, user: TgUser): Voted {
        val prev = votes[user]?.let { Vote(it) }
        val vote = votes[user] ?: Vote(null, role = role, estimation = estimation)
        vote.role = role
        vote.estimation = estimation
        votes[user] = vote
        return if (prev?.estimation == null) {
            VOTED
        } else {
            if (prev.estimation == estimation && prev.role == role) {
                NONE
            } else {
                CHANGED
            }
        }
    }
}

data class Vote(
    var value: String?,
    val time: LocalDateTime = now(),
    var role: EstimationRole? = null,
    var estimation: String? = null
) : Comparable<Vote> {

    constructor(vote: Vote) : this(vote.value, now(), vote.role, vote.estimation)

    override fun compareTo(other: Vote) = this.time.compareTo(other.time)
}
