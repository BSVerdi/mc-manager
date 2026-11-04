package br.com.mc_manager.server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class OpenApiConfig {

  @Bean
  @Profile("!prod")
  public OpenAPI api() {
    return new OpenAPI()
            .info(new Info()
                    .title("Mc-Manager API")
                    .description("Mc-Manager API Documentation")
                    .version("v1.0.0")
            ).components(new Components().addSecuritySchemes(
                            "bearerAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("Bearer")
                    )
            ).addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }
}
