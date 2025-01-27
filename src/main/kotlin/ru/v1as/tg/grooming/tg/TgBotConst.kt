package ru.v1as.tg.grooming.tg

import org.springframework.stereotype.Component
import ru.v1as.tg.starter.TgBotProperties

private val START_URL_FORMAT = "https://telegram.me/%s?start=%s"


@Component
class TgBotConst(private val botProps: TgBotProperties) {

    companion object {
        private var botName = ""

        fun getStartLink(argument: String) = START_URL_FORMAT.format(botName, argument)
    }

    init {
        botName = botProps.username
    }

}