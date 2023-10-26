package ru.v1as.tg.grooming.conf

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.bots.DefaultBotOptions
import java.net.Authenticator
import java.net.PasswordAuthentication

@Configuration
@EnableConfigurationProperties(TgProxyProps::class)
class ScrumPokerBotConf {

    @Bean
    @ConditionalOnProperty("tg.proxy.host")
    fun defaultBotOptions(proxy: TgProxyProps): DefaultBotOptions {
        val options = DefaultBotOptions()
        proxy.host.takeIf { it.isBlank() }?.let { throw IllegalArgumentException("Empty proxy host") }
        proxy.port.takeIf { it <= 0 }?.let { throw IllegalArgumentException("Invalid proxy port: $it") }
        options.proxyHost = proxy.host
        options.proxyPort = proxy.port
        options.proxyType = proxy.type
        if (proxy.username.isNotBlank() && proxy.password.isNotBlank()) {
            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(proxy.username, proxy.password.toCharArray())
            })
        }
        return options
    }
}
