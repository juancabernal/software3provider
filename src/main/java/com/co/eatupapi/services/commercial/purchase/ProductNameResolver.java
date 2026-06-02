package com.co.eatupapi.services.commercial.purchase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrato para resolver nombres de producto desde cualquier módulo.
 * Evita acoplamiento directo entre commercial e inventory.
 */
public interface ProductNameResolver {

    Map<UUID, String> resolveNames(List<UUID> productIds);
}