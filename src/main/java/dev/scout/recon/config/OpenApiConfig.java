package dev.scout.recon.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI reconOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Recon - AI Code Reviewer API")
                        .description("API for automated code reviews powered by Spring AI and GitHub integration.")
                        .version("0.0.1")
                        .contact(new Contact()
                                .name("Scout")
                                .url("https://github.com/scout"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
