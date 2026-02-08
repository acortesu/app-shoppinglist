package com.appcompras.ingredient;

import com.appcompras.domain.MeasurementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCustomIngredientRequest(
        @Schema(example = "Carne de conejo")
        @NotBlank String name,
        @Schema(example = "WEIGHT")
        @NotNull MeasurementType measurementType
) {
}
