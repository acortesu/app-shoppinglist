package com.appcompras.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @Value("${app.environment:local}") String environment,
            @Value("${app.version:0.1.0}") String version) {
        return registry -> registry.config()
                .commonTags(
                        "application", "appcompras-backend",
                        "environment", environment,
                        "version", version,
                        "service", "backend");
    }
}
