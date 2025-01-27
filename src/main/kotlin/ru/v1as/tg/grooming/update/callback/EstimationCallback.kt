package ru.v1as.tg.grooming.update.callback

import org.springframework.stereotype.Component
import ru.v1as.tg.grooming.model.ChatDataStorage
import ru.v1as.tg.grooming.update.chosenByCallbackEditMessageText
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.exceptions.TgMessageException
import ru.v1as.tg.starter.update.callback.CallbackHandler
import ru.v1as.tg.starter.update.callback.CallbackRequest
import ru.v1as.tg.starter.update.handle.Handled
import ru.v1as.tg.starter.update.handle.unmatched
import ru.v1as.tg.starter.update.request.RequestUpdateHandler

const val ESTIMATE_CALLBACK = "e_"

@Component
class EstimationCallback(
    val tgSender: TgSender,
    val chatData: ChatDataStorage,
    val requestUpdateHandler: RequestUpdateHandler
) : CallbackHandler {
    override fun handle(input: CallbackRequest): Handled {
        if (!input.data.startsWith(ESTIMATE_CALLBACK)) {
            return unmatched()
        }
        val data = input.data.split("_")
        if (data.size != 5) {
            return unmatched()
        }
        val chatId = data[1]
        val messageId = data[2]
        val session =
            chatData.getSession(chatId, messageId) ?: throw throw TgMessageException("Задача для оценки не найдена.")
        val role = data[3]
        val estimation = data[4]
        session.estimate(role, estimation, input.from)
        tgSender.execute(chosenByCallbackEditMessageText(input.update, estimation))
        return unmatched()
    }
}