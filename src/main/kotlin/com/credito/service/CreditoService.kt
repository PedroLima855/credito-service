package com.credito.service

import com.credito.dto.*
import com.credito.entity.StatusTransacao
import com.credito.entity.TipoTransacao
import com.credito.entity.Transacao
import com.credito.repository.TransacaoRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class CreditoService(
    private val repository: TransacaoRepository,
    private val notificacaoService: NotificacaoService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Cacheable(value = ["saldos"], key = "#parceiroId")
    fun consultarSaldo(parceiroId: String): SaldoResponse {
        val saldo = repository.calcularSaldo(parceiroId)
        return SaldoResponse(parceiroId, saldo)
    }

    @Transactional
    @CacheEvict(value = ["saldos"], key = "#request.parceiroId")
    fun adicionarCredito(request: CreditoRequest): TransacaoResponse {
        request.chaveIdempotencia?.let { chave ->
            repository.findByChaveIdempotencia(chave)?.let { return it.toResponse() }
        }

        val transacao = Transacao(
            parceiroId = request.parceiroId,
            tipo = TipoTransacao.CREDITO,
            valor = request.valor,
            descricao = request.descricao,
            status = StatusTransacao.CONCLUIDA,
            chaveIdempotencia = request.chaveIdempotencia
        )

        val salva = repository.save(transacao)
        notificacaoService.notificar(salva)
        return salva.toResponse()
    }

    @Transactional
    @CacheEvict(value = ["saldos"], key = "#request.parceiroId")
    fun consumirCredito(request: DebitoRequest): TransacaoResponse {
        request.chaveIdempotencia?.let { chave ->
            repository.findByChaveIdempotencia(chave)?.let { return it.toResponse() }
        }

        // Lock pessimista para evitar race condition em débitos concorrentes
        val saldoAtual = repository.calcularSaldoComLock(request.parceiroId)

        if (saldoAtual < request.valor) {
            val transacao = Transacao(
                parceiroId = request.parceiroId,
                tipo = TipoTransacao.DEBITO,
                valor = request.valor,
                descricao = request.descricao,
                status = StatusTransacao.FALHA,
                chaveIdempotencia = request.chaveIdempotencia
            )
            repository.save(transacao)
            throw SaldoInsuficienteException("Saldo insuficiente. Disponível: R$$saldoAtual, Solicitado: R$${request.valor}")
        }

        val transacao = Transacao(
            parceiroId = request.parceiroId,
            tipo = TipoTransacao.DEBITO,
            valor = request.valor,
            descricao = request.descricao,
            status = StatusTransacao.CONCLUIDA,
            chaveIdempotencia = request.chaveIdempotencia
        )

        val salva = repository.save(transacao)
        notificacaoService.notificar(salva)
        return salva.toResponse()
    }

    fun historico(parceiroId: String): List<TransacaoResponse> {
        return repository.findByParceiroIdOrderByCriadoEmDesc(parceiroId).map { it.toResponse() }
    }

    @Scheduled(fixedDelay = 60000)
    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 2000))
    @Transactional
    fun conciliarTransacoesPendentes() {
        val limite = LocalDateTime.now().minusMinutes(5)
        val pendentes = repository.findByStatusAndCriadoEmBefore(StatusTransacao.PENDENTE, limite)

        if (pendentes.isEmpty()) return

        log.info("Conciliando {} transações pendentes", pendentes.size)
        pendentes.forEach { transacao ->
            transacao.status = StatusTransacao.CONCLUIDA
            transacao.atualizadoEm = LocalDateTime.now()
            repository.save(transacao)
            log.info("Transação {} conciliada com sucesso", transacao.id)
        }
    }

    private fun Transacao.toResponse() = TransacaoResponse(
        id = id,
        parceiroId = parceiroId,
        tipo = tipo,
        valor = valor,
        descricao = descricao,
        status = status,
        criadoEm = criadoEm
    )
}

class SaldoInsuficienteException(message: String) : RuntimeException(message)
