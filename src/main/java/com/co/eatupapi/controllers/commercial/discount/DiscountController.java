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
    public List<DiscountDTO> getAllDiscounts() {
        return discountService.getAllDiscounts();
    }

    @GetMapping("/active")
    public List<DiscountDTO> getActiveDiscounts() {
        return discountService.getActiveDiscounts();
    }

    @GetMapping("/{discountId}")
    public ResponseEntity<?> getDiscountById(@PathVariable UUID discountId) {
        return discountService.getDiscountById(discountId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Descuento no encontrado con id: " + discountId)));
    }

    @PostMapping
    public ResponseEntity<DiscountDTO> createDiscount(@RequestBody DiscountDTO discountDto) {
        DiscountDTO created = discountService.createDiscount(discountDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{discountId}")
    public ResponseEntity<?> updateDiscount(
            @PathVariable UUID discountId,
            @RequestBody DiscountDTO discountDto
    ) {
        return discountService.updateDiscount(discountId, discountDto)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Descuento no encontrado con id: " + discountId)));
    }

    @PatchMapping("/{discountId}/status")
    public ResponseEntity<?> updateDiscountStatus(
            @PathVariable UUID discountId,
            @RequestBody Map<String, Boolean> request
    ) {
        return discountService.updateDiscountStatus(discountId, request.get("status"))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Descuento no encontrado con id: " + discountId)));
    }

    @DeleteMapping("/{discountId}")
    public ResponseEntity<?> deleteDiscount(@PathVariable UUID discountId) {
        if (discountService.deleteDiscount(discountId)) {
            return ResponseEntity.ok(Map.of("message", "Descuento eliminado con exito"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Descuento no encontrado con id: " + discountId));
    }
}