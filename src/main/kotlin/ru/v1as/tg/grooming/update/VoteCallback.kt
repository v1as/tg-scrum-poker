package ru.v1as.tg.grooming.update

import org.springframework.stereotype.Component
import ru.v1as.tg.grooming.model.ChatDataStorage
import ru.v1as.tg.grooming.model.Voted.*
import ru.v1as.tg.grooming.answerCallback
import ru.v1as.tg.grooming.cleaningMessage
import ru.v1as.tg.grooming.updateMessage
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.TgChat
import ru.v1as.tg.starter.model.TgUser
import ru.v1as.tg.starter.update.callback.AbstractCallbackWithPrefixHandler
import ru.v1as.tg.starter.update.callback.CallbackRequest

@Component
class VoteCallback(val chatDataStorage: ChatDataStorage, val tgSender: TgSender) :
    AbstractCallbackWithPrefixHandler("vote_") {
    override fun handle(
        input: String,
        chat: TgChat,
        user: TgUser,
        callbackRequest: CallbackRequest
    ) {
        val session = chatDataStorage.getSession(chat)
        if (session == null || session.closed) {
            tgSender.execute(cleaningMessage(callbackRequest.update.callbackQuery.message))
            tgSender.execute(answerCallback(callbackRequest, "Это голосование уже закрыто."))
            return
        }

        val voted = session.vote(input, user)
        when (voted) {
            CLOSED -> listOf(cleaningMessage(chat, session))
            VOTED -> listOf(updateMessage(chat, session))
            CLEARED ->
                listOf(
                    updateMessage(chat, session),
                    answerCallback(callbackRequest, "Вы отозвали голос")
                )
            CHANGED ->
                listOf(
                    updateMessage(chat, session),
                    answerCallback(callbackRequest, "Вы изменили голос: $input")
                )
        }.forEach { tgSender.execute(it) }
    }
}
