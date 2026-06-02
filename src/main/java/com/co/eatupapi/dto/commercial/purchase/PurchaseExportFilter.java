package com.co.eatupapi.dto.commercial.purchase;

import com.co.eatupapi.domain.commercial.purchase.PurchaseStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class PurchaseExportFilter {

    private PurchaseStatus status;

    private UUID providerId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}