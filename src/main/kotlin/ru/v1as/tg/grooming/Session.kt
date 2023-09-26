package ru.v1as.tg.grooming

import java.time.LocalDateTime
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.v1as.tg.starter.model.TgUser

const val TURN_OVER = "\uD83C\uDCCF\uD83D\uDD04"

const val COFFEE = "☕"

const val WAITING = "⏳"

val VALUES = listOf("1", "2", "3", "5", "8", "13", "21", COFFEE, TURN_OVER)

fun keyboard(): InlineKeyboardMarkup {
    val rows = mutableListOf<List<InlineKeyboardButton>>()
    var row = mutableListOf<InlineKeyboardButton>()
    for (value in VALUES) {
        if (row.size == 2) {
            rows.add(row)
            row = mutableListOf()
        }
        row.add(InlineKeyboardButton.builder().text(value).callbackData("vote_$value").build())
    }
    if (row.isNotEmpty()) {
        rows.add(row)
    }
    return InlineKeyboardMarkup(rows)
}

class Session(val msg: Message, voters: Set<TgUser> = emptySet()) {
    private var votes: MutableMap<TgUser, Vote?> = mutableMapOf()
    private val title: String
    var closed = false

    init {
        voters.forEach { votes[it] = null }
        title = msg.text
    }

    fun voters() = votes.keys

    fun close(): EditMessageText {
        if (!closed) {
            closed = true
            votes = votes.filter { it.value != null }.toMutableMap()
            val text = buildText()
            return EditMessageText.builder()
                .chatId(msg.chatId)
                .messageId(msg.messageId)
                .text(text)
                .replyMarkup(InlineKeyboardMarkup(emptyList()))
                .build()
        }
        throw IllegalStateException("Already closed")
    }

    private fun buildText(): String {
        val votesStr =
            votes
                .map {
                    val vote =
                        if (closed) {
                            ": " + it.value?.value.orEmpty()
                        } else {
                            " " + (it.value?.let { "\uD83C\uDCCF" } ?: WAITING)
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
                String.format("%.2f", avgVote)
            } else {
                ""
            }

        return title + "\n\n" + votesStr + "\n\n" + voteResult
    }

    fun editMessage(): EditMessageText {
        return EditMessageText.builder()
            .text(buildText())
            .messageId(msg.messageId)
            .chatId(msg.chatId)
            .replyMarkup(keyboard())
            .build()
    }

    fun vote(value: String, user: TgUser): EditMessageText? {
        val oldText = buildText()
        if (value == TURN_OVER) {
            return close()
        }
        val prev = votes[user]
        votes[user] =
            if (prev?.value == value) {
                null
            } else {
                Vote(value)
            }
        return if (buildText() == oldText) null else editMessage()
    }
}

data class Vote(val value: String, val time: LocalDateTime = LocalDateTime.now())
