package com.co.eatupapi.controllers.commercial.discount;

import com.co.eatupapi.dto.commercial.discount.DiscountDTO;
import com.co.eatupapi.services.commercial.discount.DiscountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/comercial/api/v1/discounts")
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
        return ResponseEntity.ok(discountService.getDiscountById(discountId));
    }

    @PostMapping
    public ResponseEntity<DiscountDTO> createDiscount(@RequestBody DiscountDTO discountDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(discountService.createDiscount(discountDto));
    }

    @PutMapping("/{discountId}")
    public ResponseEntity<DiscountDTO> updateDiscount(@PathVariable UUID discountId,
                                                      @RequestBody DiscountDTO discountDto) {
        return ResponseEntity.ok(discountService.updateDiscount(discountId, discountDto));
    }

    @PatchMapping("/{discountId}/status")
    public ResponseEntity<DiscountDTO> updateDiscountStatus(@PathVariable UUID discountId,
                                                            @RequestBody Map<String, Boolean> request) {
        return ResponseEntity.ok(discountService.updateDiscountStatus(discountId, request.get("status")));
    }

    @DeleteMapping("/{discountId}")
    public ResponseEntity<Void> deleteDiscount(@PathVariable UUID discountId) {
        discountService.deleteDiscount(discountId);
        return ResponseEntity.ok().build();
    }
}