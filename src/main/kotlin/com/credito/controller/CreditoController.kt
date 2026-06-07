package com.credito.controller

import com.credito.dto.*
import com.credito.service.CreditoService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/creditos")
class CreditoController(private val service: CreditoService) {

    @GetMapping("/saldo/{parceiroId}")
    fun consultarSaldo(@PathVariable parceiroId: String): ResponseEntity<SaldoResponse> {
        return ResponseEntity.ok(service.consultarSaldo(parceiroId))
    }

    @PostMapping("/adicionar")
    fun adicionarCredito(@Valid @RequestBody request: CreditoRequest): ResponseEntity<TransacaoResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionarCredito(request))
    }

    @PostMapping("/consumir")
    fun consumirCredito(@Valid @RequestBody request: DebitoRequest): ResponseEntity<TransacaoResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.consumirCredito(request))
    }

    @GetMapping("/historico/{parceiroId}")
    fun historico(@PathVariable parceiroId: String): ResponseEntity<List<TransacaoResponse>> {
        return ResponseEntity.ok(service.historico(parceiroId))
    }
}
