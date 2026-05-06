package com.co.eatupapi.utils.payment.invoice.mapper;

import com.co.eatupapi.domain.payment.invoice.InvoiceDetail;
import com.co.eatupapi.domain.payment.invoice.Invoice;
import com.co.eatupapi.dto.payment.invoice.detail.InvoiceDetailResponse;
import com.co.eatupapi.dto.payment.invoice.InvoiceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "invoiceId", source = "id")
    InvoiceResponse toResponse(Invoice invoice);

    @Mapping(target = "detailId", source = "id")
    InvoiceDetailResponse toDetailResponse(InvoiceDetail detail);
}
