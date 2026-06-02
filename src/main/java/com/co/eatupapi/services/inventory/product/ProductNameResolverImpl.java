package com.co.eatupapi.services.inventory.product;

import com.co.eatupapi.repositories.inventory.product.ProductRepository;
import com.co.eatupapi.services.commercial.purchase.ProductNameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductNameResolverImpl implements ProductNameResolver {

    private final ProductRepository productRepository;

    public ProductNameResolverImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> resolveNames(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) return Map.of();
        return productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getId(),
                        p -> p.getName()
                ));
    }
}
