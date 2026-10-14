package org.example.soalabs.hr.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.example.soalabs.hr.api.dto.ErrorResponse;
import org.example.soalabs.hr.service.EntityConflictException;
import org.example.soalabs.hr.service.UpstreamBadGatewayException;
import org.example.soalabs.hr.service.UpstreamUnavailableException;
import org.example.soalabs.hr.service.WorkerNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(WorkerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkerNotFound(WorkerNotFoundException exception,
                                                              HttpServletRequest request) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }

    @ExceptionHandler(EntityConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(EntityConflictException exception,
                                                        HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(UpstreamBadGatewayException.class)
    public ResponseEntity<ErrorResponse> handleBadGateway(UpstreamBadGatewayException exception,
                                                          HttpServletRequest request) {
        return response(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
    }

    @ExceptionHandler(UpstreamUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(UpstreamUnavailableException exception,
                                                           HttpServletRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, badRequestMessage(exception), request);
    }

    private String badRequestMessage(Exception exception) {
        if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            return mismatch.getName() + " has an invalid value";
        }
        return exception.getMessage();
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message,
                                                   HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
