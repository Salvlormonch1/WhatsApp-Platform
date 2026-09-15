package com.saasplatform.common.exception;

import jakarta.ws.rs.core.Response;

/**
 * Base application exception with HTTP status code.
 */
public class AppException extends RuntimeException {

    private final Response.Status status;

    public AppException(Response.Status status, String message) {
        super(message);
        this.status = status;
    }

    public Response.Status getStatus() {
        return status;
    }
}
