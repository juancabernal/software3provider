package com.co.eatupapi.dto.commercial.sales;

public class SaleDeleteRequestedMessage {

    private SaleDeleteSnapshotDTO sale;

    public SaleDeleteSnapshotDTO getSale() {
        return sale;
    }

    public void setSale(SaleDeleteSnapshotDTO sale) {
        this.sale = sale;
    }
}
