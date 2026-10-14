package org.example.soalabs.hr.service;

public class UpstreamUnavailableException extends RuntimeException {
    public UpstreamUnavailableException(String message) {
        super(message);
    }
}
