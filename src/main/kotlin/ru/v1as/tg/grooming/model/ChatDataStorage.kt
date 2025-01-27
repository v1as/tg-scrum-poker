package ru.v1as.tg.grooming.model

import org.springframework.stereotype.Component
import ru.v1as.tg.starter.exceptions.TgMessageException
import ru.v1as.tg.starter.model.TgChat
import java.util.concurrent.ConcurrentHashMap

@Component
class ChatDataStorage {
    private val chatDates: MutableMap<Long, ChatData> = ConcurrentHashMap()

    fun getSession(chat: TgChat): Session? {
        return chatDates[chat.getId()]?.session
    }

    fun getSession(chatId: String, messageId: String): Session? {
        return chatDates[chatId.toLong()]?.session?.takeIf { it.messageId == messageId.toInt() }
    }

    fun getTasks(chat: TgChat) = chatDates.computeIfAbsent(chat.getId()) { ChatData() }.tasks

    fun newSession(title: String, chat: TgChat): Session {
        val chatData = chatDates.computeIfAbsent(chat.getId()) { ChatData() }
        val session = chatData.session
        session
            ?.takeIf { !it.closed }
            ?.apply { throw TgMessageException("Уже имеется открытый опрос.") }

        val newSession = Session(title, session?.voters() ?: emptySet(), chat.getId())
        chatData.session = newSession
        return newSession
    }
}

data class ChatData(var session: Session? = null, val tasks: MutableList<String> = mutableListOf())
