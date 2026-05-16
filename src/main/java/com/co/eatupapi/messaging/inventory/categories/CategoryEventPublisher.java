package com.co.eatupapi.messaging.inventory.categories;

import com.co.eatupapi.dto.inventory.categories.CategoryDTO;
import com.co.eatupapi.dto.inventory.categories.CategoryUpdateStatusRequestedMessage;

public interface CategoryEventPublisher {

    void publishCreateRequested(CategoryDTO request);

    void publishUpdateStatusRequested(CategoryUpdateStatusRequestedMessage message);
}
