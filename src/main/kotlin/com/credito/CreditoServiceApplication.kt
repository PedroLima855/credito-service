package com.credito

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableRetry
class CreditoServiceApplication

fun main(args: Array<String>) {
	runApplication<CreditoServiceApplication>(*args)
}
