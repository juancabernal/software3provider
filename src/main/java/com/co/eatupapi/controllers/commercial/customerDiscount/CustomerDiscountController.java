package com.co.eatupapi.controllers.commercial.customerDiscount;

import com.co.eatupapi.dto.commercial.customerDiscount.CustomerDiscountDTO;
import com.co.eatupapi.services.commercial.customerDiscount.CustomerDiscountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/comercial/api/v1")
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
    public ResponseEntity<List<CustomerDiscountDTO>> getCustomersByDiscountId(
            @PathVariable UUID discountId) {
        return ResponseEntity.ok(
                customerDiscountService.getCustomersByDiscountId(discountId));
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
    public ResponseEntity<CustomerDiscountDTO> createCustomerDiscount(@RequestBody CustomerDiscountDTO customerDiscountDto) {
        CustomerDiscountDTO created = customerDiscountService.createCustomerDiscount(customerDiscountDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/customer-discounts/{id}")
    public ResponseEntity<?> updateCustomerDiscount(
            @PathVariable UUID id,
            @RequestBody CustomerDiscountDTO customerDiscountDto
    ) {
        return customerDiscountService.updateCustomerDiscount(id, customerDiscountDto)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Descuento de cliente no encontrado con id: " + id)));
    }

    @DeleteMapping("/customer-discounts/{id}")
    public ResponseEntity<?> deleteCustomerDiscount(@PathVariable UUID id) {
        if (customerDiscountService.deleteCustomerDiscount(id)) {
            return ResponseEntity.ok(Map.of("message", "Se elimino el descuento al cliente con exito"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Descuento de cliente no encontrado con id: " + id));
    }

}
