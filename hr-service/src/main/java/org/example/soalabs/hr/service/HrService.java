package org.example.soalabs.hr.service;

import lombok.RequiredArgsConstructor;
import org.example.soalabs.hr.api.dto.OrganizationDto;
import org.example.soalabs.hr.api.dto.WorkerDto;
import org.example.soalabs.hr.client.WorkerServiceClient;
import org.example.soalabs.hr.client.dto.WorkerUpdateRequest;
import org.example.soalabs.hr.domain.WorkerStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class HrService {

    private static final BigDecimal MAX_INT = BigDecimal.valueOf(Integer.MAX_VALUE);

    private final WorkerServiceClient workerServiceClient;

    public WorkerDto fireWorker(int workerId, String organizationName) {
        WorkerDto worker = workerServiceClient.getWorker(workerId);
        OrganizationDto organization = worker.organization();

        if (organization == null || !organization.fullName().equals(organizationName)) {
            throw new EntityConflictException(
                    "worker does not belong to organization " + organizationName
            );
        }
        if (worker.status() == WorkerStatus.FIRED) {
            return worker;
        }

        WorkerUpdateRequest request = WorkerUpdateRequest.from(
                worker,
                worker.salary(),
                WorkerStatus.FIRED
        );
        return workerServiceClient.updateWorker(workerId, request);
    }

    public WorkerDto indexSalary(int workerId, BigDecimal coefficient) {
        WorkerDto worker = workerServiceClient.getWorker(workerId);
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
        return workerServiceClient.updateWorker(workerId, request);
    }
}
