package ru.v1as.tg.grooming.update

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.v1as.tg.grooming.model.COFFEE
import ru.v1as.tg.grooming.model.EstimationRole
import ru.v1as.tg.grooming.model.Session
import ru.v1as.tg.grooming.model.TIMER
import ru.v1as.tg.grooming.model.TURN_OVER
import ru.v1as.tg.grooming.update.callback.ESTIMATE_DAYS_CALLBACK
import ru.v1as.tg.grooming.update.callback.ESTIMATE_ROLE_CALLBACK
import ru.v1as.tg.starter.model.TgChat
import ru.v1as.tg.starter.update.answerCallbackQuery
import ru.v1as.tg.starter.update.callback.CallbackRequest
import ru.v1as.tg.starter.update.editMessageText
import ru.v1as.tg.starter.update.inlineKeyboardButton

var VALUES = listOf("1", "2", "3", "5", "8", "13", "21", COFFEE, TURN_OVER, TIMER)
var ROLES =
    listOf(EstimationRole("BA", "🔍"), EstimationRole("DEV", "🛠"), EstimationRole("QA", "💎"))
var DAYS = listOf("1", "2", "3", "4", "5", "10", "15", COFFEE)

fun intVoteValues(): List<Int> = VALUES.map { it.toIntOrNull() }.filterNotNull()

fun Message.replySendMessage(block: SendMessage.() -> Unit = {}): SendMessage {
    val srcMsg = this
    return SendMessage().apply {
        srcMsg.replyToMessage?.messageThreadId?.let { messageThreadId = it }
        chatId = srcMsg.chatId.toString()
        this.apply(block)
    }
}

fun buildSessionMessage(message: Message, session: Session) =
    message.replySendMessage {
        text = session.text()
        parseMode = ParseMode.MARKDOWN
        disableWebPagePreview = true
        replyMarkup = votingKeyboard()
    }

fun updateSessionMessage(session: Session): EditMessageText = editMessageText {
    this.chatId = session.chatId.toString()
    messageId = session.messageId
    text = session.text()
    parseMode = ParseMode.MARKDOWN
    disableWebPagePreview = true
    replyMarkup = votingKeyboard()
}

fun cleaningReplyMarkupSessionMessage(message: Message) = editMessageText {
    chatId = message.chatId.toString()
    messageId = message.messageId
    text = message.text
    replyMarkup = columnInlineKeyboardMarkup()
}

fun cleaningReplyMarkupSessionMessage(chat: TgChat, session: Session) = editMessageText {
    chatId = chat.getId().toString()
    messageId = session.messageId
    parseMode = ParseMode.MARKDOWN
    text = session.text()
    replyMarkup = columnInlineKeyboardMarkup()
}

fun chosenByCallbackEditMessageText(update: Update, choose: String) = editMessageText {
    val prevMsg = update.callbackQuery.message
    val chooseText =
        update.callbackQuery.message.replyMarkup.keyboard
            .flatten()
            .find { button -> button.callbackData.endsWith(choose) }
            ?.text ?: choose
    text = "${prevMsg.text}\n[Выбор: $chooseText]"
    messageId = prevMsg.messageId
    chatId = prevMsg.chatId.toString()
    replyMarkup = InlineKeyboardMarkup(listOf())
}

fun columnInlineKeyboardMarkup(vararg buttons: Pair<String, String>): InlineKeyboardMarkup {
    return InlineKeyboardMarkup(
        listOf(
            buttons.map {
                inlineKeyboardButton {
                    text = it.first
                    callbackData = it.second
                }
            }))
}

fun answerCallback(callbackRequest: CallbackRequest, text: String) = answerCallbackQuery {
    callbackQueryId = callbackRequest.callbackQueryId()
    this.text = text
}

fun votingKeyboard(): InlineKeyboardMarkup {
    val rows = mutableListOf<List<InlineKeyboardButton>>()
    var tempRow = mutableListOf<InlineKeyboardButton>()
    for (value in VALUES) {
        if (tempRow.size == 2) {
            rows += tempRow
            tempRow = mutableListOf()
        }
        tempRow += inlineKeyboardButton {
            text = value
            callbackData = "vote_$value"
        }
    }
    if (tempRow.isNotEmpty()) {
        rows += tempRow
    }
    return InlineKeyboardMarkup(rows)
}

fun rolesKeyboard(chatId: String, messageId: String): InlineKeyboardMarkup {
    var row = mutableListOf<InlineKeyboardButton>()
    for (role in ROLES) {
        row += inlineKeyboardButton {
            text = "${role.emoji} ${role.name}"
            callbackData = "${ESTIMATE_ROLE_CALLBACK}${chatId}_${messageId}_${role.name}"
        }
    }
    return InlineKeyboardMarkup(listOf(row))
}

fun estimationDaysKeyboard(chatId: String, messageId: String, role: String): InlineKeyboardMarkup {
    var row = mutableListOf<InlineKeyboardButton>()
    val rows = mutableListOf(row)
    for (days in DAYS) {
        if (row.size >= 4) {
            row = mutableListOf()
            rows += row
        }
        row += inlineKeyboardButton {
            text = days
            callbackData = "${ESTIMATE_DAYS_CALLBACK}${chatId}_${messageId}_${role}_${days}"
        }
    }
    return InlineKeyboardMarkup(rows)
}

@Component
class TgMethods(
    @Value("\${scrum.values}") val valuesStr: String,
    @Value("\${scrum.roles}") val estimateRoles: String
) {
    companion object : KLogging()

    init {
        VALUES =
            valuesStr
                .split(",")
                .toMutableList()
                .also {
                    it.add(COFFEE)
                    it.add(TURN_OVER)
                }
                .toList()
        ROLES =
            estimateRoles
                .split(",")
                .map { EstimationRole(it.split(" ")[0], it.split(" ")[1]) }
                .toList()
        logger.info { "Values: $VALUES, roles: $ROLES" }
    }
}
