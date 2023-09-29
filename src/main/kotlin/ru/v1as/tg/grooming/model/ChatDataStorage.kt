package ru.v1as.tg.grooming.model

import org.springframework.stereotype.Component
import ru.v1as.tg.starter.model.TgChat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class ChatDataStorage {
    private val chatDates: MutableMap<Long, ChatData> = ConcurrentHashMap()

    fun getSession(chat: TgChat): Session? {
        return chatDates[chat.getId()]?.session
    }

    fun newSession(title: String, chat: TgChat): Session {
        val chatId = chat.getId()
        val chatData = chatDates.computeIfAbsent(chatId) { ChatData() }
        val session = chatData.session
        session?.takeIf { !it.closed }?.apply { throw IllegalStateException("Not closed session.") }

        val newSession = Session(title, session?.voters() ?: emptySet())
        chatData.session = newSession
        return newSession
    }
}

data class ChatData(
    var session: Session? = null,
    val tasks: MutableList<StringJoiner> = mutableListOf()
)
