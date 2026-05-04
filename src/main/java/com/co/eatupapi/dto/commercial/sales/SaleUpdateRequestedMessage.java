package com.co.eatupapi.dto.commercial.sales;

public class SaleUpdateRequestedMessage {

    private SaleResponseDTO oldSale;
    private SaleRequestDTO newSale;

    public SaleUpdateRequestedMessage() {
    }

    public SaleUpdateRequestedMessage(SaleResponseDTO oldSale, SaleRequestDTO newSale) {
        this.oldSale = oldSale;
        this.newSale = newSale;
    }

    public SaleResponseDTO getOldSale() {
        return oldSale;
    }

    public void setOldSale(SaleResponseDTO oldSale) {
        this.oldSale = oldSale;
    }

    public SaleRequestDTO getNewSale() {
        return newSale;
    }

    public void setNewSale(SaleRequestDTO newSale) {
        this.newSale = newSale;
    }
}
