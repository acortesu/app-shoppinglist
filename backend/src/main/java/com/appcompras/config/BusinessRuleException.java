package com.appcompras.config;

public class BusinessRuleException extends RuntimeException {

    private final ApiErrorCode code;

    public BusinessRuleException(ApiErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ApiErrorCode getCode() {
        return code;
    }

    public String getCodeAsString() {
        return code.name();
    }
}
