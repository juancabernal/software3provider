package com.co.eatupapi.domain.commercial.customerDiscount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_discounts")
public class CustomerDiscountDomain {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID locationId;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private UUID discountId;

    @Column(nullable = false)
    private LocalDate assignedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime modifiedAt;

    @Column
    private LocalDate startDate;   // inicio de vigencia

    @Column
    private LocalDate endDate;     // fin de vigencia (null = sin vencimiento)

    public CustomerDiscountDomain() {
    }

    public CustomerDiscountDomain(UUID id, UUID locationId, UUID customerId, UUID discountId, LocalDate assignedAt) {
        this.id = id;
        this.locationId = locationId;
        this.customerId = customerId;
        this.discountId = discountId;
        this.assignedAt = assignedAt;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.modifiedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLocationId() {
        return locationId;
    }

    public void setLocationId(UUID locationId) {
        this.locationId = locationId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getDiscountId() {
        return discountId;
    }

    public void setDiscountId(UUID discountId) {
        this.discountId = discountId;
    }

    public LocalDate getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDate assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}

