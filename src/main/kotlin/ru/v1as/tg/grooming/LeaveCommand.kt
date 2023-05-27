package ru.v1as.tg.grooming

import org.springframework.stereotype.Component
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.exceptions.TgMessageException
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest

@Component
class LeaveCommand(
    val chatDataStorage: ChatDataStorage,
    val tgSender: TgSender,
) : AbstractCommandHandler("leave") {
    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        if (!chat.isUserChat()) {
            throw TgMessageException("Command allowed only in private chat.")
        }
        val editMessageText = chatDataStorage.leave(user)
        tgSender.execute(editMessageText)
    }
}
