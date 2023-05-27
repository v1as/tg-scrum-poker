package ru.v1as.tg.grooming

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.exceptions.TgMessageException
import ru.v1as.tg.starter.keyboard.StartCommandLinkFactory
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest

@Component
class StartCommand(
    val tgSender: TgSender,
    val startLink: StartCommandLinkFactory,
    val chatData: ChatDataStorage
) : AbstractCommandHandler("start") {
    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        if (command.arguments.isEmpty()) {
            if (chat.isUserChat()) {
                throw TgMessageException("Command allowed only in group chat.")
            } else {
                val sendMessage = SendMessage(chat.idStr(), "Grooming session is starting...")
                sendMessage.replyMarkup = startLink.buildKeyboard("Join!", "join", chat.getId())
                val message = tgSender.execute(sendMessage)
                chatData.setData(chat, GroomingSession(message))?.also {
                    tgSender.execute(it.finish())
                }
            }
        } else if (command.arguments.contains("join") && chat.isUserChat()) {
            val chatId = command.argumentAfter("join").toLong()
            val data = chatData.getData(chatId)
            data?.join(user)?.also {
                chatData.bind(user, chatId)
                tgSender.execute(it)
                tgSender.message(chat, "You have joined!!")
            }
        }
    }
}
