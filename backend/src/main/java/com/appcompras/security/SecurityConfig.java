package com.appcompras.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            @Value("${app.security.require-auth:true}") boolean requireAuth,
            SecurityErrorHandlers securityErrorHandlers
    ) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (requireAuth) {
            http
                    .authorizeHttpRequests(authz -> authz
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers(
                                    "/swagger-ui.html",
                                    "/swagger-ui/**",
                                    "/v3/api-docs/**",
                                    "/actuator/health",
                                    "/actuator/info"
                            ).permitAll()
                            .requestMatchers("/api/**").authenticated()
                            .anyRequest().permitAll())
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(securityErrorHandlers)
                            .accessDeniedHandler(securityErrorHandlers))
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                    }));
        } else {
            http.authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
        }

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.require-auth", havingValue = "true", matchIfMissing = true)
    JwtDecoder jwtDecoder(@Value("${app.security.google-client-id:}") String googleClientId) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("Missing app.security.google-client-id");
        }

        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(GOOGLE_ISSUER);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(GOOGLE_ISSUER);
        OAuth2TokenValidator<Jwt> withAudience = jwt -> jwt.getAudience().contains(googleClientId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));

        ((org.springframework.security.oauth2.jwt.NimbusJwtDecoder) decoder)
                .setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));

        return decoder;
    }
}
