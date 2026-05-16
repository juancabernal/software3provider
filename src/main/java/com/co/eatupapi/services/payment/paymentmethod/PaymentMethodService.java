package com.co.eatupapi.services.payment.paymentmethod;

import com.co.eatupapi.dto.payment.paymentmethod.CreatePaymentMethodRequest;
import com.co.eatupapi.dto.payment.paymentmethod.PaymentMethodResponse;

import java.util.List;

public interface PaymentMethodService {

    List<PaymentMethodResponse> getActivePaymentMethods();

    List<PaymentMethodResponse> getAllPaymentMethods();

    void createPaymentMethod(CreatePaymentMethodRequest request);

    void togglePaymentMethodStatus(java.util.UUID id);
}
