package com.appcompras.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

  private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

  private static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of(
      "https://www.acortesdev.xyz",
      "https://acortesdev.xyz"
  );

  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins:}") List<String> configuredAllowedOrigins
  ) {
    CorsConfiguration config = new CorsConfiguration();
    List<String> parsedOrigins = configuredAllowedOrigins == null ? List.of() : configuredAllowedOrigins.stream()
        // Supports both list-style values and a single comma-separated env var value.
        .flatMap(value -> Arrays.stream(value.split(",")))
        .map(String::trim)
        .filter(origin -> !origin.isBlank())
        .distinct()
        .toList();
    List<String> allowedOrigins = parsedOrigins.isEmpty() ? DEFAULT_ALLOWED_ORIGINS : parsedOrigins;

    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of(
        "Authorization",
        "Content-Type",
        "X-API-Version",
        "X-Requested-With",
        "Accept",
        "Origin"
    ));
    config.setExposedHeaders(List.of("Location"));
    config.setAllowCredentials(true);
    config.setMaxAge(Duration.ofHours(1).getSeconds());
    log.info("CORS allowed origins: {}", allowedOrigins);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
