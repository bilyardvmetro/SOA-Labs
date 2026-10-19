package org.example.soalabs.hr.service;

@jakarta.ejb.ApplicationException(rollback = true)
public class UpstreamBadGatewayException extends RuntimeException {
    public UpstreamBadGatewayException(String message) {
        super(message);
    }
}
