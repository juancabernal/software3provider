package com.co.eatupapi.utils.inventory.product.mapper;

import com.co.eatupapi.domain.inventory.product.Product;
import com.co.eatupapi.dto.inventory.product.ProductDTO;
import com.co.eatupapi.dto.inventory.product.ProductPatchDTO;
import com.co.eatupapi.dto.inventory.product.ProductRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductDTO toDto(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setCategoryId(product.getCategoryId());
        dto.setLocationId(product.getLocationId());
        dto.setUnitOfMeasure(product.getUnitOfMeasure());
        dto.setSalePrice(product.getSalePrice());
        dto.setStock(product.getStock());
        dto.setStartDate(product.getStartDate());
        return dto;
    }

    public Product toDomain(ProductRequestDTO request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setCategoryId(request.getCategoryId());
        product.setLocationId(request.getLocationId());
        product.setUnitOfMeasure(request.getUnitOfMeasure());
        product.setSalePrice(request.getSalePrice());
        product.setStock(request.getStock());
        product.setStartDate(request.getStartDate());
        return product;
    }

    public void mergePatch(Product product, ProductPatchDTO request) {

        if (request.getName() != null) {
            product.setName(request.getName().trim().replaceAll("\\s+", " "));
        }

        if (request.getCategoryId() != null) {
            product.setCategoryId(request.getCategoryId());
        }

        if (request.getLocationId() != null) {
            product.setLocationId(request.getLocationId());
        }

        if (request.getUnitOfMeasure() != null) {
            product.setUnitOfMeasure(request.getUnitOfMeasure());
        }

        if (request.getSalePrice() != null) {
            product.setSalePrice(request.getSalePrice());
        }

        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }

        if (request.getStartDate() != null) {
            product.setStartDate(request.getStartDate());
        }
    }
}