package com.saasplatform.common.exception;

import jakarta.ws.rs.core.Response;

public class NotFoundException extends AppException {
    public NotFoundException(String resource) {
        super(Response.Status.NOT_FOUND, resource + " not found");
    }
}
