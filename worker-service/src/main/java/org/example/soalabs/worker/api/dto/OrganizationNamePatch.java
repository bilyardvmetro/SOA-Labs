package org.example.soalabs.worker.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrganizationNamePatch(
        @NotNull @NotBlank @Size(max = 1804) String fullName
) {
}
