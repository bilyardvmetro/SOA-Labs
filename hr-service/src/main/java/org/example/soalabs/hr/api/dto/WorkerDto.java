package org.example.soalabs.hr.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.soalabs.hr.domain.WorkerStatus;

import java.time.Instant;

public record WorkerDto(
        @NotNull @Positive Integer id,
        @NotBlank String name,
        @NotNull @Valid CoordinatesDto coordinates,
        @NotNull Instant creationDate,
        @Positive Integer salary,
        @NotNull Instant startDate,
        Instant endDate,
        @NotNull WorkerStatus status,
        @Valid OrganizationDto organization
) {
}
