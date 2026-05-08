package com.co.eatupapi.controllers.commercial.seller;

import com.co.eatupapi.dto.commercial.seller.SellerDTO;
import com.co.eatupapi.dto.commercial.seller.SellerPatchDTO;
import com.co.eatupapi.dto.commercial.seller.SellerStatusUpdateDTO;
import com.co.eatupapi.services.commercial.seller.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comercialapi/v1/sellers")
@Tag(name = "Vendedores", description = "Gestión de vendedores del sistema")
public class SellerController {

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @Operation(
            summary = "Crear vendedor",
            description = "Acepta la solicitud de creación del vendedor y publica el evento para que commercial-service lo procese."
    )
    @ApiResponse(responseCode = "202", description = "Solicitud de creación enviada a RabbitMQ")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o vendedor duplicado")
    @PostMapping
    public ResponseEntity<SellerDTO> createSeller(@RequestBody SellerDTO request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(sellerService.createSeller(request));
    }

    @Operation(
            summary = "Listar vendedores",
            description = "Retorna todos los vendedores. Se puede filtrar por status: ACTIVE o INACTIVE."
    )
    @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    @ApiResponse(responseCode = "400", description = "Valor de status inválido")
    @GetMapping
    public ResponseEntity<List<SellerDTO>> getSellers(
            @Parameter(description = "Filtrar por estado: ACTIVE o INACTIVE")
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(sellerService.getSellers(status));
    }

    @Operation(
            summary = "Obtener vendedor por ID",
            description = "Retorna el vendedor correspondiente al ID proporcionado."
    )
    @ApiResponse(responseCode = "200", description = "Vendedor encontrado")
    @ApiResponse(responseCode = "404", description = "Vendedor no encontrado")
    @GetMapping("/{sellerId}")
    public ResponseEntity<SellerDTO> getSellerById(
            @Parameter(description = "ID del vendedor") @PathVariable UUID sellerId) {
        return ResponseEntity.ok(sellerService.getSellerById(sellerId));
    }

    @Operation(
            summary = "Actualizar vendedor",
            description = "Acepta la solicitud de actualización completa y publica el evento para que commercial-service lo procese."
    )
    @ApiResponse(responseCode = "202", description = "Solicitud de actualización enviada a RabbitMQ")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o intento de cambiar email")
    @ApiResponse(responseCode = "404", description = "Vendedor no encontrado")
    @PutMapping("/{sellerId}")
    public ResponseEntity<SellerDTO> updateSeller(
            @Parameter(description = "ID del vendedor") @PathVariable UUID sellerId,
            @RequestBody SellerDTO request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(sellerService.updateSeller(sellerId, request));
    }

    @Operation(
            summary = "Actualizar estado del vendedor",
            description = "Acepta la solicitud de cambio de estado y publica el evento para que commercial-service lo procese."
    )
    @ApiResponse(responseCode = "202", description = "Solicitud de cambio de estado enviada a RabbitMQ")
    @ApiResponse(responseCode = "400", description = "Valor de estado inválido")
    @ApiResponse(responseCode = "404", description = "Vendedor no encontrado")
    @PatchMapping("/{sellerId}/status")
    public ResponseEntity<SellerDTO> updateStatus(
            @Parameter(description = "ID del vendedor") @PathVariable UUID sellerId,
            @RequestBody SellerStatusUpdateDTO request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(sellerService.updateStatus(sellerId, request.getStatus()));
    }

    @Operation(
            summary = "Actualizar campos parciales del vendedor",
            description = "Acepta la solicitud de actualización parcial y publica el evento para que commercial-service lo procese."
    )
    @ApiResponse(responseCode = "202", description = "Solicitud de actualización parcial enviada a RabbitMQ")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Vendedor no encontrado")
    @PatchMapping("/{sellerId}")
    public ResponseEntity<SellerDTO> patchSeller(
            @Parameter(description = "ID del vendedor") @PathVariable UUID sellerId,
            @RequestBody SellerPatchDTO request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(sellerService.patchSeller(sellerId, request));
    }
}