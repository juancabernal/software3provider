package com.co.eatupapi.dto.commercial.sales;

public class SaleDeleteRequestedMessage {

    private SaleResponseDTO sale;

    public SaleDeleteRequestedMessage() {
    }

    public SaleDeleteRequestedMessage(SaleResponseDTO sale) {
        this.sale = sale;
    }

    public SaleResponseDTO getSale() {
        return sale;
    }

    public void setSale(SaleResponseDTO sale) {
        this.sale = sale;
    }
}
