package ru.v1as.tg.grooming.update.command

import org.springframework.stereotype.Component
import ru.v1as.tg.grooming.model.ChatDataStorage
import ru.v1as.tg.grooming.update.buildMessage
import ru.v1as.tg.grooming.update.replySendMessage
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest

@Component
class RepeatCommand(val tgSender: TgSender, val chatData: ChatDataStorage) :
    AbstractCommandHandler("repeat") {
    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        val msg = command.update.message
        val prevSession = chatData.getSession(chat)
        if (prevSession == null || !prevSession.closed) {
            tgSender.execute(msg.replySendMessage { text = "Не нашёл голосование для повторения." })
            return
        }
        val session = chatData.newSession(prevSession.title, chat)
        val sessionMsg = tgSender.execute(buildMessage(msg, session))
        session.messageId = sessionMsg.messageId
    }
}
