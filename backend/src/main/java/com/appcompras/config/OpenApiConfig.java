package com.appcompras.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI appOpenApi() {
        Components components = new Components()
                .addSchemas("ApiError", new ObjectSchema()
                        .addProperty("timestamp", new StringSchema().description("UTC timestamp"))
                        .addProperty("status", new io.swagger.v3.oas.models.media.IntegerSchema().example(400))
                        .addProperty("code", new StringSchema().example("BUSINESS_RULE_VIOLATION"))
                        .addProperty("error", new StringSchema().example("Human-readable error message"))
                        .addProperty("path", new StringSchema().example("/api/recipes"))
                        .addProperty("requestId", new StringSchema().example("6d7b57f9-8f6e-4fb2-8f88-5d4a7e4d25a4"))
                        .required(java.util.List.of("timestamp", "status", "code", "error", "path"))
                )
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .name("bearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .info(new Info()
                        .title("AppCompras Backend API")
                        .version("v1")
                        .description("Week Meal Prep + Shopping List backend API. Optional header: X-API-Version=1"))
                .components(components)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .schemaRequirement("bearerAuth", components.getSecuritySchemes().get("bearerAuth"));
    }

    @Bean
    OpenApiCustomizer standardizeApiDocsCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> customizePath(path, pathItem));
        };
    }

    private void customizePath(String path, PathItem pathItem) {
        if (!path.startsWith("/api/")) {
            return;
        }
        pathItem.readOperations().forEach(this::customizeOperation);
    }

    private void customizeOperation(Operation operation) {
        addVersionHeader(operation);
        addStandardErrorResponse(operation, "400", "Bad Request", "BUSINESS_RULE_VIOLATION", "Invalid business input");
        addStandardErrorResponse(operation, "401", "Unauthorized", "UNAUTHORIZED", "Authentication required");
        addStandardErrorResponse(operation, "403", "Forbidden", "FORBIDDEN", "Insufficient permissions");
        addStandardErrorResponse(operation, "404", "Not Found", "RESOURCE_NOT_FOUND", "Resource not found");
    }

    private void addVersionHeader(Operation operation) {
        if (operation.getParameters() != null && operation.getParameters().stream()
                .anyMatch(p -> "X-API-Version".equalsIgnoreCase(p.getName()))) {
            return;
        }

        StringSchema versionSchema = new StringSchema();
        versionSchema.addEnumItemObject("1");
        versionSchema.addEnumItemObject("v1");

        Parameter versionHeader = new Parameter()
                .in("header")
                .name("X-API-Version")
                .required(false)
                .description("Optional API version header. Supported values: 1, v1")
                .schema(versionSchema)
                .example("1");

        operation.addParametersItem(versionHeader);
    }

    private void addStandardErrorResponse(
            Operation operation,
            String status,
            String description,
            String code,
            String message
    ) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        if (responses.containsKey(status)) {
            return;
        }

        Example errorExample = new Example().value(Map.of(
                "timestamp", "2026-02-08T12:00:00Z",
                "status", Integer.parseInt(status),
                "code", code,
                "error", message,
                "path", "/api/example",
                "requestId", "b5cb35b0-4223-4f3d-b4bc-249d7a37c5ef"
        ));

        MediaType mediaType = new MediaType()
                .schema(new ObjectSchema().$ref("#/components/schemas/ApiError"))
                .addExamples("default", errorExample);

        ApiResponse response = new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json", mediaType));

        responses.addApiResponse(status, response);
    }
}
