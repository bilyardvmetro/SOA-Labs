package org.example.soalabs.hr.client.dto;

import org.example.soalabs.hr.api.dto.CoordinatesDto;
import org.example.soalabs.hr.api.dto.OrganizationDto;
import org.example.soalabs.hr.api.dto.WorkerDto;
import org.example.soalabs.hr.domain.WorkerStatus;

import java.time.Instant;

public record WorkerUpdateRequest(
        String name,
        CoordinatesDto coordinates,
        Integer salary,
        Instant startDate,
        Instant endDate,
        WorkerStatus status,
        OrganizationDto organization
) {
    public static WorkerUpdateRequest from(WorkerDto worker, Integer salary, WorkerStatus status) {
        return new WorkerUpdateRequest(
                worker.name(),
                worker.coordinates(),
                salary,
                worker.startDate(),
                worker.endDate(),
                status,
                worker.organization()
        );
    }
}
