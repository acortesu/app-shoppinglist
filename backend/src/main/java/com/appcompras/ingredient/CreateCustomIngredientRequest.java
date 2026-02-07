package com.appcompras.ingredient;

import com.appcompras.domain.MeasurementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCustomIngredientRequest(
        @NotBlank String name,
        @NotNull MeasurementType measurementType
) {
}
