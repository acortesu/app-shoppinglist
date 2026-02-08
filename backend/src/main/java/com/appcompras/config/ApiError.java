package com.appcompras.config;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String error,
        String path,
        String requestId
) {
}
