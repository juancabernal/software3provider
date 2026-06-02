package com.co.eatupapi.controllers.commercial.purchase;

import com.co.eatupapi.domain.commercial.purchase.PurchaseStatus;
import com.co.eatupapi.dto.commercial.purchase.CreatePurchaseRequest;
import com.co.eatupapi.dto.commercial.purchase.PurchaseResponse;
import com.co.eatupapi.dto.commercial.purchase.UpdatePurchaseStatusRequest;
import com.co.eatupapi.services.commercial.purchase.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations/{locationId}/purchases")
@Tag(name = "Compras", description = "Gestión de compras de productos a proveedores")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @Operation(summary = "Crear una compra", description = "Registra una nueva compra de productos a un proveedor.")
    @ApiResponse(responseCode = "201", description = "Compra creada exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos en el request")
    @PostMapping
    public ResponseEntity<PurchaseResponse> createPurchase(
            @PathVariable UUID locationId,
            @Valid @RequestBody CreatePurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(purchaseService.createPurchase(locationId, request));
    }

    @Operation(summary = "Consultar una compra", description = "Retorna una compra por su ID.")
    @ApiResponse(responseCode = "200", description = "Compra encontrada")
    @ApiResponse(responseCode = "404", description = "Compra no encontrada")
    @GetMapping("/{purchaseId}")
    public ResponseEntity<PurchaseResponse> getPurchaseById(
            @PathVariable UUID locationId,
            @Parameter(description = "ID de la compra") @PathVariable UUID purchaseId) {
        return ResponseEntity.ok(purchaseService.getPurchaseById(locationId, purchaseId));
    }

    @Operation(summary = "Listar compras", description = "Retorna las compras con paginación, opcionalmente filtradas por estado.")
    @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    @GetMapping
    public ResponseEntity<Page<PurchaseResponse>> getPurchases(
            @PathVariable UUID locationId,
            @Parameter(description = "Estado de la compra") @RequestParam(required = false) PurchaseStatus status,
            @Parameter(description = "Número de página (inicia en 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de resultados por página") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        return ResponseEntity.ok(purchaseService.getPurchases(locationId,status, pageable));
    }

    @Operation(summary = "Actualizar una compra", description = "Actualiza los datos de una compra en estado CREATED.")
    @ApiResponse(responseCode = "200", description = "Compra actualizada exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o estado no permite modificación")
    @ApiResponse(responseCode = "404", description = "Compra no encontrada")
    @PutMapping("/{purchaseId}")
    public ResponseEntity<PurchaseResponse> updatePurchase(
            @PathVariable UUID locationId,
            @Parameter(description = "ID de la compra") @PathVariable UUID purchaseId,
            @Valid @RequestBody CreatePurchaseRequest request) {
        return ResponseEntity.ok(purchaseService.updatePurchase(locationId,purchaseId, request));
    }

    @Operation(summary = "Cambiar estado de una compra", description = "Actualiza el estado: CREATED → APPROVED/CANCELLED, APPROVED → RECEIVED/CANCELLED.")
    @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente")
    @ApiResponse(responseCode = "400", description = "Transición de estado no permitida")
    @ApiResponse(responseCode = "404", description = "Compra no encontrada")
    @PatchMapping("/{purchaseId}/status")
    public ResponseEntity<PurchaseResponse> updateStatus(
            @PathVariable UUID locationId,
            @Parameter(description = "ID de la compra") @PathVariable UUID purchaseId,
            @Valid @RequestBody UpdatePurchaseStatusRequest request) {
        return ResponseEntity.ok(purchaseService.updateStatus(locationId,purchaseId, request.getStatus()));
    }

    @Operation(summary = "Eliminar una compra", description = "Soft-delete de la compra si está en estado CREATED o CANCELLED.")
    @ApiResponse(responseCode = "204", description = "Compra eliminada exitosamente")
    @ApiResponse(responseCode = "400", description = "Estado no permite eliminación")
    @ApiResponse(responseCode = "404", description = "Compra no encontrada")
    @DeleteMapping("/{purchaseId}")
    public ResponseEntity<Void> deletePurchase(
            @PathVariable UUID locationId,
            @Parameter(description = "ID de la compra") @PathVariable UUID purchaseId) {
        purchaseService.deletePurchase(locationId,purchaseId);
        return ResponseEntity.noContent().build();
    }
}