package com.credito.service

import com.credito.config.RabbitConfig
import com.credito.entity.Transacao
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service

@Service
class NotificacaoService(private val rabbitTemplate: RabbitTemplate) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 1000, multiplier = 2.0))
    fun notificar(transacao: Transacao) {
        val mensagem = "Transacao ${transacao.id} | ${transacao.tipo} | R$${transacao.valor} | Parceiro: ${transacao.parceiroId}"
        rabbitTemplate.convertAndSend(RabbitConfig.FILA_NOTIFICACOES, mensagem)
        log.info("Notificação enviada: {}", mensagem)
    }

    @Recover
    fun recuperarNotificacao(ex: Exception, transacao: Transacao) {
        log.error("Falha definitiva ao notificar transação {} após 3 tentativas: {}", transacao.id, ex.message)
    }
}
