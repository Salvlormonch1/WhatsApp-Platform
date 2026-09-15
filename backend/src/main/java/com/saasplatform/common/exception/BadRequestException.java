package com.saasplatform.common.exception;

import jakarta.ws.rs.core.Response;

public class BadRequestException extends AppException {
    public BadRequestException(String message) {
        super(Response.Status.BAD_REQUEST, message);
    }
}
