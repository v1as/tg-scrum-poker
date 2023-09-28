package ru.v1as.tg.grooming.update

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.v1as.tg.grooming.model.COFFEE
import ru.v1as.tg.grooming.model.Session
import ru.v1as.tg.grooming.model.TURN_OVER
import ru.v1as.tg.starter.model.TgChat
import ru.v1as.tg.starter.update.answerCallbackQuery
import ru.v1as.tg.starter.update.callback.CallbackRequest
import ru.v1as.tg.starter.update.editMessageText
import ru.v1as.tg.starter.update.sendMessage

var VALUES = listOf("1", "2", "3", "5", "8", "13", "21", COFFEE, TURN_OVER)

fun buildMessage(update: Update, session: Session) = sendMessage {
    chatId = update.message.chatId.toString()
    text = session.text()
    replyMarkup = votingKeyboard()
    messageThreadId = update.message.messageThreadId
}

fun updateMessage(chat: TgChat, session: Session) = editMessageText {
    chatId = chat.getId().toString()
    messageId = session.messageId
    text = session.text()
    replyMarkup = votingKeyboard()
}

fun cleaningMessage(message: Message) = editMessageText {
    chatId = message.chatId.toString()
    messageId = message.messageId
    text = message.text
    replyMarkup = emptyInlineKeyboardMarkup()
}

fun cleaningMessage(chat: TgChat, session: Session) = editMessageText {
    chatId = chat.getId().toString()
    messageId = session.messageId
    text = session.text()
    replyMarkup = emptyInlineKeyboardMarkup()
}

fun emptyInlineKeyboardMarkup() = InlineKeyboardMarkup(emptyList())

fun answerCallback(callbackRequest: CallbackRequest, text: String) = answerCallbackQuery {
    callbackQueryId = callbackRequest.callbackQueryId()
    this.text = text
}

fun votingKeyboard(): InlineKeyboardMarkup {
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

@Component
class TgMethods(@Value("\${scrum.values}") val valuesStr: String) {
    companion object : KLogging()
    init {
        VALUES =
            valuesStr
                .split(",")
                .toMutableList()
                .also {
                    it.add(COFFEE)
                    it.add(TURN_OVER)
                }
                .toList()
        logger.info { "Values: $VALUES" }
    }
}
