package com.saasplatform.common.exception;

import com.saasplatform.common.dto.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler — maps all exceptions to ApiResponse format.
 * Prevents leaking internal error details in production.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable throwable) {

        // Known application exceptions
        if (throwable instanceof AppException appEx) {
            LOG.debugf("Application exception: %s - %s", appEx.getStatus(), appEx.getMessage());
            return Response.status(appEx.getStatus())
                    .entity(ApiResponse.error(appEx.getMessage()))
                    .build();
        }

        // Bean validation errors
        if (throwable instanceof ConstraintViolationException cve) {
            Map<String, String> fieldErrors = cve.getConstraintViolations().stream()
                    .collect(Collectors.toMap(
                            v -> extractField(v.getPropertyPath().toString()),
                            ConstraintViolation::getMessage,
                            (a, b) -> a
                    ));
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Validation failed", fieldErrors))
                    .build();
        }

        // JAX-RS not found
        if (throwable instanceof jakarta.ws.rs.NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Resource not found"))
                    .build();
        }

        // Unexpected errors — log but don't expose details
        LOG.errorf(throwable, "Unhandled exception");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.error("An internal error occurred"))
                .build();
    }

    private String extractField(String propertyPath) {
        String[] parts = propertyPath.split("\\.");
        return parts[parts.length - 1];
    }
}
