package ru.v1as.tg.grooming

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.keyboard.StartCommandLinkFactory
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest

@Component
class StartCommand(
    val tgSender: TgSender,
    val startLink: StartCommandLinkFactory,
    val chatDataStorage: ChatDataStorage
) :
    AbstractCommandHandler("start") {
    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        if (command.arguments.isEmpty()) {
            if (chat.isUserChat()) {
                tgSender.execute(SendMessage(chat.idStr(), "Command allowed only in group chat."))
            } else {
                val sendMessage = SendMessage(chat.idStr(), "Grooming session is starting...")
                sendMessage.replyMarkup =
                    startLink.inlineKeyboardMarkupStartLink("Join!", "join", chat.getId())
                tgSender.execute(sendMessage)
            }
        } else if (command.arguments.contains("join") && chat.isUserChat()) {
            tgSender.execute(SendMessage(chat.idStr(), "You have joined!!"))
        }
    }
}