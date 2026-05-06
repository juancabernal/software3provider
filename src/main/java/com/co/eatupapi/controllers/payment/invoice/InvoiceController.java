package com.co.eatupapi.controllers.payment.invoice;

import com.co.eatupapi.domain.payment.invoice.InvoiceStatus;
import com.co.eatupapi.dto.payment.invoice.InvoiceAcceptedResponse;
import com.co.eatupapi.dto.payment.invoice.InvoiceRequest;
import com.co.eatupapi.dto.payment.invoice.InvoiceResponse;
import com.co.eatupapi.dto.payment.invoice.InvoiceStatusUpdateRequest;
import com.co.eatupapi.services.payment.invoice.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations/{locationId}/invoices")
@Tag(name = "Facturas", description = "Gestion de facturas del sistema")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Operation(
            summary = "Crear factura",
            description = "Publica un comando asincrono para crear una nueva factura"
    )
    @ApiResponse(responseCode = "202", description = "Comando de creacion publicado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @PostMapping
    public ResponseEntity<InvoiceAcceptedResponse> createInvoice(
            @Parameter(description = "ID de la sede") @PathVariable UUID locationId,
            @Valid @RequestBody InvoiceRequest request) {

        InvoiceResponse response = invoiceService.createInvoice(locationId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                new InvoiceAcceptedResponse(
                        "Invoice create command published",
                        response.getInvoiceId(),
                        HttpStatus.ACCEPTED.name(),
                        LocalDateTime.now()
                )
        );
    }

    @Operation(
            summary = "Listar facturas",
            description = "Obtiene las facturas de una sede con paginacion"
    )
    @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> getInvoices(
            @Parameter(description = "ID de la sede") @PathVariable UUID locationId,
            @Parameter(description = "Numero de pagina") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad por pagina") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(invoiceService.getInvoicesByLocation(locationId, pageable));
    }

    @Operation(
            summary = "Obtener factura por ID",
            description = "Retorna una factura especifica de una sede"
    )
    @ApiResponse(responseCode = "200", description = "Factura encontrada")
    @ApiResponse(responseCode = "404", description = "Factura no encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(
            @Parameter(description = "ID de la sede") @PathVariable UUID locationId,
            @Parameter(description = "ID de la factura") @PathVariable UUID id) {

        return ResponseEntity.ok(invoiceService.getInvoiceById(locationId, id));
    }

    @Operation(
            summary = "Actualizar estado de factura",
            description = "Publica un comando asincrono para actualizar el estado de una factura"
    )
    @ApiResponse(responseCode = "202", description = "Comando de actualizacion publicado correctamente")
    @ApiResponse(responseCode = "400", description = "Transicion invalida")
    @ApiResponse(responseCode = "404", description = "Factura no encontrada")
    @PatchMapping("/{id}/status")
    public ResponseEntity<InvoiceAcceptedResponse> updateStatus(
            @Parameter(description = "ID de la sede") @PathVariable UUID locationId,
            @Parameter(description = "ID de la factura") @PathVariable UUID id,
            @Valid @RequestBody InvoiceStatusUpdateRequest request) {

        InvoiceResponse response = invoiceService.updateStatus(locationId, id, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                new InvoiceAcceptedResponse(
                        statusMessage(request.getStatus()),
                        response.getInvoiceId(),
                        HttpStatus.ACCEPTED.name(),
                        LocalDateTime.now()
                )
        );
    }

    private String statusMessage(InvoiceStatus status) {
        if (status == InvoiceStatus.CANCELLED) {
            return "Invoice cancel command published";
        }
        if (status == InvoiceStatus.PAID) {
            return "Invoice mark-paid command published";
        }
        return "Invoice status command published";
    }
}
