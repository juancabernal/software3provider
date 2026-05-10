package com.co.eatupapi.dto.commercial.discount;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class DiscountDTO {

    private UUID id;

    @NotNull(message = "categoryId es obligatorio")
    private UUID categoryId;

    @NotNull(message = "percentage es obligatorio")
    @Min(value = 1, message = "percentage debe estar entre 1 y 100")
    @Max(value = 100, message = "percentage debe estar entre 1 y 100")
    private Integer percentage;

    @NotBlank(message = "description es obligatoria")
    @Size(min = 5, max = 100, message = "description debe tener entre 5 y 100 caracteres")
    private String description;

    @NotNull(message = "status es obligatorio")
    private Boolean status;

    public DiscountDTO() {}

    public DiscountDTO(UUID id, UUID categoryId, Integer percentage, String description, Boolean status) {
        this.id = id;
        this.categoryId = categoryId;
        this.percentage = percentage;
        this.description = description;
        this.status = status;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public Integer getPercentage() { return percentage; }
    public void setPercentage(Integer percentage) { this.percentage = percentage; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }
}