package org.example.soalabs.worker.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.soalabs.worker.domain.WorkerStatus;

import java.time.Instant;

public record WorkerInput(
        @NotNull @NotBlank String name,
        @NotNull @Valid CoordinatesDto coordinates,
        @Positive Integer salary,
        @NotNull Instant startDate,
        Instant endDate,
        @NotNull WorkerStatus status,
        @Valid OrganizationDto organization
) {
}
