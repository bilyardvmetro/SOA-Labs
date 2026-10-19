package org.example.soalabs.hr.service;

@jakarta.ejb.ApplicationException(rollback = true)
public class EntityConflictException extends RuntimeException {
    public EntityConflictException(String message) {
        super(message);
    }
}
