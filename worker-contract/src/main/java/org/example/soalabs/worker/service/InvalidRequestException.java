package org.example.soalabs.worker.service;

@jakarta.ejb.ApplicationException(rollback = true)
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
