package ru.v1as.tg.grooming.update.command

import org.springframework.stereotype.Component
import ru.v1as.tg.grooming.model.ChatDataStorage
import ru.v1as.tg.grooming.update.cleaningReplyMarkupSessionMessage
import ru.v1as.tg.grooming.update.replySendMessage
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest

@Component
class ClearCommand(val tgSender: TgSender, val chatData: ChatDataStorage) :
    AbstractCommandHandler("clear") {
    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        val tasks = chatData.getTasks(chat)
        val session = chatData.getSession(chat)
        if (session?.closed == false) {
            session.resetVotes()
            session.close()
            tgSender.execute(cleaningReplyMarkupSessionMessage(chat, session))
        }
        val reply =
            if (tasks.isEmpty()) {
                "Очередь задач пуста."
            } else {
                val cleaned = tasks.size
                tasks.clear()
                "Из очереди удалено $cleaned задач"
            }
        tgSender.execute(command.message.replySendMessage { text = reply })
    }
}
