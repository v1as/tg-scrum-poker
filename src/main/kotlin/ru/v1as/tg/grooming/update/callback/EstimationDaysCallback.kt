package ru.v1as.tg.grooming.update.callback

import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Component
import ru.v1as.tg.grooming.model.ChatDataStorage
import ru.v1as.tg.grooming.model.Session
import ru.v1as.tg.grooming.model.Voted.CLOSED
import ru.v1as.tg.grooming.model.Voted.NONE
import ru.v1as.tg.grooming.update.ROLES
import ru.v1as.tg.grooming.update.chosenByCallbackEditMessageText
import ru.v1as.tg.grooming.update.updateSessionMessage
import ru.v1as.tg.starter.TgSender
import ru.v1as.tg.starter.exceptions.TgMessageException
import ru.v1as.tg.starter.model.TgUser
import ru.v1as.tg.starter.update.callback.CallbackHandler
import ru.v1as.tg.starter.update.callback.CallbackRequest
import ru.v1as.tg.starter.update.handle.Handled
import ru.v1as.tg.starter.update.handle.handled
import ru.v1as.tg.starter.update.handle.unmatched
import ru.v1as.tg.starter.update.request.UpdateRequest

const val ESTIMATE_DAYS_CALLBACK = "e_"

@Component
class EstimationDaysCallback(val tgSender: TgSender, val chatData: ChatDataStorage) :
    CallbackHandler {

    var manualEstimationRequests = ConcurrentHashMap<String, UpdateRequest>()

    override fun handle(input: CallbackRequest): Handled {
        if (!input.data.startsWith(ESTIMATE_DAYS_CALLBACK)) {
            return unmatched()
        }
        val data = input.data.split("_")
        if (data.size != 5) {
            return unmatched()
        }
        val chatId = data[1]
        val sessionId = data[2]
        val session =
            chatData.getSession(chatId, sessionId)
                ?: throw throw TgMessageException("Задача для оценки не найдена.")
        val role =
            ROLES.find { it.name == data[3] }
                ?: throw throw TgMessageException("Незнакомая роль ${data[3]}")
        val estimation = data[4]
        tgSender.execute(chosenByCallbackEditMessageText(input.update, estimation))
        val user = input.from
        val voted = session.estimate(role, estimation, user)
        dropManualEstimationRequest(user, session)
        when (voted) {
            CLOSED -> throw throw TgMessageException("Задача уже закрыта.")
            NONE -> throw throw TgMessageException("Вы уже внесли такую оценку.")
            else -> listOf(updateSessionMessage(session))
        }.forEach { tgSender.executeAsync(it) }
        return handled()
    }

    fun registerManualEstimationRequest(user: TgUser, session: Session, request: UpdateRequest) {
        manualEstimationRequests["${session.id}_${user.id()}"] = request
    }

    fun dropManualEstimationRequest(user: TgUser, session: Session) {
        manualEstimationRequests.remove("${session.id}_${user.id()}")
    }
}
