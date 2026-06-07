package com.credito.controller

import com.credito.dto.CreditoRequest
import com.credito.dto.DebitoRequest
import com.credito.dto.SaldoResponse
import com.credito.dto.TransacaoResponse
import com.credito.entity.StatusTransacao
import com.credito.entity.TipoTransacao
import com.credito.service.CreditoService
import com.credito.service.SaldoInsuficienteException
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@WebMvcTest(CreditoController::class)
class CreditoControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockBean
    lateinit var service: CreditoService

    @Test
    fun `GET saldo retorna 200 com saldo do parceiro`() {
        whenever(service.consultarSaldo("parceiro-001"))
            .thenReturn(SaldoResponse("parceiro-001", BigDecimal("750.00")))

        mockMvc.get("/api/creditos/saldo/parceiro-001")
            .andExpect {
                status { isOk() }
                jsonPath("$.parceiroId") { value("parceiro-001") }
                jsonPath("$.saldo") { value(750.00) }
            }
    }

    @Test
    fun `GET saldo parceiro inexistente retorna saldo zero`() {
        whenever(service.consultarSaldo("parceiro-inexistente"))
            .thenReturn(SaldoResponse("parceiro-inexistente", BigDecimal.ZERO))

        mockMvc.get("/api/creditos/saldo/parceiro-inexistente")
            .andExpect {
                status { isOk() }
                jsonPath("$.saldo") { value(0) }
            }
    }

    @Test
    fun `POST adicionar retorna 201 com transacao criada`() {
        val request = CreditoRequest("parceiro-001", BigDecimal("500.00"), "Venda lote", "chave-123")
        val response = TransacaoResponse(
            id = UUID.randomUUID(),
            parceiroId = "parceiro-001",
            tipo = TipoTransacao.CREDITO,
            valor = BigDecimal("500.00"),
            descricao = "Venda lote",
            status = StatusTransacao.CONCLUIDA,
            criadoEm = LocalDateTime.now()
        )
        whenever(service.adicionarCredito(any())).thenReturn(response)

        mockMvc.post("/api/creditos/adicionar") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.parceiroId") { value("parceiro-001") }
            jsonPath("$.tipo") { value("CREDITO") }
            jsonPath("$.valor") { value(500.00) }
            jsonPath("$.status") { value("CONCLUIDA") }
        }
    }

    @Test
    fun `POST adicionar com campos invalidos retorna 400`() {
        val body = mapOf("parceiroId" to "", "valor" to 0)

        mockMvc.post("/api/creditos/adicionar") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.erro") { exists() }
        }
    }

    @Test
    fun `POST adicionar sem parceiroId retorna 400`() {
        val body = """{"valor": 100.00}"""

        mockMvc.post("/api/creditos/adicionar") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST consumir retorna 201 com debito realizado`() {
        val request = DebitoRequest("parceiro-001", BigDecimal("100.00"), "Consumo API", "debito-123")
        val response = TransacaoResponse(
            id = UUID.randomUUID(),
            parceiroId = "parceiro-001",
            tipo = TipoTransacao.DEBITO,
            valor = BigDecimal("100.00"),
            descricao = "Consumo API",
            status = StatusTransacao.CONCLUIDA,
            criadoEm = LocalDateTime.now()
        )
        whenever(service.consumirCredito(any())).thenReturn(response)

        mockMvc.post("/api/creditos/consumir") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.tipo") { value("DEBITO") }
            jsonPath("$.status") { value("CONCLUIDA") }
        }
    }

    @Test
    fun `POST consumir com saldo insuficiente retorna 422`() {
        whenever(service.consumirCredito(any()))
            .thenThrow(SaldoInsuficienteException("Saldo insuficiente. Disponível: R$50.00, Solicitado: R$100.00"))

        val request = DebitoRequest("parceiro-001", BigDecimal("100.00"), "Consumo")

        mockMvc.post("/api/creditos/consumir") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isUnprocessableEntity() }
            jsonPath("$.erro") { value("Saldo insuficiente. Disponível: R\$50.00, Solicitado: R\$100.00") }
        }
    }

    @Test
    fun `POST consumir com campos invalidos retorna 400`() {
        val body = mapOf("parceiroId" to "parceiro-001", "valor" to -10)

        mockMvc.post("/api/creditos/consumir") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `GET historico retorna 200 com lista de transacoes`() {
        val transacoes = listOf(
            TransacaoResponse(UUID.randomUUID(), "parceiro-001", TipoTransacao.CREDITO, BigDecimal("200.00"), "Venda", StatusTransacao.CONCLUIDA, LocalDateTime.now()),
            TransacaoResponse(UUID.randomUUID(), "parceiro-001", TipoTransacao.DEBITO, BigDecimal("50.00"), "Consumo", StatusTransacao.CONCLUIDA, LocalDateTime.now())
        )
        whenever(service.historico("parceiro-001")).thenReturn(transacoes)

        mockMvc.get("/api/creditos/historico/parceiro-001")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].tipo") { value("CREDITO") }
                jsonPath("$[1].tipo") { value("DEBITO") }
            }
    }

    @Test
    fun `GET historico parceiro sem transacoes retorna lista vazia`() {
        whenever(service.historico("parceiro-novo")).thenReturn(emptyList())

        mockMvc.get("/api/creditos/historico/parceiro-novo")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }
    }
}
