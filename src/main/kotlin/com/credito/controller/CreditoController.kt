package com.credito.controller

import com.credito.dto.*
import com.credito.service.CreditoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/creditos")
@Tag(name = "Créditos", description = "Gestão de créditos de parceiros B2B")
class CreditoController(private val service: CreditoService) {

    @GetMapping("/saldo/{parceiroId}")
    @Operation(summary = "Consultar saldo", description = "Retorna o saldo atual de créditos do parceiro")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Saldo consultado com sucesso")
    )
    fun consultarSaldo(@PathVariable parceiroId: String): ResponseEntity<SaldoResponse> {
        return ResponseEntity.ok(service.consultarSaldo(parceiroId))
    }

    @PostMapping("/adicionar")
    @Operation(summary = "Adicionar crédito", description = "Adiciona créditos ao saldo do parceiro (ex: venda reportada)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Crédito adicionado com sucesso"),
        ApiResponse(responseCode = "400", description = "Dados inválidos")
    )
    fun adicionarCredito(@Valid @RequestBody request: CreditoRequest): ResponseEntity<TransacaoResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionarCredito(request))
    }

    @PostMapping("/consumir")
    @Operation(summary = "Consumir crédito", description = "Debita créditos do saldo do parceiro com validação de saldo disponível")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Débito realizado com sucesso"),
        ApiResponse(responseCode = "400", description = "Dados inválidos"),
        ApiResponse(responseCode = "422", description = "Saldo insuficiente")
    )
    fun consumirCredito(@Valid @RequestBody request: DebitoRequest): ResponseEntity<TransacaoResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.consumirCredito(request))
    }

    @GetMapping("/historico/{parceiroId}")
    @Operation(summary = "Histórico de transações", description = "Retorna todas as transações do parceiro ordenadas por data (mais recente primeiro)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso")
    )
    fun historico(@PathVariable parceiroId: String): ResponseEntity<List<TransacaoResponse>> {
        return ResponseEntity.ok(service.historico(parceiroId))
    }
}
