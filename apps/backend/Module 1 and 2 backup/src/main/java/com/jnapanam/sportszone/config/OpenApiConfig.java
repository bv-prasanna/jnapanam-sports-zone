package com.jnapanam.sportszone.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
    @Bean OpenAPI openAPI() {
        String scheme = "bearerAuth";
        return new OpenAPI().info(new Info().title("Jnapanam Sports Zone API").version("v1").description("Backend APIs for web, Android, iOS and operations applications"))
                .components(new Components().addSecuritySchemes(scheme, new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(scheme));
    }
}
