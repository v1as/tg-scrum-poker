package ru.v1as.tg.grooming.update.callback

import org.springframework.stereotype.Component
import ru.v1as.tg.grooming.model.ChatDataStorage
import ru.v1as.tg.grooming.update.answerCallback
import ru.v1as.tg.grooming.update.buildSessionMessage
import ru.v1as.tg.grooming.update.cleaningReplyMarkupSessionMessage
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.TgChat
import ru.v1as.tg.starter.model.TgUser
import ru.v1as.tg.starter.update.callback.AbstractSimpleCallbackHandler
import ru.v1as.tg.starter.update.callback.CallbackRequest

@Component
class RepeatCallback(val chatData: ChatDataStorage, val tgSender: TgSender) :
    AbstractSimpleCallbackHandler("repeat") {

    override fun handle(chat: TgChat, user: TgUser, callbackRequest: CallbackRequest) {
        val callbackMsg = callbackRequest.update.callbackQuery.message
        val prevSession = chatData.getSession(chat)
        if (prevSession == null ||
            !prevSession.closed ||
            prevSession.messageId != callbackMsg.messageId) {
            tgSender.execute(cleaningReplyMarkupSessionMessage(callbackMsg))
            tgSender.execute(answerCallback(callbackRequest, "Голосование не найдено."))
            return
        }
        tgSender.execute(cleaningReplyMarkupSessionMessage(callbackMsg))
        val session = chatData.newSession(prevSession.title, chat)
        val message = tgSender.execute(buildSessionMessage(callbackMsg, session))
        session.messageId = message.messageId
    }
}
