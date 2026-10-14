package org.example.soalabs.worker.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.example.soalabs.worker.domain.OrganizationType;

public record OrganizationDto(
        @NotNull @NotBlank @Size(max = 1804) String fullName,
        @NotNull @Positive Integer annualTurnover,
        @NotNull OrganizationType type
) {
}
