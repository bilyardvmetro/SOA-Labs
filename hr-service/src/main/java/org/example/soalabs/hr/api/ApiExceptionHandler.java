package org.example.soalabs.hr.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.ws.WebServiceException;
import org.example.soalabs.hr.api.dto.ErrorResponse;
import org.example.soalabs.hr.soap.HrFault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice(assignableTypes = HrController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(HrFault.class)
    public ResponseEntity<ErrorResponse> handleSoapFault(HrFault exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.getFaultInfo().getCode()) {
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case WORKER_NOT_FOUND -> HttpStatus.UNPROCESSABLE_ENTITY;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UPSTREAM_BAD_GATEWAY -> HttpStatus.BAD_GATEWAY;
            case UPSTREAM_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return response(status, exception.getMessage(), request);
    }

    @ExceptionHandler(WebServiceException.class)
    public ResponseEntity<ErrorResponse> handleSoapFailure(HttpServletRequest request) {
        return response(HttpStatus.BAD_GATEWAY, "HR SOAP call failed", request);
    }

    @ExceptionHandler({
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
