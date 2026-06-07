package com.credito.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "transacoes")
data class Transacao(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "parceiro_id", nullable = false)
    val parceiroId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tipo: TipoTransacao,

    @Column(nullable = false, precision = 15, scale = 2)
    val valor: BigDecimal,

    @Column(length = 500)
    val descricao: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: StatusTransacao = StatusTransacao.PENDENTE,

    @Column(name = "chave_idempotencia", unique = true)
    val chaveIdempotencia: String? = null,

    @Column(name = "criado_em", nullable = false)
    val criadoEm: LocalDateTime = LocalDateTime.now(),

    @Column(name = "atualizado_em", nullable = false)
    var atualizadoEm: LocalDateTime = LocalDateTime.now()
)

enum class TipoTransacao { CREDITO, DEBITO }
enum class StatusTransacao { PENDENTE, CONCLUIDA, FALHA }
