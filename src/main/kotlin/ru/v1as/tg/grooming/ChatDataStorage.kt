package ru.v1as.tg.grooming

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import ru.v1as.tg.starter.exceptions.TgMessageException
import ru.v1as.tg.starter.model.TgChat
import ru.v1as.tg.starter.model.TgUser
import java.util.concurrent.ConcurrentHashMap

@Component
class ChatDataStorage {
    val chatDates: MutableMap<Long, Session> = ConcurrentHashMap()

    fun newVote(message: Message): List<EditMessageText> {
        val edits = mutableListOf<EditMessageText>()
        val chatId = message.chat.id
        val session = chatDates[chatId]?.also {
            if (!it.closed) {
                edits.add(it.close())
            }
        }
        val newSession = Session(message, session?.voters() ?: emptySet())
        chatDates[chatId] = newSession
        edits.add(newSession.editMessage())
        return edits
    }

    fun vote(value: String, chat: TgChat, user: TgUser): EditMessageText? {
        val session = chatDates[chat.getId()] ?: throw TgMessageException("Что-то пошло не так")
        if (!session.closed) {
            return session.vote(value, user)
        }
        return null
    }
}
