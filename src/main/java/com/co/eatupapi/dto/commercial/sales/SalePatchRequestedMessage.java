package com.co.eatupapi.dto.commercial.sales;

import java.util.UUID;

public class SalePatchRequestedMessage {

    private UUID saleId;
    private SalePatchDTO request;

    public SalePatchRequestedMessage() {
    }

    public SalePatchRequestedMessage(UUID saleId, SalePatchDTO request) {
        this.saleId = saleId;
        this.request = request;
    }

    public UUID getSaleId() {
        return saleId;
    }

    public void setSaleId(UUID saleId) {
        this.saleId = saleId;
    }

    public SalePatchDTO getRequest() {
        return request;
    }

    public void setRequest(SalePatchDTO request) {
        this.request = request;
    }
}
