package com.co.eatupapi.dto.inventory.categories;

public class CategoryCreateRequestedMessage {

    private final String type;
    private final String subtype;
    private final String name;
    private final String locationId;

    public CategoryCreateRequestedMessage(CategoryDTO category) {
        this.type = category.getType();
        this.subtype = category.getSubtype();
        this.name = category.getName();
        this.locationId = category.getLocationId();
    }

    public String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }

    public String getName() {
        return name;
    }

    public String getLocationId() {
        return locationId;
    }
}
