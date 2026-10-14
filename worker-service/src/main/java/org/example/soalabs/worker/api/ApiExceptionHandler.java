package org.example.soalabs.worker.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.example.soalabs.worker.api.dto.ErrorResponse;
import org.example.soalabs.worker.service.EntityConflictException;
import org.example.soalabs.worker.service.InvalidRequestException;
import org.example.soalabs.worker.service.WorkerNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(WorkerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(WorkerNotFoundException exception,
                                                        HttpServletRequest request) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }

    @ExceptionHandler({EntityConflictException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> handleConflict(Exception exception, HttpServletRequest request) {
        String message = exception instanceof DataIntegrityViolationException
                ? "Worker violates database constraints"
                : exception.getMessage();
        return response(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEntity(MethodArgumentNotValidException exception,
                                                             HttpServletRequest request) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null
                ? exception.getBindingResult().getAllErrors().get(0).getDefaultMessage()
                : fieldError.getField() + " " + fieldError.getDefaultMessage();
        return response(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler({
            InvalidRequestException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, badRequestMessage(exception), request);
    }

    private String badRequestMessage(Exception exception) {
        if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            return mismatch.getName() + " has an invalid value";
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return "Request body is missing or malformed";
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
