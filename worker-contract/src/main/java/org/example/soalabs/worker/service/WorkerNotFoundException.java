package org.example.soalabs.worker.service;

@jakarta.ejb.ApplicationException(rollback = true)
public class WorkerNotFoundException extends RuntimeException {
    public WorkerNotFoundException(Integer id) {
        super("Worker with id " + id + " does not exist");
    }
}
