package com.co.eatupapi.controllers.commercial.discount;

import com.co.eatupapi.dto.commercial.discount.DiscountDTO;
import com.co.eatupapi.dto.commercial.discount.DiscountAsyncResponseDTO;
import com.co.eatupapi.dto.commercial.discount.UpdateDiscountStatusRequest;
import com.co.eatupapi.services.commercial.discount.DiscountService;
import com.co.eatupapi.utils.commercial.discount.exceptions.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/commercial/api/v1/discounts")
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @GetMapping
    public ResponseEntity<List<DiscountDTO>> getAllDiscounts() {
        return ResponseEntity.ok(discountService.getAllDiscounts());
    }

    @GetMapping("/active")
    public ResponseEntity<List<DiscountDTO>> getActiveDiscounts() {
        return ResponseEntity.ok(discountService.getActiveDiscounts());
    }

    @GetMapping("/{discountId}")
    public ResponseEntity<DiscountDTO> getDiscountById(@PathVariable UUID discountId) {
        return ResponseEntity.ok(discountService.getDiscountById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Descuento no encontrado con id: " + discountId)));
    }//cambiar cuando facturas quite el acoplamiento

    @PostMapping
    public ResponseEntity<DiscountAsyncResponseDTO> createDiscount(@Valid @RequestBody DiscountDTO discountDto) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(discountService.createDiscount(discountDto));
    }

    @PutMapping("/{discountId}")
    public ResponseEntity<DiscountAsyncResponseDTO> updateDiscount(@PathVariable UUID discountId,
                                                                   @Valid @RequestBody DiscountDTO discountDto) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(discountService.updateDiscount(discountId, discountDto));
    }

    @PatchMapping("/{discountId}/status")
    public ResponseEntity<DiscountAsyncResponseDTO> updateDiscountStatus(
            @PathVariable UUID discountId,
            @Valid @RequestBody UpdateDiscountStatusRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(discountService.updateDiscountStatus(discountId, request.getStatus()));
    }

    @DeleteMapping("/{discountId}")
    public ResponseEntity<DiscountAsyncResponseDTO> deleteDiscount(@PathVariable UUID discountId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(discountService.deleteDiscount(discountId));
    }
}