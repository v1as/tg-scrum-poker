package ru.v1as.tg.grooming.update

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.v1as.tg.grooming.model.COFFEE
import ru.v1as.tg.grooming.model.Session
import ru.v1as.tg.grooming.model.TURN_OVER
import ru.v1as.tg.starter.model.TgChat
import ru.v1as.tg.starter.update.answerCallbackQuery
import ru.v1as.tg.starter.update.callback.CallbackRequest
import ru.v1as.tg.starter.update.editMessageText
import ru.v1as.tg.starter.update.inlineKeyboardButton
import ru.v1as.tg.starter.update.sendMessage

var VALUES = listOf("1", "2", "3", "5", "8", "13", "21", COFFEE, TURN_OVER)

fun intVoteValues(): List<Int> =
    VALUES.stream().map { it.toIntOrNull() }.filter { it != null }.map { it!! }.toList()

fun buildMessage(message: Message?, session: Session) = sendMessage {
    chatId = message?.chatId.toString()
    text = session.text()
    replyMarkup = votingKeyboard()
    messageThreadId = message?.messageThreadId
}

fun updateMessage(chat: TgChat, session: Session) = editMessageText {
    chatId = chat.getId().toString()
    messageId = session.messageId
    text = session.text()
    replyMarkup = votingKeyboard()
}

fun cleaningReplyMarkupMessage(message: Message) = editMessageText {
    chatId = message.chatId.toString()
    messageId = message.messageId
    text = message.text
    replyMarkup = columnInlineKeyboardMarkup()
}

fun cleaningReplyMarkupMessage(chat: TgChat, session: Session) = editMessageText {
    chatId = chat.getId().toString()
    messageId = session.messageId
    text = session.text()
    replyMarkup = columnInlineKeyboardMarkup()
}

fun columnInlineKeyboardMarkup(vararg buttons: Pair<String, String>): InlineKeyboardMarkup {
    return InlineKeyboardMarkup(
        listOf(
            buttons.map {
                inlineKeyboardButton {
                    text = it.first
                    callbackData = it.second
                }
            }))
}

fun answerCallback(callbackRequest: CallbackRequest, text: String) = answerCallbackQuery {
    callbackQueryId = callbackRequest.callbackQueryId()
    this.text = text
}

fun votingKeyboard(): InlineKeyboardMarkup {
    val rows = mutableListOf<List<InlineKeyboardButton>>()
    var tempRow = mutableListOf<InlineKeyboardButton>()
    for (value in VALUES) {
        if (tempRow.size == 2) {
            rows += tempRow
            tempRow = mutableListOf()
        }
        tempRow += inlineKeyboardButton {
            text = value
            callbackData = "vote_$value"
        }
    }
    if (tempRow.isNotEmpty()) {
        rows += tempRow
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
