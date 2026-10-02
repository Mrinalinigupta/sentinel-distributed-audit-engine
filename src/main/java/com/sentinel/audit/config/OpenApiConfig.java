package com.sentinel.audit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sentinelAuditOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sentinel - Distributed Audit & Compliance Engine API")
                        .description("Event-sourced, cryptographically sealed immutable audit ledger with Apache Kafka streaming, " +
                                     "SHA-256 hash chaining, Redis timeline caching, and automated tamper verification.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Sentinel Engineering")
                                .email("compliance@sentinel-engine.internal"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
