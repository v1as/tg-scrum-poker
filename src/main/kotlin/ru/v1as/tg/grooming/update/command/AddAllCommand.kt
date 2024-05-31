package ru.v1as.tg.grooming.update.command

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Message
import ru.v1as.tg.grooming.model.ChatDataStorage
import ru.v1as.tg.grooming.update.replySendMessage
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest
import ru.v1as.tg.starter.update.request.RequestUpdateHandler
import ru.v1as.tg.starter.update.request.replyOnMessageRequest

@Component
class AddAllCommand(
    val tgSender: TgSender,
    val chatData: ChatDataStorage,
    val requestUpdateHandler: RequestUpdateHandler
) : AbstractCommandHandler("add_all") {
    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        val msgText = command.argumentsString
        val msg = command.message
        handle(msgText, msg, user, chat)
    }

    private fun handle(msgText: String, msg: Message, user: TgUserWrapper, chat: TgChatWrapper) {
        val newTasks = msgText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        if (newTasks.isEmpty()) {
            val reqMsg =
                tgSender.execute(
                    msg.replySendMessage {
                        text =
                            "Ответьте на это сообщение списком задач.\n" +
                                "(Бот имеет доступ только ответам на свои сообщения)"
                    })
            requestUpdateHandler.register(
                replyOnMessageRequest(
                    reqMsg, { handle(it.message.text, it.message, user, chat) }, user))
            return
        }
        if (newTasks.sumOf { it.length } > 10_000) {
            tgSender.execute(msg.replySendMessage { text = "Слишком длинная команда" })
            return
        }
        val tasks = chatData.getTasks(chat)
        if (tasks.size + newTasks.size > 50) {
            val replyMessage =
                msg.replySendMessage { text = "Слишком много задач. Очистите очередь /clean" }
            tgSender.execute(replyMessage)
            return
        }
        tasks += newTasks
        tgSender.execute(
            msg.replySendMessage {
                text =
                    "Добавлено ${newTasks.size} задач в очередь." +
                        " Всего в очереди задач ${tasks.size}."
            })
    }
}
