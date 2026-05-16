package com.co.eatupapi.messaging.inventory.recipe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RecipeQueueConfig {

    @Value("${rabbitmq.exchange.recipe}")
    private String recipeExchangeName;

    @Value("${rabbitmq.routing-key.recipe.create}")
    private String createRoutingKey;

    @Value("${rabbitmq.routing-key.recipe.update}")
    private String updateRoutingKey;

    @Value("${rabbitmq.routing-key.recipe.patch}")
    private String patchRoutingKey;

    public String getRecipeExchangeName() {
        return recipeExchangeName;
    }

    public String getCreateRoutingKey() {
        return createRoutingKey;
    }

    public String getUpdateRoutingKey() {
        return updateRoutingKey;
    }

    public String getPatchRoutingKey() {
        return patchRoutingKey;
    }
}
