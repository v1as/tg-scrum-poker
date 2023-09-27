package ru.v1as.tg.grooming.model

import org.springframework.stereotype.Component
import ru.v1as.tg.starter.model.TgChat
import java.util.concurrent.ConcurrentHashMap

@Component
class ChatDataStorage {
    private val chatDates: MutableMap<Long, Session> = ConcurrentHashMap()

    fun getSession(chat: TgChat): Session? {
        return chatDates[chat.getId()]
    }

    fun newSession(title: String, chat: TgChat): Session {
        val chatId = chat.getId()
        val session = chatDates[chatId]
        session?.takeIf { !it.closed }?.apply { throw IllegalStateException("Not closed session.") }

        val newSession = Session(title, session?.voters() ?: emptySet())
        chatDates[chatId] = newSession
        return newSession
    }
}
