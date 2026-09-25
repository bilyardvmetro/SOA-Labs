package org.example.soalabs.hr.service;

public class UpstreamBadGatewayException extends RuntimeException {
    public UpstreamBadGatewayException(String message) {
        super(message);
    }
}
