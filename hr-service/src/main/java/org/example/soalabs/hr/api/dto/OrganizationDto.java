package org.example.soalabs.hr.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.example.soalabs.hr.domain.OrganizationType;

public record OrganizationDto(
        @NotBlank @Size(max = 1804) String fullName,
        @NotNull @Positive Integer annualTurnover,
        @NotNull OrganizationType type
) {
}
