package com.co.eatupapi.services.inventory.product;

import com.co.eatupapi.domain.inventory.product.Product;
import com.co.eatupapi.dto.inventory.product.ProductDTO;
import com.co.eatupapi.dto.inventory.product.ProductPatchDTO;
import com.co.eatupapi.dto.inventory.product.ProductRequestDTO;
import com.co.eatupapi.messaging.inventory.product.ProductCreatedEventPublisher;
import com.co.eatupapi.messaging.inventory.product.ProductDeleteEventPublisher;
import com.co.eatupapi.messaging.inventory.product.ProductPatchEventPublisher;
import com.co.eatupapi.messaging.inventory.product.ProductUpdateEventPublisher;
import com.co.eatupapi.repositories.inventory.product.ProductRepository;
import com.co.eatupapi.utils.inventory.product.exceptions.BusinessException;
import com.co.eatupapi.utils.inventory.product.exceptions.ResourceNotFoundException;
import com.co.eatupapi.utils.inventory.product.exceptions.ValidationException;
import com.co.eatupapi.utils.inventory.product.mapper.ProductMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCTO_NO_ENCONTRADO = "Producto no encontrado con id: ";

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductCreatedEventPublisher productCreatedEventPublisher;
    private final ProductUpdateEventPublisher productUpdateEventPublisher;
    private final ProductPatchEventPublisher productPatchEventPublisher;
    private final ProductDeleteEventPublisher productDeleteEventPublisher;

    public ProductServiceImpl(ProductRepository productRepository,
                              ProductMapper productMapper, ProductCreatedEventPublisher productCreatedEventPublisher, ProductUpdateEventPublisher productUpdateEventPublisher, ProductPatchEventPublisher productPatchEventPublisher, ProductDeleteEventPublisher productDeleteEventPublisher) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.productCreatedEventPublisher = productCreatedEventPublisher;
        this.productUpdateEventPublisher = productUpdateEventPublisher;
        this.productPatchEventPublisher = productPatchEventPublisher;
        this.productDeleteEventPublisher = productDeleteEventPublisher;
    }

    @Override
    public Page<ProductDTO> findAll(int page, int size, String name) {
        Pageable pageable = PageRequest.of(page, size);

        if (name != null && !name.isBlank()) {
            return productRepository
                    .findByNameContainingIgnoreCase(name.trim(), pageable)
                    .map(productMapper::toDto);
        }

        return productRepository.findAll(pageable)
                .map(productMapper::toDto);
    }

    @Override
    public Page<ProductDTO> findByLocation(UUID locationId, int page, int size, String name) {
        if (locationId == null) {
            throw new ValidationException("El id de la sede es obligatorio");
        }

        Pageable pageable = PageRequest.of(page, size);

        if (name != null && !name.isBlank()) {
            return productRepository
                    .findByLocationIdAndNameContainingIgnoreCase(locationId, name.trim(), pageable)
                    .map(productMapper::toDto);
        }

        return productRepository
                .findByLocationId(locationId, pageable)
                .map(productMapper::toDto);
    }

    @Override
    public ProductDTO findById(UUID id) {
        validateId(id);
        return productRepository.findById(id)
                .map(productMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCTO_NO_ENCONTRADO + id));
    }

    @Override
    @Transactional
    public ProductDTO create(ProductRequestDTO request) {

        validateRequestNotNull(request);

        String normalizedName = normalizeName(request.getName());
        validateDuplicateNameAndLocation(null, normalizedName, request.getLocationId());

        Product product = productMapper.toDomain(request);

        productCreatedEventPublisher.publish(request);

        return productMapper.toDto(product);
    }

    @Override
    @Transactional
    public ProductDTO update(UUID id, ProductRequestDTO request) {

        validateId(id);
        validateRequestNotNull(request);

        Product product = findProductOrThrow(id);
        String normalizedName = normalizeName(request.getName());

        validateDuplicateNameAndLocation(id, normalizedName, request.getLocationId());
        applyFullUpdate(product, request, normalizedName);

        productUpdateEventPublisher.publish(id, request);

        return productMapper.toDto(product);
    }

    @Override
    @Transactional
    public ProductDTO patch(UUID id, ProductPatchDTO request) {
        validateId(id);
        validatePatchRequest(request);

        Product product = findProductOrThrow(id);

        String effectiveName = request.getName() != null
                ? normalizeName(request.getName())
                : product.getName();
        UUID effectiveLocationId = request.getLocationId() != null
                ? request.getLocationId()
                : product.getLocationId();

        if (requestChangesNameOrLocation(request)) {
            validateDuplicateNameAndLocation(id, effectiveName, effectiveLocationId);
        }

        applyPatch(product, request, effectiveName);

        productPatchEventPublisher.publish(id, request);

        return productMapper.toDto(product);
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        validateId(id);

        Product product = findProductOrThrow(id);

        if (product.getStock().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(
                    "No se puede eliminar el producto '" + product.getName()
                            + "' porque tiene " + product.getStock() + " unidades en stock"
            );
        }

        productDeleteEventPublisher.publish(id);
    }

    private Product findProductOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCTO_NO_ENCONTRADO + id));
    }

    private void applyFullUpdate(Product product, ProductRequestDTO request, String normalizedName) {
        product.setName(normalizedName);
        product.setCategoryId(request.getCategoryId());
        product.setLocationId(request.getLocationId());
        product.setUnitOfMeasure(request.getUnitOfMeasure());
        product.setSalePrice(request.getSalePrice());
        product.setStock(request.getStock());
        product.setStartDate(request.getStartDate());
    }

    private void applyPatch(Product product, ProductPatchDTO request, String normalizedName) {
        if (request.getName() != null) {
            product.setName(normalizedName);
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

    private boolean requestChangesNameOrLocation(ProductPatchDTO request) {
        return request.getName() != null || request.getLocationId() != null;
    }

    private void validateDuplicateNameAndLocation(UUID id, String name, UUID locationId) {
        long count = id == null
                ? productRepository.countByNameAndLocation(name, locationId)
                : productRepository.countByNameAndLocationAndIdNot(name, locationId, id);

        if (count > 0) {
            throw new BusinessException("Ya existe el producto '" + name + "' en esta sede");
        }
    }

    private void validateId(UUID id) {
        if (id == null) {
            throw new ValidationException("El id del producto es obligatorio");
        }
    }

    private void validateRequestNotNull(Object request) {
        if (request == null) {
            throw new ValidationException("La solicitud no puede estar vacia");
        }
    }

    private void validatePatchRequest(ProductPatchDTO request) {
        validateRequestNotNull(request);

        boolean hasAnyField =
                request.getName() != null ||
                request.getCategoryId() != null ||
                request.getLocationId() != null ||
                request.getUnitOfMeasure() != null ||
                request.getSalePrice() != null ||
                request.getStock() != null ||
                request.getStartDate() != null;

        if (!hasAnyField) {
            throw new ValidationException("La solicitud de actualizacion parcial no contiene campos para modificar");
        }
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }
}
