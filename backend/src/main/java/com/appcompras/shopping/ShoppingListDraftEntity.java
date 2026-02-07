package com.appcompras.shopping;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shopping_list_drafts")
public class ShoppingListDraftEntity {

    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Column(name = "plan_id", nullable = false, length = 36)
    private String planId;

    @ElementCollection
    @CollectionTable(name = "shopping_list_draft_items", joinColumns = @JoinColumn(name = "draft_id"))
    @OrderColumn(name = "position")
    private List<ShoppingListDraftItemEmbeddable> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public List<ShoppingListDraftItemEmbeddable> getItems() {
        return items;
    }

    public void setItems(List<ShoppingListDraftItemEmbeddable> items) {
        this.items = items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
