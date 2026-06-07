package com.credito.repository

import com.credito.entity.StatusTransacao
import com.credito.entity.Transacao
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

interface TransacaoRepository : JpaRepository<Transacao, UUID> {

    fun findByParceiroIdOrderByCriadoEmDesc(parceiroId: String): List<Transacao>

    fun findByChaveIdempotencia(chaveIdempotencia: String): Transacao?

    fun findByStatusAndCriadoEmBefore(status: StatusTransacao, antes: LocalDateTime): List<Transacao>

    @Query("""
        SELECT COALESCE(
            (SELECT SUM(t.valor) FROM Transacao t WHERE t.parceiroId = :parceiroId AND t.tipo = 'CREDITO' AND t.status = 'CONCLUIDA'), 0
        ) - COALESCE(
            (SELECT SUM(t.valor) FROM Transacao t WHERE t.parceiroId = :parceiroId AND t.tipo = 'DEBITO' AND t.status = 'CONCLUIDA'), 0
        )
    """)
    fun calcularSaldo(parceiroId: String): BigDecimal

    @Query(
        value = """SELECT COALESCE(SUM(CASE WHEN tipo = 'CREDITO' THEN valor ELSE -valor END), 0)
                   FROM transacoes
                   WHERE parceiro_id = :parceiroId AND status = 'CONCLUIDA'
                   FOR UPDATE""",
        nativeQuery = true
    )
    fun calcularSaldoComLock(parceiroId: String): BigDecimal
}
