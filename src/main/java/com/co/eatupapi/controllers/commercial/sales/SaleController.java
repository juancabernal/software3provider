package com.co.eatupapi.controllers.commercial.sales;

import com.co.eatupapi.dto.commercial.sales.SaleAsyncResponseDTO;
import com.co.eatupapi.dto.commercial.sales.SalePatchDTO;
import com.co.eatupapi.dto.commercial.sales.SaleRequestDTO;
import com.co.eatupapi.dto.commercial.sales.SaleResponseDTO;
import com.co.eatupapi.services.commercial.sales.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/commercial/api/v1/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @Operation(summary = "Crear una venta")
    @PostMapping
    public ResponseEntity<SaleAsyncResponseDTO> create(@Valid @RequestBody SaleRequestDTO request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(saleService.createSale(request));
    }

    @Operation(summary = "Obtener una venta por ID")
    @GetMapping("/{id}")
    public ResponseEntity<SaleResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(saleService.getSaleById(id));
    }

    @Operation(summary = "Listar todas las ventas")
    @GetMapping
    public ResponseEntity<List<SaleResponseDTO>> findAll() {
        return ResponseEntity.ok(saleService.getAllSales());
    }

    @Operation(summary = "Actualizar una venta")
    @PutMapping("/{id}")
    public ResponseEntity<SaleAsyncResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody SaleRequestDTO request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(saleService.updateSale(id, request));
    }

    @Operation(summary = "Actualizar parcialmente una venta")
    @PatchMapping("/{id}")
    public ResponseEntity<SaleAsyncResponseDTO> patch(@PathVariable UUID id, @Valid @RequestBody SalePatchDTO request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(saleService.patchSale(id, request));
    }

    @Operation(summary = "Eliminar una venta")
    @DeleteMapping("/{id}")
    public ResponseEntity<SaleAsyncResponseDTO> delete(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(saleService.deleteSale(id));
    }
}
