package org.example.soalabs.worker.service;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.sql.SQLException;

public class PersistenceErrors {
    @PersistenceContext(unitName = "workers")
    private EntityManager em;

    @AroundInvoke
    public Object translate(InvocationContext context) throws Exception {
        try {
            Object result = context.proceed();
            // Surface DB constraints before returning across the remote EJB boundary.
            em.flush();
            return result;
        } catch (PersistenceException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && sql.getSQLState() != null &&
                        sql.getSQLState().startsWith("23")) {
                    throw new EntityConflictException("Worker violates database constraints");
                }
            }
            throw exception;
        }
    }
}
