package com.co.eatupapi.dto.commercial.sales;

import java.util.List;
import java.util.UUID;

public class SaleUpdateRequestSnapshotDTO {

    private List<SaleUpdateDetailMessageDTO> details;
    private UUID locationId;
    private String sellerId;
    private String tableId;

    public List<SaleUpdateDetailMessageDTO> getDetails() {
        return details;
    }

    public void setDetails(List<SaleUpdateDetailMessageDTO> details) {
        this.details = details;
    }

    public UUID getLocationId() {
        return locationId;
    }

    public void setLocationId(UUID locationId) {
        this.locationId = locationId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }
}
