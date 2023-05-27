package ru.v1as.tg.grooming

import java.time.LocalDateTime
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import ru.v1as.tg.starter.exceptions.TgMessageException
import ru.v1as.tg.starter.model.TgUser

class GroomingSession(val welcome: Message) {
    private val users: MutableMap<TgUser, LocalDateTime> = mutableMapOf()
    var active = true

    fun join(tgUser: TgUser): EditMessageText {
        if (!active) {
            throw TgMessageException("Grooming session is finished.")
        }
        if (this.users.contains(tgUser)) {
            throw TgMessageException("You are already joined.")
        }
        users[tgUser] = LocalDateTime.now()
        return editMessageText()
    }

    fun leave(tgUser: TgUser): EditMessageText {
        users.remove(tgUser)
        return editMessageText()
    }

    private fun editMessageText(): EditMessageText {
        val users =
            users.entries.joinToString("\n") {
                it.key.usernameOrFullName() + ": " + it.value.toLocalTime()
            }
        val status = if (active) "in process..." else "finished"
        val edit = EditMessageText("Grooming session is $status\n\n$users")
        edit.chatId = welcome.chatId.toString()
        edit.messageId = welcome.messageId
        edit.replyMarkup = welcome.replyMarkup
        return edit
    }

    fun finish(): EditMessageText {
        active = false
        val editMessage = editMessageText()
        editMessage.replyMarkup = null
        return editMessage
    }
}