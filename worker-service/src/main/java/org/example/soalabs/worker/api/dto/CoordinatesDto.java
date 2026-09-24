package org.example.soalabs.worker.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public record CoordinatesDto(
        @NotNull @Max(202) Integer x,
        @NotNull @DecimalMin(value = "-992", inclusive = false) Float y
) {
}
