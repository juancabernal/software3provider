package com.co.eatupapi.repositories.commercial.purchase;

import com.co.eatupapi.domain.commercial.purchase.PurchaseDomain;
import com.co.eatupapi.domain.commercial.purchase.PurchaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository
        extends JpaRepository<PurchaseDomain, UUID>,
        JpaSpecificationExecutor<PurchaseDomain> {

    Page<PurchaseDomain> findByLocationIdAndDeletedFalse(UUID locationId, Pageable pageable);

    Page<PurchaseDomain> findByLocationIdAndStatusAndDeletedFalse(
            UUID locationId,
            PurchaseStatus status,
            Pageable pageable
    );

    Optional<PurchaseDomain> findByIdAndLocationIdAndDeletedFalse(UUID id, UUID locationId);}