package ru.v1as.tg.grooming

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GroomingBotApplication

fun main(args: Array<String>) {
    runApplication<GroomingBotApplication>(*args)
}
