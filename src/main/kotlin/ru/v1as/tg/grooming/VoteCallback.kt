package ru.v1as.tg.grooming

import org.springframework.stereotype.Component
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.TgChat
import ru.v1as.tg.starter.model.TgUser
import ru.v1as.tg.starter.update.callback.AbstractCallbackWithPrefixHandler

@Component
class VoteCallback(val chatDataStorage: ChatDataStorage, val tgSender: TgSender) :
    AbstractCallbackWithPrefixHandler("vote_") {
    override fun handle(value: String, chat: TgChat, user: TgUser) {
        chatDataStorage.vote(value, chat, user)?.let { tgSender.execute(it) }
    }
}