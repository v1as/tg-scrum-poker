package ru.v1as.tg.grooming.update.command

import org.springframework.stereotype.Component
import ru.v1as.tg.grooming.model.ChatDataStorage
import ru.v1as.tg.grooming.update.buildMessage
import ru.v1as.tg.grooming.update.cleaningReplyMarkupMessage
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.exceptions.TgMessageException
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest

@Component
class StartCommand(val tgSender: TgSender, val chatData: ChatDataStorage) :
    AbstractCommandHandler("start") {
    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        if (command.arguments.isNotEmpty()) {
            if (chat.isUserChat()) {
                throw TgMessageException("Command allowed only in group chat.")
            } else {
                chatData
                    .getSession(chat)
                    ?.takeIf { !it.closed }
                    ?.let {
                        it.close()
                        tgSender.execute(cleaningReplyMarkupMessage(chat, it))
                    }
                val session = chatData.newSession(command.argumentsString, chat)
                val message = tgSender.execute(buildMessage(command.update.message, session))
                session.messageId = message.messageId
            }
        }
    }
}
