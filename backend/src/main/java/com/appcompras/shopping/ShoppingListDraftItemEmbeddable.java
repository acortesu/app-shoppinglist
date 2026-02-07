package com.appcompras.shopping;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ShoppingListDraftItemEmbeddable {

    @Column(name = "item_id", nullable = false, length = 64)
    private String id;

    @Column(name = "ingredient_id")
    private String ingredientId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double quantity;

    @Column(nullable = false)
    private String unit;

    @Column(name = "suggested_packages")
    private Integer suggestedPackages;

    @Column(name = "package_amount")
    private Double packageAmount;

    @Column(name = "package_unit")
    private String packageUnit;

    @Column(nullable = false)
    private boolean manual;

    public ShoppingListDraftItemEmbeddable() {
    }

    public ShoppingListDraftItemEmbeddable(
            String id,
            String ingredientId,
            String name,
            double quantity,
            String unit,
            Integer suggestedPackages,
            Double packageAmount,
            String packageUnit,
            boolean manual
    ) {
        this.id = id;
        this.ingredientId = ingredientId;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.suggestedPackages = suggestedPackages;
        this.packageAmount = packageAmount;
        this.packageUnit = packageUnit;
        this.manual = manual;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(String ingredientId) {
        this.ingredientId = ingredientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getSuggestedPackages() {
        return suggestedPackages;
    }

    public void setSuggestedPackages(Integer suggestedPackages) {
        this.suggestedPackages = suggestedPackages;
    }

    public Double getPackageAmount() {
        return packageAmount;
    }

    public void setPackageAmount(Double packageAmount) {
        this.packageAmount = packageAmount;
    }

    public String getPackageUnit() {
        return packageUnit;
    }

    public void setPackageUnit(String packageUnit) {
        this.packageUnit = packageUnit;
    }

    public boolean isManual() {
        return manual;
    }

    public void setManual(boolean manual) {
        this.manual = manual;
    }
}
