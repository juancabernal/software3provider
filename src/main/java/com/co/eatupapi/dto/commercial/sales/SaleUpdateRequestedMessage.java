package com.co.eatupapi.dto.commercial.sales;

public class SaleUpdateRequestedMessage {

    private SaleUpdateSnapshotDTO oldSale;
    private SaleUpdateRequestSnapshotDTO newSale;

    public SaleUpdateRequestedMessage() {
    }

    public SaleUpdateRequestedMessage(SaleUpdateSnapshotDTO oldSale, SaleUpdateRequestSnapshotDTO newSale) {
        this.oldSale = oldSale;
        this.newSale = newSale;
    }

    public SaleUpdateSnapshotDTO getOldSale() {
        return oldSale;
    }

    public void setOldSale(SaleUpdateSnapshotDTO oldSale) {
        this.oldSale = oldSale;
    }

    public SaleUpdateRequestSnapshotDTO getNewSale() {
        return newSale;
    }

    public void setNewSale(SaleUpdateRequestSnapshotDTO newSale) {
        this.newSale = newSale;
    }
}
