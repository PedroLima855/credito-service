package com.credito.dto

import com.credito.entity.StatusTransacao
import com.credito.entity.TipoTransacao
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

data class CreditoRequest(
    @field:NotBlank val parceiroId: String,
    @field:NotNull @field:DecimalMin("0.01") val valor: BigDecimal,
    val descricao: String? = null,
    val chaveIdempotencia: String? = null
)

data class DebitoRequest(
    @field:NotBlank val parceiroId: String,
    @field:NotNull @field:DecimalMin("0.01") val valor: BigDecimal,
    val descricao: String? = null,
    val chaveIdempotencia: String? = null
)

data class SaldoResponse(
    val parceiroId: String,
    val saldo: BigDecimal
)

data class TransacaoResponse(
    val id: UUID,
    val parceiroId: String,
    val tipo: TipoTransacao,
    val valor: BigDecimal,
    val descricao: String?,
    val status: StatusTransacao,
    val criadoEm: LocalDateTime
)
