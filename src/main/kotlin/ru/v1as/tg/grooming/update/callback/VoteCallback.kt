package ru.v1as.tg.grooming.update.callback

import mu.KLogging
import org.springframework.stereotype.Component
import ru.v1as.tg.grooming.model.ChatDataStorage
import ru.v1as.tg.grooming.model.TIMER
import ru.v1as.tg.grooming.model.Voted.*
import ru.v1as.tg.grooming.update.answerCallback
import ru.v1as.tg.grooming.update.cleaningReplyMarkupMessage
import ru.v1as.tg.grooming.update.columnInlineKeyboardMarkup
import ru.v1as.tg.grooming.update.updateMessage
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.TgChat
import ru.v1as.tg.starter.model.TgUser
import ru.v1as.tg.starter.update.callback.AbstractCallbackWithPrefixHandler
import ru.v1as.tg.starter.update.callback.CallbackRequest

@Component
class VoteCallback(val chatDataStorage: ChatDataStorage, val tgSender: TgSender) :
    AbstractCallbackWithPrefixHandler("vote_") {

    companion object : KLogging()

    override fun handle(
        input: String,
        chat: TgChat,
        user: TgUser,
        callbackRequest: CallbackRequest
    ) {
        val session = chatDataStorage.getSession(chat)
        val callbackMsg = callbackRequest.update.callbackQuery.message
        if (session == null || session.closed || session.messageId != callbackMsg.messageId) {
            tgSender.execute(cleaningReplyMarkupMessage(callbackMsg))
            tgSender.execute(answerCallback(callbackRequest, "Это голосование уже закрыто."))
            return
        }
        if (input == TIMER) {

            return
        }

        val voted = session.vote(input, user)
        logger.debug { "Voted: $voted" }
        when (voted) {
            CLOSED -> {
                val message = cleaningReplyMarkupMessage(chat, session)
                if (session.needDiscussion()) {
                    message.replyMarkup = columnInlineKeyboardMarkup("Повторить" to "repeat")
                }
                listOf(message)
            }
            VOTED -> listOf(updateMessage(session))
            CLEARED ->
                listOf(updateMessage(session), answerCallback(callbackRequest, "Вы отозвали голос"))
            CHANGED -> listOf(answerCallback(callbackRequest, "Вы изменили голос: $input"))
            else -> listOf()
        }.forEach { tgSender.executeAsync(it) }
    }
}
