package ru.v1as.tg.grooming

import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import ru.v1as.tg.starter.exceptions.TgMessageException
import ru.v1as.tg.starter.model.TgChat
import ru.v1as.tg.starter.model.TgUser

@Component
class ChatDataStorage {
    val chatDates: MutableMap<Long, GroomingSession> = ConcurrentHashMap()
    val userToChat: MutableMap<TgUser, Long> = ConcurrentHashMap()

    fun getData(chatId: Long) = chatDates[chatId]
    fun contains(chat: TgChat) = chatDates.contains(chat.getId())

    fun setData(chat: TgChat, groomingSession: GroomingSession): GroomingSession? {
        return chatDates.put(chat.getId(), groomingSession)
    }

    fun bind(user: TgUser, chatId: Long) {
        if (userToChat.contains(user)) {
            throw TgMessageException("You are already in other chat. User /leave command.")
        }
        userToChat[user] = chatId
    }

    fun leave(user: TgUser): EditMessageText {
        val chatId = userToChat.remove(user) ?: throw TgMessageException("You are not joined.")
        val groomingSession = chatDates[chatId] ?: throw TgMessageException("Oops!")
        return groomingSession.leave(user)
    }
}
