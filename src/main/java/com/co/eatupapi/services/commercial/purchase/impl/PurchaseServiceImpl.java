package com.co.eatupapi.services.commercial.purchase.impl;

import com.co.eatupapi.domain.commercial.purchase.PurchaseDomain;
import com.co.eatupapi.domain.commercial.purchase.PurchaseStatus;
import com.co.eatupapi.dto.commercial.purchase.CreatePurchaseRequest;
import com.co.eatupapi.dto.commercial.purchase.PurchaseResponse;
import com.co.eatupapi.messaging.commercial.purchase.PurchaseAction;
import com.co.eatupapi.messaging.commercial.purchase.PurchaseItemMessage;
import com.co.eatupapi.messaging.commercial.purchase.PurchaseMessage;
import com.co.eatupapi.messaging.commercial.purchase.PurchaseMessagePublisher;
import com.co.eatupapi.repositories.commercial.purchase.PurchaseRepository;
import com.co.eatupapi.services.commercial.purchase.PurchaseService;
import com.co.eatupapi.utils.commercial.purchase.exceptions.PurchaseBusinessException;
import com.co.eatupapi.utils.commercial.purchase.exceptions.PurchaseConflictException;
import com.co.eatupapi.utils.commercial.purchase.exceptions.PurchaseErrorCode;
import com.co.eatupapi.utils.commercial.purchase.exceptions.PurchaseNotFoundException;
import com.co.eatupapi.utils.commercial.purchase.mapper.PurchaseMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseMapper purchaseMapper;
    private final PurchaseMessagePublisher purchaseMessagePublisher;

    public PurchaseServiceImpl(PurchaseRepository purchaseRepository,
                               PurchaseMapper purchaseMapper,
                               PurchaseMessagePublisher purchaseMessagePublisher) {
        this.purchaseRepository = purchaseRepository;
        this.purchaseMapper = purchaseMapper;
        this.purchaseMessagePublisher = purchaseMessagePublisher;
    }

    @Override
    public PurchaseResponse createPurchase(UUID locationId, CreatePurchaseRequest request) {

        String orderNumber = generateOrderNumber();
        UUID purchaseId = UUID.randomUUID();

        List<PurchaseItemMessage> itemMessages = buildItemMessages(request);
        BigDecimal total = calculateTotal(itemMessages);

        PurchaseMessage message = new PurchaseMessage(
                purchaseId,
                orderNumber,
                request.getProviderId(),
                locationId,
                itemMessages,
                total,
                PurchaseStatus.CREATED,
                PurchaseAction.CREATED
        );

        purchaseMessagePublisher.publishPurchaseReceived(message);

        return buildResponse(purchaseId, orderNumber, request.getProviderId(),
                locationId, itemMessages, total, PurchaseStatus.CREATED);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseResponse getPurchaseById(UUID locationId, UUID purchaseId) {
        return purchaseMapper.toResponse(findByIdOrThrow(purchaseId, locationId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseResponse> getPurchases(UUID locationId, PurchaseStatus status, Pageable pageable) {
        if (status == null) {
            return purchaseRepository
                    .findByLocationIdAndDeletedFalse(locationId, pageable)
                    .map(purchaseMapper::toResponse);
        }
        return purchaseRepository
                .findByLocationIdAndStatusAndDeletedFalse(locationId, status, pageable)
                .map(purchaseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseResponse updatePurchase(UUID locationId, UUID purchaseId, CreatePurchaseRequest request) {

        PurchaseDomain existing = findByIdOrThrow(purchaseId, locationId);

        if (existing.getStatus() != PurchaseStatus.CREATED) {
            throw new PurchaseBusinessException(
                    PurchaseErrorCode.INVALID_STATUS_TRANSITION,
                    "Only purchases in CREATED status can be modified"
            );
        }

        List<PurchaseItemMessage> itemMessages = buildItemMessages(request);
        BigDecimal total = calculateTotal(itemMessages);

        PurchaseMessage message = new PurchaseMessage(
                purchaseId,
                existing.getOrderNumber(),
                request.getProviderId(),
                locationId,
                itemMessages,
                total,
                existing.getStatus(),
                PurchaseAction.UPDATED
        );

        purchaseMessagePublisher.publishPurchaseReceived(message);

        return buildResponse(purchaseId, existing.getOrderNumber(), request.getProviderId(),
                locationId, itemMessages, total, existing.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseResponse updateStatus(UUID locationId, UUID purchaseId, PurchaseStatus newStatus) {

        PurchaseDomain existing = findByIdOrThrow(purchaseId, locationId);

        if (!existing.getStatus().canTransitionTo(newStatus)) {
            throw new PurchaseConflictException(
                    PurchaseErrorCode.INVALID_STATUS_TRANSITION,
                    "Cannot transition from " + existing.getStatus() + " to " + newStatus
            );
        }

        List<PurchaseItemMessage> itemMessages = existing.getItems().stream()
                .map(item -> {
                    PurchaseItemMessage itemMessage = new PurchaseItemMessage();
                    itemMessage.setProductId(item.getProductId());
                    itemMessage.setQuantity(item.getQuantity());
                    itemMessage.setUnitPrice(item.getUnitPrice());
                    itemMessage.setSubtotal(item.getSubtotal());
                    return itemMessage;
                })
                .toList();

        PurchaseMessage message = new PurchaseMessage(
                purchaseId,
                existing.getOrderNumber(),
                existing.getProviderId(),
                locationId,
                itemMessages,
                existing.getTotal(),
                newStatus,
                PurchaseAction.STATUS_UPDATED
        );

        purchaseMessagePublisher.publishPurchaseReceived(message);

        return buildResponse(purchaseId, existing.getOrderNumber(), existing.getProviderId(),
                locationId, itemMessages, existing.getTotal(), newStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public void deletePurchase(UUID locationId, UUID purchaseId) {

        PurchaseDomain existing = findByIdOrThrow(purchaseId, locationId);

        if (existing.isDeleted()) {
            throw new PurchaseBusinessException(
                    PurchaseErrorCode.PURCHASE_ALREADY_DELETED,
                    "Purchase with id: " + purchaseId + " is already deleted"
            );
        }

        if (existing.getStatus() == PurchaseStatus.APPROVED
                || existing.getStatus() == PurchaseStatus.RECEIVED) {
            throw new PurchaseBusinessException(
                    PurchaseErrorCode.INVALID_STATUS_TRANSITION,
                    "Cannot delete a purchase in " + existing.getStatus() + " status"
            );
        }

        PurchaseMessage message = new PurchaseMessage(
                purchaseId,
                existing.getOrderNumber(),
                existing.getProviderId(),
                locationId,
                List.of(),
                existing.getTotal(),
                existing.getStatus(),
                PurchaseAction.DELETED
        );

        purchaseMessagePublisher.publishPurchaseReceived(message);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────

    private List<PurchaseItemMessage> buildItemMessages(CreatePurchaseRequest request) {
        return request.getItems().stream()
                .map(item -> {
                    PurchaseItemMessage itemMessage = new PurchaseItemMessage();
                    itemMessage.setProductId(item.getProductId());
                    itemMessage.setQuantity(item.getQuantity());
                    itemMessage.setUnitPrice(item.getUnitPrice());
                    itemMessage.setSubtotal(item.getQuantity().multiply(item.getUnitPrice()));
                    return itemMessage;
                })
                .toList();
    }

    private BigDecimal calculateTotal(List<PurchaseItemMessage> items) {
        return items.stream()
                .map(PurchaseItemMessage::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PurchaseResponse buildResponse(UUID purchaseId, String orderNumber, UUID providerId,
                                           UUID locationId, List<PurchaseItemMessage> itemMessages,
                                           BigDecimal total, PurchaseStatus status) {
        PurchaseResponse response = new PurchaseResponse();
        response.setId(purchaseId);
        response.setOrderNumber(orderNumber);
        response.setProviderId(providerId);
        response.setLocationId(locationId);
        response.setTotal(total);
        response.setStatus(status);
        response.setCreatedDate(LocalDateTime.now());
        response.setModifiedDate(LocalDateTime.now());
        return response;
    }

    private PurchaseDomain findByIdOrThrow(UUID id, UUID locationId) {
        return purchaseRepository.findByIdAndLocationIdAndDeletedFalse(id, locationId)
                .orElseThrow(() -> new PurchaseNotFoundException(
                        "Purchase not found with id: " + id + " for location: " + locationId));
    }

    private String generateOrderNumber() {
        return "PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}