package ru.v1as.tg.grooming.update.command

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Message
import ru.v1as.tg.grooming.model.ChatDataStorage
import ru.v1as.tg.grooming.update.buildMessage
import ru.v1as.tg.grooming.update.cleaningReplyMarkupMessage
import ru.v1as.tg.grooming.update.replySendMessage
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.exceptions.TgMessageException
import ru.v1as.tg.starter.model.TgChat
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest

@Component
class StartCommand(val tgSender: TgSender, val chatData: ChatDataStorage) :
    AbstractCommandHandler("start") {
    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        val msgSrc = command.message
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
                openSession(chat, command.argumentsString, msgSrc)
            }
        } else {
            val tasks = chatData.getTasks(command.chat)
            if (tasks.isNotEmpty()) {
                val task = tasks.removeFirst()
                openSession(chat, task, msgSrc)
            } else {
                tgSender.execute(
                    msgSrc.replySendMessage {
                        text =
                            "Тело команды пусто.\n" +
                                "Отправьте, например, '/start TGSM-123 Описание задачи'" +
                                "или воспользуйтесь командой /add_all."
                    })
            }
        }
    }

    private fun openSession(chat: TgChat, title: String, msgSrc: Message) {
        val session = chatData.newSession(title, chat)
        val message = tgSender.execute(buildMessage(msgSrc, session))
        session.messageId = message.messageId
    }
}
