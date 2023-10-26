package ru.v1as.tg.grooming.conf

import org.springframework.boot.context.properties.ConfigurationProperties
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.DefaultBotOptions.ProxyType.NO_PROXY

@ConfigurationProperties(prefix = "tg.proxy")
class TgProxyProps(
    var host: String = "",
    var port: Int = -1,
    var username: String = "",
    var password: String = "",
    var type: DefaultBotOptions.ProxyType = NO_PROXY
)
