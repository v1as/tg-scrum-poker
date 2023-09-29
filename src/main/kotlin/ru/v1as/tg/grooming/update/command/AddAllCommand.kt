package ru.v1as.tg.grooming.update.command

import org.springframework.stereotype.Component
import ru.v1as.tg.grooming.model.ChatDataStorage
import ru.v1as.tg.grooming.update.replySendMessage
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest

@Component
class AddAllCommand(val tgSender: TgSender, val chatData: ChatDataStorage) :
    AbstractCommandHandler("add_all") {
    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        val newTasks =
            command.argumentsString.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val msg = command.message
        if (newTasks.isEmpty()) {
            tgSender.execute(msg.replySendMessage { text = "Команда пуста" })
            return
        }
        if (newTasks.sumOf { it.length } > 10_000) {
            tgSender.execute(msg.replySendMessage { text = "Слишком длинная команда" })
            return
        }
        val tasks = chatData.getTasks(command.chat)
        if (tasks.size + newTasks.size > 50) {
            tgSender.execute(
                msg.replySendMessage { text = "Слишком много задач. Очистите очередь /clean" })
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
