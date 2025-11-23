package com.gilmotech.emailservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gilmotech Email Service API")
                        .version("1.0.0")
                        .description("API d'envoi d'emails pour Gilmotech / Assurantis")
                        .contact(new Contact()
                                .name("Support Gilmotech")
                                .email("contact@gilmotech.be")
                        )
                );
    }
}

//j'ai mis dans le YML springdoc:
//  swagger-ui:
//    path: /api/docs
//dc pas http://localhost:8080/swagger-ui/index.html
//mais alors
// http://localhost:8080/api/swagger-ui/index.html