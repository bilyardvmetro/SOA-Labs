package org.example.soalabs.worker.service;

@jakarta.ejb.ApplicationException(rollback = true)
public class EntityConflictException extends RuntimeException {
    public EntityConflictException(String message) {
        super(message);
    }
}
