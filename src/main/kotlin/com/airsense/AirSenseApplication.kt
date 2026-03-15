package com.airsense

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class AirSenseApplication

fun main(args: Array<String>) {
    runApplication<AirSenseApplication>(*args)
}
