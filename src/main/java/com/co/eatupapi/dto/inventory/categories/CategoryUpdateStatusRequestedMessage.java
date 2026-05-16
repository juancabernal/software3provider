package com.co.eatupapi.dto.inventory.categories;

import java.util.UUID;

public class CategoryUpdateStatusRequestedMessage {

    private final UUID id;
    private final CategoryStatusUpdateDTO data;

    public CategoryUpdateStatusRequestedMessage(UUID id, CategoryStatusUpdateDTO data) {
        this.id = id;
        this.data = data;
    }

    public UUID getId() {
        return id;
    }

    public CategoryStatusUpdateDTO getData() {
        return data;
    }
}
