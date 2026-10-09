package br.com.sauf_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI saufOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API SAUF-BR")
                        .version("v1")
                        .description("Catálogo de cursos e universidades do SAUF-BR."));
    }
}
