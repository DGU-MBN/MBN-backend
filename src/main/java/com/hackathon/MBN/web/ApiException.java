package com.hackathon.MBN.web;

import org.springframework.http.HttpStatus;

/** 명세서의 code(예: INVALID_BBOX, SOURCE_NOT_FOUND 등)를 그대로 던지기 위한 예외 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
