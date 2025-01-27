package ru.v1as.tg.grooming.update.callback

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.v1as.tg.grooming.model.ChatDataStorage
import ru.v1as.tg.grooming.update.chosenByCallbackEditMessageText
import ru.v1as.tg.grooming.update.estimationDaysKeyboard
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.update.callback.CallbackHandler
import ru.v1as.tg.starter.update.callback.CallbackRequest
import ru.v1as.tg.starter.update.handle.Handled
import ru.v1as.tg.starter.update.handle.handled
import ru.v1as.tg.starter.update.handle.unmatched
import ru.v1as.tg.starter.update.request.RequestUpdateHandler

const val ESTIMATE_ROLE_CALLBACK = "er_"

@Component
class EstimateRoleCallback(
    val tgSender: TgSender,
    val chatData: ChatDataStorage,
    val requestUpdateHandler: RequestUpdateHandler
) : CallbackHandler {
    override fun handle(input: CallbackRequest): Handled {
        if (!input.data.startsWith(ESTIMATE_ROLE_CALLBACK)) {
            return unmatched()
        }
        val data = input.data.split("_")
        val chatId = data[1]
        val messageId = data[2]
        val session = chatData.getSession(chatId, messageId) ?: return unmatched()
        val role = data[3]
        tgSender.execute(chosenByCallbackEditMessageText(input.update, role))
        tgSender.execute(
            SendMessage(
                input.chat.idStr(),
                "Выберите или пришлите количество дней для роли '$role' по задаче '${session.title}'"
            ).also {
                it.replyMarkup =
                    estimationDaysKeyboard(chatId, messageId, role)
            }
        )
        return handled()
    }
}