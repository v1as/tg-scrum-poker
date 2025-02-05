package ru.v1as.tg.grooming.update.command

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import ru.v1as.tg.grooming.model.ChatDataStorage
import ru.v1as.tg.grooming.update.buildSessionMessage
import ru.v1as.tg.grooming.update.cleaningReplyMarkupSessionMessage
import ru.v1as.tg.grooming.update.replySendMessage
import ru.v1as.tg.grooming.update.rolesKeyboard
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.exceptions.TgMessageException
import ru.v1as.tg.starter.model.TgChat
import ru.v1as.tg.starter.model.base.TgChatWrapper
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.command.AbstractCommandHandler
import ru.v1as.tg.starter.update.command.CommandRequest
import ru.v1as.tg.starter.update.request.RequestUpdateHandler
import ru.v1as.tg.starter.update.request.replyOnMessageRequest

val TIME_ESTIMATION_ARGUMENT = "TIMEESTIMATION"

@Component
class StartCommand(
    val tgSender: TgSender,
    val chatData: ChatDataStorage,
    val requestUpdateHandler: RequestUpdateHandler
) : AbstractCommandHandler("start") {

    override fun handle(command: CommandRequest, user: TgUserWrapper, chat: TgChatWrapper) {
        val msgSrc = command.message
        if (chat.isUserChat() &&
            command.arguments.size == 3 &&
            command.arguments.first().startsWith(TIME_ESTIMATION_ARGUMENT)) {
            val chatId = command.arguments[1]
            val sessionId = command.arguments[2]
            val session =
                chatData.getSession(chatId, sessionId)
                    ?: throw TgMessageException("Задача для оценки не найдена.")
            tgSender.execute(
                SendMessage(
                        chat.getId().toString(),
                        "Выберите роль оценки для задачи: ${session.title}")
                    .also { it.replyMarkup = rolesKeyboard(chatId, sessionId) })
        } else if (!chat.isUserChat() && command.arguments.isNotEmpty()) {
            chatData
                .getSession(chat)
                ?.takeIf { !it.closed }
                ?.let {
                    it.close()
                    tgSender.execute(cleaningReplyMarkupSessionMessage(chat, it))
                }
            openSession(chat, command.argumentsString, msgSrc)
        } else if (!chat.isUserChat()) {
            val tasks = chatData.getTasks(command.chat)
            if (tasks.isNotEmpty()) {
                val task = tasks.removeFirst()
                openSession(chat, task, msgSrc)
            } else {
                val msg =
                    tgSender.execute(
                        msgSrc.replySendMessage {
                            text =
                                "Отправьте, описание задачи ответом на это сообщение.\n" +
                                    "(Бот имеет доступ только ответам на свои сообщения)"
                        })
                requestUpdateHandler.register(
                    replyOnMessageRequest(
                        msg, { openSession(chat, it.message.text, it.message) }, user))
            }
        } else {
            tgSender.message(chat, "Эта команда работает только в групповом чате.")
        }
    }

    private fun openSession(chat: TgChat, title: String, msgSrc: Message) {
        val session = chatData.newSession(title, chat)
        val message = tgSender.execute(buildSessionMessage(msgSrc, session))
        session.messageId = message.messageId
    }
}
