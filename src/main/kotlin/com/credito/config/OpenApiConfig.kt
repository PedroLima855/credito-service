package com.credito.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Crédito Service API")
                .description("Microserviço de gestão de créditos para parceiros B2B")
                .version("1.0.0")
                .contact(Contact().name("Equipe Crédito").email("credito@empresa.com"))
        )
}
