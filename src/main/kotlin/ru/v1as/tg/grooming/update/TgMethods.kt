package ru.v1as.tg.grooming

import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.v1as.tg.grooming.model.Session
import ru.v1as.tg.grooming.model.VALUES
import ru.v1as.tg.starter.model.TgChat
import ru.v1as.tg.starter.update.callback.CallbackRequest

fun buildMessage(update: Update, session: Session): SendMessage =
    SendMessage.builder()
        .chatId(update.message.chatId)
        .text(session.text())
        .replyMarkup(votingKeyboard())
        .messageThreadId(update.message.messageThreadId)
        .build()

fun updateMessage(chat: TgChat, session: Session): EditMessageText =
    EditMessageText.builder()
        .chatId(chat.getId())
        .messageId(session.messageId)
        .text(session.text())
        .replyMarkup(votingKeyboard())
        .build()

fun cleaningMessage(message: Message): EditMessageText =
    EditMessageText.builder()
        .chatId(message.chatId)
        .messageId(message.messageId)
        .text(message.text)
        .replyMarkup(InlineKeyboardMarkup(emptyList()))
        .build()

fun cleaningMessage(chat: TgChat, session: Session): EditMessageText =
    EditMessageText.builder()
        .chatId(chat.getId())
        .messageId(session.messageId)
        .text(session.text())
        .replyMarkup(InlineKeyboardMarkup(emptyList()))
        .build()

fun answerCallback(callbackRequest: CallbackRequest, text: String): AnswerCallbackQuery =
    AnswerCallbackQuery.builder()
        .callbackQueryId(callbackRequest.callbackQueryId())
        .text(text)
        .build()

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
