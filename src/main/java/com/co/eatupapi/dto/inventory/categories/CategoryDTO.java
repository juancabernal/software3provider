package com.co.eatupapi.dto.inventory.categories;

import com.co.eatupapi.domain.inventory.categories.CategoryStatus;
import java.time.LocalDateTime;

public class CategoryDTO {

    private String id;
    private Long cns;
    private String type;
    private String subtype;
    private String name;
    private String locationId;
    private LocalDateTime entryDate;
    private CategoryStatus status;

    public CategoryDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
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
}
