package com.co.eatupapi.controllers.payment.paymentmethod;

import com.co.eatupapi.dto.payment.paymentmethod.PaymentMethodResponse;
import com.co.eatupapi.dto.payment.paymentmethod.CreatePaymentMethodRequest;
import com.co.eatupapi.services.payment.paymentmethod.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-methods")
@Tag(name = "Métodos de Pago", description = "Consulta de métodos de pago disponibles")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.OPTIONS})
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @Operation(summary = "Listar métodos de pago activos")
    @GetMapping
    public ResponseEntity<List<PaymentMethodResponse>> getActivePaymentMethods() {
        return ResponseEntity.ok(paymentMethodService.getActivePaymentMethods());
    }

    @Operation(summary = "Listar todos los métodos de pago")
    @GetMapping("/all")
    public ResponseEntity<List<PaymentMethodResponse>> getAllPaymentMethods() {
        return ResponseEntity.ok(paymentMethodService.getAllPaymentMethods());
    }

    @Operation(summary = "Crear método de pago")
    @PostMapping
    public ResponseEntity<Void> createPaymentMethod(@RequestBody CreatePaymentMethodRequest request) {
        paymentMethodService.createPaymentMethod(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Cambiar estado del método de pago")
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Void> togglePaymentMethodStatus(@PathVariable("id") UUID id) {
        paymentMethodService.togglePaymentMethodStatus(id);
        return ResponseEntity.ok().build();
    }
}