package com.co.eatupapi.domain.inventory.categories;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class CategoryDomain {

    @Id
    private UUID id;

    @Column(unique = true)
    private Long cns;

    @Column(nullable = false)
    private String type;

    @Column
    private String subtype;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(nullable = false)
    private LocalDateTime entryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryStatus status;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime modifiedDate;

    public CategoryDomain() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getCns() {
        return cns;
    }

    public void setCns(Long cns) {
        this.cns = cns;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getLocationId() {
        return locationId;
    }

    public void setLocationId(UUID locationId) {
        this.locationId = locationId;
    }

    public LocalDateTime getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDateTime entryDate) {
        this.entryDate = entryDate;
    }

    public CategoryStatus getStatus() {
        return status;
    }

    public void setStatus(CategoryStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
}
