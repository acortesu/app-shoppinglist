package com.appcompras.planning;

import com.appcompras.recipe.MealType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;

@Embeddable
public class MealPlanSlotEmbeddable {

    @Column(name = "slot_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 32)
    private MealType mealType;

    @Column(name = "recipe_id", nullable = false, length = 36)
    private String recipeId;

    public MealPlanSlotEmbeddable() {
    }

    public MealPlanSlotEmbeddable(LocalDate date, MealType mealType, String recipeId) {
        this.date = date;
        this.mealType = mealType;
        this.recipeId = recipeId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public MealType getMealType() {
        return mealType;
    }

    public void setMealType(MealType mealType) {
        this.mealType = mealType;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }
}
