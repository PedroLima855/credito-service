package com.credito.service

import com.credito.dto.CreditoRequest
import com.credito.dto.DebitoRequest
import com.credito.entity.StatusTransacao
import com.credito.entity.TipoTransacao
import com.credito.entity.Transacao
import com.credito.repository.TransacaoRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class CreditoServiceTest {

    @Mock
    lateinit var repository: TransacaoRepository

    @Mock
    lateinit var notificacaoService: NotificacaoService

    @InjectMocks
    lateinit var service: CreditoService

    @Test
    fun `consultarSaldo retorna saldo do parceiro`() {
        whenever(repository.calcularSaldo("parceiro-001")).thenReturn(BigDecimal("500.00"))

        val resultado = service.consultarSaldo("parceiro-001")

        assertEquals("parceiro-001", resultado.parceiroId)
        assertEquals(BigDecimal("500.00"), resultado.saldo)
    }

    @Test
    fun `consultarSaldo retorna zero para parceiro sem transacoes`() {
        whenever(repository.calcularSaldo("parceiro-novo")).thenReturn(BigDecimal.ZERO)

        val resultado = service.consultarSaldo("parceiro-novo")

        assertEquals(BigDecimal.ZERO, resultado.saldo)
    }

    @Test
    fun `adicionarCredito cria transacao com sucesso`() {
        val request = CreditoRequest("parceiro-001", BigDecimal("100.00"), "Venda", "chave-1")

        whenever(repository.findByChaveIdempotencia("chave-1")).thenReturn(null)
        whenever(repository.save(any<Transacao>())).thenAnswer { it.arguments[0] as Transacao }

        val resultado = service.adicionarCredito(request)

        assertEquals("parceiro-001", resultado.parceiroId)
        assertEquals(TipoTransacao.CREDITO, resultado.tipo)
        assertEquals(BigDecimal("100.00"), resultado.valor)
        assertEquals(StatusTransacao.CONCLUIDA, resultado.status)
        verify(notificacaoService).notificar(any())
    }

    @Test
    fun `adicionarCredito com idempotencia retorna transacao existente`() {
        val existente = Transacao(
            parceiroId = "parceiro-001",
            tipo = TipoTransacao.CREDITO,
            valor = BigDecimal("100.00"),
            status = StatusTransacao.CONCLUIDA,
            chaveIdempotencia = "chave-dup"
        )
        whenever(repository.findByChaveIdempotencia("chave-dup")).thenReturn(existente)

        val request = CreditoRequest("parceiro-001", BigDecimal("100.00"), "Venda", "chave-dup")
        val resultado = service.adicionarCredito(request)

        assertEquals(existente.id, resultado.id)
        verify(repository, never()).save(any())
        verify(notificacaoService, never()).notificar(any())
    }

    @Test
    fun `adicionarCredito sem chave idempotencia cria normalmente`() {
        val request = CreditoRequest("parceiro-001", BigDecimal("200.00"), "Bônus")

        whenever(repository.save(any<Transacao>())).thenAnswer { it.arguments[0] as Transacao }

        val resultado = service.adicionarCredito(request)

        assertEquals(BigDecimal("200.00"), resultado.valor)
        assertEquals("Bônus", resultado.descricao)
        verify(repository, never()).findByChaveIdempotencia(any())
    }

    @Test
    fun `consumirCredito com saldo suficiente debita com sucesso`() {
        val request = DebitoRequest("parceiro-001", BigDecimal("50.00"), "Consumo", "debito-1")

        whenever(repository.findByChaveIdempotencia("debito-1")).thenReturn(null)
        whenever(repository.calcularSaldoComLock("parceiro-001")).thenReturn(BigDecimal("500.00"))
        whenever(repository.save(any<Transacao>())).thenAnswer { it.arguments[0] as Transacao }

        val resultado = service.consumirCredito(request)

        assertEquals(TipoTransacao.DEBITO, resultado.tipo)
        assertEquals(StatusTransacao.CONCLUIDA, resultado.status)
        assertEquals(BigDecimal("50.00"), resultado.valor)
        verify(notificacaoService).notificar(any())
    }

    @Test
    fun `consumirCredito com saldo insuficiente lanca excecao`() {
        val request = DebitoRequest("parceiro-001", BigDecimal("1000.00"), "Consumo grande", "debito-2")

        whenever(repository.findByChaveIdempotencia("debito-2")).thenReturn(null)
        whenever(repository.calcularSaldoComLock("parceiro-001")).thenReturn(BigDecimal("100.00"))
        whenever(repository.save(any<Transacao>())).thenAnswer { it.arguments[0] as Transacao }

        val ex = assertThrows<SaldoInsuficienteException> {
            service.consumirCredito(request)
        }

        assertTrue(ex.message!!.contains("Saldo insuficiente"))
        verify(notificacaoService, never()).notificar(any())
    }

    @Test
    fun `consumirCredito com idempotencia retorna transacao existente`() {
        val existente = Transacao(
            parceiroId = "parceiro-001",
            tipo = TipoTransacao.DEBITO,
            valor = BigDecimal("50.00"),
            status = StatusTransacao.CONCLUIDA,
            chaveIdempotencia = "debito-dup"
        )
        whenever(repository.findByChaveIdempotencia("debito-dup")).thenReturn(existente)

        val request = DebitoRequest("parceiro-001", BigDecimal("50.00"), "Consumo", "debito-dup")
        val resultado = service.consumirCredito(request)

        assertEquals(existente.id, resultado.id)
        verify(repository, never()).calcularSaldoComLock(any())
    }

    @Test
    fun `historico retorna transacoes do parceiro`() {
        val transacoes = listOf(
            Transacao(parceiroId = "parceiro-001", tipo = TipoTransacao.CREDITO, valor = BigDecimal("100.00"), status = StatusTransacao.CONCLUIDA),
            Transacao(parceiroId = "parceiro-001", tipo = TipoTransacao.DEBITO, valor = BigDecimal("30.00"), status = StatusTransacao.CONCLUIDA)
        )
        whenever(repository.findByParceiroIdOrderByCriadoEmDesc("parceiro-001")).thenReturn(transacoes)

        val resultado = service.historico("parceiro-001")

        assertEquals(2, resultado.size)
        assertEquals(TipoTransacao.CREDITO, resultado[0].tipo)
        assertEquals(TipoTransacao.DEBITO, resultado[1].tipo)
    }

    @Test
    fun `historico retorna lista vazia para parceiro sem transacoes`() {
        whenever(repository.findByParceiroIdOrderByCriadoEmDesc("parceiro-novo")).thenReturn(emptyList())

        val resultado = service.historico("parceiro-novo")

        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `conciliarTransacoesPendentes atualiza transacoes antigas`() {
        val pendentes = listOf(
            Transacao(
                parceiroId = "parceiro-001",
                tipo = TipoTransacao.CREDITO,
                valor = BigDecimal("100.00"),
                status = StatusTransacao.PENDENTE,
                criadoEm = LocalDateTime.now().minusMinutes(10)
            )
        )
        whenever(repository.findByStatusAndCriadoEmBefore(eq(StatusTransacao.PENDENTE), any())).thenReturn(pendentes)
        whenever(repository.save(any<Transacao>())).thenAnswer { it.arguments[0] as Transacao }

        service.conciliarTransacoesPendentes()

        verify(repository).save(argThat<Transacao> { status == StatusTransacao.CONCLUIDA })
    }

    @Test
    fun `conciliarTransacoesPendentes nao faz nada sem pendentes`() {
        whenever(repository.findByStatusAndCriadoEmBefore(eq(StatusTransacao.PENDENTE), any())).thenReturn(emptyList())

        service.conciliarTransacoesPendentes()

        verify(repository, never()).save(any())
    }
}
