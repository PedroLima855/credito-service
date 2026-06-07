package com.credito.service

import com.credito.config.RabbitConfig
import com.credito.entity.StatusTransacao
import com.credito.entity.TipoTransacao
import com.credito.entity.Transacao
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class NotificacaoServiceTest {

    @Mock
    lateinit var rabbitTemplate: RabbitTemplate

    @InjectMocks
    lateinit var service: NotificacaoService

    @Test
    fun `notificar envia mensagem para fila`() {
        val transacao = Transacao(
            parceiroId = "parceiro-001",
            tipo = TipoTransacao.CREDITO,
            valor = BigDecimal("250.00"),
            status = StatusTransacao.CONCLUIDA
        )

        service.notificar(transacao)

        verify(rabbitTemplate).convertAndSend(
            eq(RabbitConfig.FILA_NOTIFICACOES),
            argThat<String> { contains("parceiro-001") && contains("250.00") && contains("CREDITO") }
        )
    }

    @Test
    fun `notificar com falha no rabbit propaga excecao para retry`() {
        val transacao = Transacao(
            parceiroId = "parceiro-001",
            tipo = TipoTransacao.DEBITO,
            valor = BigDecimal("100.00"),
            status = StatusTransacao.CONCLUIDA
        )

        whenever(rabbitTemplate.convertAndSend(any<String>(), any<String>()))
            .thenThrow(RuntimeException("Connection refused"))

        org.junit.jupiter.api.assertThrows<RuntimeException> {
            service.notificar(transacao)
        }
    }
}
