package org.example.soalabs.worker.api.dto;

import org.example.soalabs.worker.domain.WorkerStatus;

import java.time.Instant;

public record WorkerResponse(
        Integer id,
        String name,
        CoordinatesDto coordinates,
        Instant creationDate,
        Integer salary,
        Instant startDate,
        Instant endDate,
        WorkerStatus status,
        OrganizationDto organization
) {
}
