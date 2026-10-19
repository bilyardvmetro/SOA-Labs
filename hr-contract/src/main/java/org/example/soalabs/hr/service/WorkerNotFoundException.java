package org.example.soalabs.hr.service;

@jakarta.ejb.ApplicationException(rollback = true)
public class WorkerNotFoundException extends RuntimeException {
    public WorkerNotFoundException(String message) {
        super(message);
    }
}
