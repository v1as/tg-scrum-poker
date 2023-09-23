package ru.v1as.tg.grooming

import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.v1as.tg.starter.model.TgUser
import java.time.LocalDateTime

const val OPEN = "Открыть"

const val COFFEE = "☕"

val VALUES = listOf("1", "2", "3", "5", "8", "13", "21", COFFEE, OPEN)

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
            votes = mutableMapOf()
            votes = votes.filter { it.value != null }.toMutableMap()
            val text = buildText()
            return EditMessageText.builder().chatId(msg.chatId).messageId(msg.messageId).text(text)
                .replyMarkup(
                    InlineKeyboardMarkup(emptyList())
                ).build()
        }
        throw IllegalStateException("Already closed")
    }

    private fun buildText(): String {
        val votes = votes.map {
            val vote = if (closed) {
                ": " + it.value?.value.orEmpty()
            } else {
                " " + (it.value?.let { "\uD83D\uDFE2" } ?: "⏳")
            }
            return it.key.usernameOrFullName() + vote
        }.joinToString("\n")

        return title + "\n\n" + votes
    }

    fun editMessage(): EditMessageText {
        return EditMessageText.builder().text(buildText()).messageId(msg.messageId)
            .chatId(msg.chatId).replyMarkup(keyboard()).build()
    }

    fun vote(value: String, user: TgUser): EditMessageText? {
        val oldText = buildText()
        if (value == OPEN) {
            return close()
        }
        val prev = votes[user]
        votes[user] = if (prev?.value == value) {
            null
        } else {
            Vote(value)
        }
        return if (buildText() == oldText) null else editMessage()
    }

}

data class Vote(
    val value: String, val time: LocalDateTime = LocalDateTime.now()
)

