package org.example.soalabs.hr.service;

import jakarta.ejb.*;
import org.example.soalabs.hr.api.dto.OrganizationDto;
import org.example.soalabs.hr.api.dto.WorkerDto;
import org.example.soalabs.hr.client.WorkerServiceClient;
import org.example.soalabs.hr.client.dto.WorkerUpdateRequest;
import org.example.soalabs.hr.domain.WorkerStatus;


import java.math.BigDecimal;
import java.math.RoundingMode;

@Stateless(name = "HrService")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class HrService implements HrRemote {

    private static final BigDecimal MAX_INT = BigDecimal.valueOf(Integer.MAX_VALUE);

    @EJB
    private org.example.soalabs.hr.config.WorkerServiceClientConfiguration clients;

    public void health() { clients.health(); }

    public WorkerDto fireWorker(int workerId, String organizationName) {
        WorkerServiceClient client = clients.client();
        WorkerDto worker = client.getWorker(workerId);
        OrganizationDto organization = worker.organization();

        if (organization == null || !organization.fullName().equals(organizationName)) {
            throw new EntityConflictException(
                    "worker does not belong to organization " + organizationName
            );
        }
        if (worker.status() == WorkerStatus.FIRED) {
            return worker;
        }

        return client.fireWorker(workerId);
    }

    public WorkerDto indexSalary(int workerId, BigDecimal coefficient) {
        WorkerServiceClient client = clients.client();
        WorkerDto worker = client.getWorker(workerId);
        if (worker.status() == WorkerStatus.FIRED) {
            throw new EntityConflictException("fired worker salary cannot be indexed");
        }
        if (worker.salary() == null) {
            throw new EntityConflictException("worker salary must not be null for index operation");
        }

        BigDecimal indexedSalary = BigDecimal.valueOf(worker.salary().longValue())
                .multiply(coefficient)
                .setScale(0, RoundingMode.HALF_UP);
        if (indexedSalary.signum() <= 0 || indexedSalary.compareTo(MAX_INT) > 0) {
            throw new EntityConflictException("indexed salary must be within positive int32 range");
        }

        WorkerUpdateRequest request = WorkerUpdateRequest.from(
                worker,
                indexedSalary.intValueExact(),
                worker.status()
        );
        return client.updateWorker(workerId, request);
    }
}
