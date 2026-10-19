package org.example.soalabs.hr.service;

@jakarta.ejb.ApplicationException(rollback = true)
public class UpstreamUnavailableException extends RuntimeException {
    public UpstreamUnavailableException(String message) {
        super(message);
    }
}
