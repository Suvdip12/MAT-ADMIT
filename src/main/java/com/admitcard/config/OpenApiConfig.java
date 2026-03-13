package com.admitcard.config;

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
                        .title("MAT Admit Card Generator API")
                        .version("1.0")
                        .description("API documentation for the Admit Card Generator application using Scalar UI.")
                        .contact(new Contact()
                                .name("Developer Support")
                                .email("support@example.com")));
    }
}
