package com.credito.config

import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfig {

    companion object {
        const val FILA_NOTIFICACOES = "transacoes.notificacoes"
    }

    @Bean
    fun filaNotificacoes(): Queue = Queue(FILA_NOTIFICACOES, true)
}
