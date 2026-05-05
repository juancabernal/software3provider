package com.co.eatupapi.services.payment.cashreceipt.impl;

import com.co.eatupapi.domain.payment.cashreceipt.CashReceipt;
import com.co.eatupapi.domain.payment.invoice.Invoice;
import com.co.eatupapi.domain.payment.paymentmethod.PaymentMethod;
import com.co.eatupapi.dto.payment.cashreceipt.CashReceiptResponse;
import com.co.eatupapi.dto.payment.cashreceipt.CreateCashReceiptRequest;
import com.co.eatupapi.messaging.payment.cashreceipt.CashReceiptCancelMessage;
import com.co.eatupapi.messaging.payment.cashreceipt.CashReceiptCreateMessage;
import com.co.eatupapi.messaging.payment.cashreceipt.CashReceiptMessagePublisher;
import com.co.eatupapi.repositories.payment.cashreceipt.CashReceiptRepository;
import com.co.eatupapi.repositories.payment.invoice.InvoiceRepository;
import com.co.eatupapi.repositories.payment.paymentmethod.PaymentMethodRepository;
import com.co.eatupapi.services.payment.cashreceipt.CashReceiptService;
import com.co.eatupapi.utils.payment.cashreceipt.exceptions.CashReceiptBusinessException;
import com.co.eatupapi.utils.payment.cashreceipt.exceptions.CashReceiptNotFoundException;
import com.co.eatupapi.utils.payment.cashreceipt.exceptions.ErrorCode;
import com.co.eatupapi.utils.payment.cashreceipt.mapper.CashReceiptMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CashReceiptServiceImpl implements CashReceiptService {

    private final CashReceiptRepository cashReceiptRepository;
    private final CashReceiptMapper cashReceiptMapper;
    private final CashReceiptMessagePublisher cashReceiptMessagePublisher;
    private final InvoiceRepository invoiceRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public CashReceiptServiceImpl(CashReceiptRepository cashReceiptRepository,
                                  CashReceiptMapper cashReceiptMapper,
                                  CashReceiptMessagePublisher cashReceiptMessagePublisher,
                                  InvoiceRepository invoiceRepository,
                                  PaymentMethodRepository paymentMethodRepository) {
        this.cashReceiptRepository = cashReceiptRepository;
        this.cashReceiptMapper = cashReceiptMapper;
        this.cashReceiptMessagePublisher = cashReceiptMessagePublisher;
        this.invoiceRepository = invoiceRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Override
    public void createCashReceipt(UUID locationId, CreateCashReceiptRequest request) {
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new CashReceiptBusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Invoice not found: " + request.getInvoiceId()
                ));

        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                .orElseThrow(() -> new CashReceiptBusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Payment method not found: " + request.getPaymentMethodId()
                ));

        CashReceiptCreateMessage event = new CashReceiptCreateMessage(
                locationId,
                request.getInvoiceId(),
                invoice.getLocationId(),
                invoice.getStatus() != null ? invoice.getStatus().name() : null,
                invoice.getTotalPrice(),
                request.getAmount(),
                request.getPaymentMethodId(),
                paymentMethod.getActive()
        );
        cashReceiptMessagePublisher.publishCreate(event);
    }

    @Override
    public Page<CashReceiptResponse> getCashReceiptsBySite(UUID locationId, Pageable pageable) {

        return cashReceiptRepository
                .findByLocationId(locationId, pageable)
                .map(cashReceiptMapper::toResponse);
    }

    @Override
    public void cancelCashReceipt(UUID locationId, UUID receiptId) {
        CashReceipt receipt = cashReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new CashReceiptNotFoundException("Cash receipt not found"));

        if (!locationId.equals(receipt.getLocationId())) {
            throw new CashReceiptBusinessException(
                    ErrorCode.RECEIPT_DOES_NOT_BELONG_TO_SITE,
                    "Cash receipt does not belong to location: " + locationId
            );
        }

        Invoice invoice = invoiceRepository.findById(receipt.getInvoiceId())
                .orElseThrow(() -> new CashReceiptBusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Invoice not found for receipt: " + receipt.getInvoiceId()
                ));

        CashReceiptCancelMessage event = new CashReceiptCancelMessage(locationId, receiptId, invoice.getTotalPrice());
        cashReceiptMessagePublisher.publishCancel(event);
    }
}