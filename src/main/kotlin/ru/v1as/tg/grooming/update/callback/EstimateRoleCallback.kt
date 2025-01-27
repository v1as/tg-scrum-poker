package ru.v1as.tg.grooming.update.callback

import java.time.Duration
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import ru.v1as.tg.grooming.model.ChatDataStorage
import ru.v1as.tg.grooming.model.EstimationRole
import ru.v1as.tg.grooming.model.Session
import ru.v1as.tg.grooming.model.Voted.CLOSED
import ru.v1as.tg.grooming.model.Voted.NONE
import ru.v1as.tg.grooming.update.ROLES
import ru.v1as.tg.grooming.update.chosenByCallbackEditMessageText
import ru.v1as.tg.grooming.update.cleaningReplyMarkupMessage
import ru.v1as.tg.grooming.update.estimationDaysKeyboard
import ru.v1as.tg.grooming.update.updateMessage
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.exceptions.TgMessageException
import ru.v1as.tg.starter.model.base.TgUserWrapper
import ru.v1as.tg.starter.update.callback.CallbackHandler
import ru.v1as.tg.starter.update.callback.CallbackRequest
import ru.v1as.tg.starter.update.handle.Handled
import ru.v1as.tg.starter.update.handle.handled
import ru.v1as.tg.starter.update.handle.unmatched
import ru.v1as.tg.starter.update.request.RequestUpdateHandler
import ru.v1as.tg.starter.update.request.UpdateRequest

const val ESTIMATE_ROLE_CALLBACK = "er_"

@Component
class EstimateRoleCallback(
    val tgSender: TgSender,
    val chatData: ChatDataStorage,
    val requestUpdateHandler: RequestUpdateHandler,
    val estimationDaysCallback: EstimationDaysCallback
) : CallbackHandler {
    override fun handle(input: CallbackRequest): Handled {
        if (!input.data.startsWith(ESTIMATE_ROLE_CALLBACK)) {
            return unmatched()
        }
        val data = input.data.split("_")
        val chatId = data[1]
        val sessionId = data[2]
        val session = chatData.getSession(chatId, sessionId) ?: return unmatched()
        val roleStr = data[3]
        val role =
            ROLES.find { it.name == data[3] }
                ?: throw throw TgMessageException("Незнакомая роль ${data[3]}")
        tgSender.execute(chosenByCallbackEditMessageText(input.update, roleStr))
        val message =
            tgSender.execute(
                SendMessage(
                        input.chat.idStr(),
                        "Выберите или пришлите количество дней для роли '$roleStr' по задаче '${session.title}'")
                    .also { it.replyMarkup = estimationDaysKeyboard(chatId, sessionId, roleStr) })

        val manualEstimationUpdateRequest =
            UpdateRequest(
                { this.estimate(it, session, role, message) },
                Duration.ofMinutes(3),
                input.from.id(),
                input.from.id(),
                filter = { this.isIntForSession(it, session) },
                onTimeout = {
                    estimationDaysCallback.dropManualEstimationRequest(input.from, session)
                })
        requestUpdateHandler.register(manualEstimationUpdateRequest)
        estimationDaysCallback.registerManualEstimationRequest(
            input.from, session, manualEstimationUpdateRequest)
        return handled()
    }

    private fun estimate(value: Update, session: Session, role: EstimationRole, message: Message) {
        if (session.closed) {
            return
        }
        val estimation = value.message.text
        tgSender.execute(cleaningReplyMarkupMessage(message))
        val voted = session.estimate(role, estimation, TgUserWrapper(value.message.from))
        when (voted) {
            CLOSED -> throw throw TgMessageException("Задача уже закрыта.")
            NONE -> throw throw TgMessageException("Вы уже внесли такую оценку.")
            else -> listOf(updateMessage(session))
        }.forEach { tgSender.executeAsync(it) }
        tgSender.execute(
            SendMessage(
                message.chat.id.toString(),
                "Ваша оценка $estimation для роли '$role' по задаче '${session.title}' принята."))
    }

    private fun isIntForSession(value: Update, session: Session) =
        !session.closed && value.message?.text?.toIntOrNull()?.let { true } ?: false
}
