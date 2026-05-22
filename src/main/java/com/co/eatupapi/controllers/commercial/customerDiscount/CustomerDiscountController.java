package com.co.eatupapi.controllers.commercial.customerDiscount;

import com.co.eatupapi.dto.commercial.customerDiscount.CustomerDiscountDTO;
import com.co.eatupapi.dto.commercial.customerDiscount.CustomerDiscountAsyncResponseDTO;
import com.co.eatupapi.services.commercial.customerDiscount.CustomerDiscountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/commercial/api/v1")
public class CustomerDiscountController {

    private final CustomerDiscountService customerDiscountService;

    public CustomerDiscountController(CustomerDiscountService customerDiscountService) {
        this.customerDiscountService = customerDiscountService;
    }

    @GetMapping("/customer-discounts")
    public ResponseEntity<List<CustomerDiscountDTO>> getAllCustomerDiscounts() {
        return ResponseEntity.ok(customerDiscountService.getAllCustomerDiscounts());
    }

    @GetMapping("/customer-discounts/{id}")
    public ResponseEntity<CustomerDiscountDTO> getCustomerDiscountById(@PathVariable UUID id) {
        return ResponseEntity.ok(customerDiscountService.getCustomerDiscountById(id));
    }

    @GetMapping("/discounts/{discountId}/customers")
    public ResponseEntity<List<CustomerDiscountDTO>> getCustomersByDiscountId(@PathVariable UUID discountId) {
        return ResponseEntity.ok(customerDiscountService.getCustomersByDiscountId(discountId));
    }

    @GetMapping("/customers/{customerId}/discounts")
    public ResponseEntity<List<CustomerDiscountDTO>> getDiscountsByCustomerId(
            @PathVariable UUID customerId,
            @RequestParam(required = false) UUID locationId) {
        if (locationId != null) {
            return ResponseEntity.ok(
                    customerDiscountService.getDiscountsByCustomerAndLocation(customerId, locationId));
        }
        return ResponseEntity.ok(customerDiscountService.getDiscountsByCustomerId(customerId));
    }

    @GetMapping("/customer-discounts/{id}/validate")
    public ResponseEntity<CustomerDiscountDTO> getApplicableCustomerDiscount(
            @PathVariable UUID id,
            @RequestParam UUID customerId,
            @RequestParam UUID locationId) {
        return ResponseEntity.ok(
                customerDiscountService.getApplicableCustomerDiscount(id, customerId, locationId));
    }

    @PostMapping("/customer-discounts")
    public ResponseEntity<CustomerDiscountAsyncResponseDTO> createCustomerDiscount(
            @Valid @RequestBody CustomerDiscountDTO customerDiscountDto) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(customerDiscountService.createCustomerDiscount(customerDiscountDto));
    }

    @PutMapping("/customer-discounts/{id}")
    public ResponseEntity<CustomerDiscountAsyncResponseDTO> updateCustomerDiscount(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerDiscountDTO customerDiscountDto) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(customerDiscountService.updateCustomerDiscount(id, customerDiscountDto));
    }

    @DeleteMapping("/customer-discounts/{id}")
    public ResponseEntity<CustomerDiscountAsyncResponseDTO> deleteCustomerDiscount(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(customerDiscountService.deleteCustomerDiscount(id));
    }
}
