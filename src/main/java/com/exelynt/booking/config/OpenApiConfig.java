package com.exelynt.booking.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Resource Booking API",
                version = "1.0.0",
                description = """
                        REST API for booking resources such as rooms, vehicles, and equipment.

                        Authenticate with POST /auth/login, then click Authorize and paste the
                        returned token to call the protected endpoints.

                        Seed accounts: admin/Admin@123 (ADMIN), alice/User@123 and bob/User@123 (USER).
                        """),
        servers = @Server(url = "/", description = "Default"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}
