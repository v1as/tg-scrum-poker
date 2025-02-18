package ru.v1as.tg.grooming.model

import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Component
import ru.v1as.tg.starter.exceptions.TgMessageException
import ru.v1as.tg.starter.model.TgChat

@Component
class ChatDataStorage {
    private val chatDates: MutableMap<Long, ChatData> = ConcurrentHashMap()

    fun getSession(chat: TgChat): Session? {
        return chatDates[chat.getId()]?.session
    }

    fun getSession(chatId: String, sessionId: String): Session? {
        return chatDates[chatId.toLong()]?.session?.takeIf { it.id == sessionId.toInt() }
    }

    fun getTasks(chat: TgChat) = chatDates.computeIfAbsent(chat.getId()) { ChatData() }.tasks

    fun newSession(title: String, chat: TgChat): Session {
        val chatData = chatDates.computeIfAbsent(chat.getId()) { ChatData() }
        val session = chatData.session
        session
            ?.takeIf { !it.closed }
            ?.apply { throw TgMessageException("Уже имеется открытый опрос.") }

        val newSession = Session(title, chat.getId(), session?.voters() ?: emptySet())
        chatData.session = newSession
        return newSession
    }
}

data class ChatData(var session: Session? = null, val tasks: MutableList<String> = mutableListOf())
