package com.co.eatupapi.controllers.inventory.transfer;

import com.co.eatupapi.dto.inventory.transfer.TransferObservacionUpdateDTO;
import com.co.eatupapi.dto.inventory.transfer.TransferRequestDTO;
import com.co.eatupapi.dto.inventory.transfer.TransferResponseDTO;
import com.co.eatupapi.dto.inventory.transfer.TransferStatusUpdateDTO;
import com.co.eatupapi.services.inventory.transfer.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/inventory/api/v1/transfers")
@Tag(name="Traslados", description="Endpoints para gestionar los traslados de productos entre sedes en el inventario")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @Operation(summary = "Crear un nuevo traslado",
               description = "Permite crear un nuevo traslado de productos entre sedes. " +
                       "Requiere un objeto JSON con los detalles del traslado a crear.")
    @ApiResponse(responseCode = "201", description = "Traslado creado exitosamente")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida, por ejemplo, si falta un campo requerido o el formato es incorrecto")
    @ApiResponse(responseCode = "404", description = "No se encontró una sede o producto con el ID proporcionado")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    @PostMapping
    public ResponseEntity<TransferResponseDTO> createTransfer(@Valid @RequestBody TransferRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.create(request));
    }

    @Operation(summary = "Actualizar el estado de un traslado",
               description = "Permite actualizar el estado de un traslado existente. " +
                       "Requiere el ID del traslado a actualizar y un objeto JSON con el nuevo estado.")
    @ApiResponse(responseCode = "200", description = "Estado del traslado actualizado exitosamente")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida, por ejemplo, si el ID del traslado no es válido o el formato del nuevo estado es incorrecto")
    @ApiResponse(responseCode = "404", description = "No se encontró un traslado con el ID proporcionado")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    @PatchMapping("/{id}/status")
    public ResponseEntity<TransferResponseDTO> updateTransferStatus(@PathVariable Long id,
                                                                    @RequestParam(required = false) String sedeOrigen,
                                                                     @Valid @RequestBody TransferStatusUpdateDTO statusUpdate) {
        return ResponseEntity.ok(transferService.updateStatus(id, sedeOrigen, statusUpdate));
    }

    @Operation(summary = "Obtener traslado por ID",
               description = "Permite obtener los detalles de un traslado específico utilizando su ID. " +
                       "El ID debe ser un número entero válido.")
    @ApiResponse(responseCode = "200", description = "Traslado encontrado y devuelto exitosamente")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida, por ejemplo, si el ID del traslado no es un número entero válido")
    @ApiResponse(responseCode = "404", description = "No se encontró un traslado con el ID proporcionado")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    @GetMapping("/{id}")
    public ResponseEntity<TransferResponseDTO> getTransferById(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.findById(id));
    }

    @Operation(summary = "Obtener todos los traslados",
               description = "Permite obtener una lista de todos los traslados registrados en el sistema.")
    @ApiResponse(responseCode = "200", description = "Lista de traslados devuelta exitosamente")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    @GetMapping
    public ResponseEntity<List<TransferResponseDTO>> getAllTransfers() {
        return ResponseEntity.ok(transferService.findAll());
    }

    @Operation(summary = "Obtener traslados en transito",
               description = "Permite obtener una lista de los traslados que actualmente estan en estado EN_PROCESO.")
    @ApiResponse(responseCode = "200", description = "Lista de traslados en transito devuelta exitosamente")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    @GetMapping("/in-transit")
    public ResponseEntity<List<TransferResponseDTO>> getTransfersInTransit() {
        return ResponseEntity.ok(transferService.findAllInTransit());
    }

    @Operation(summary = "Obtener traslados completados",
               description = "Permite obtener una lista de los traslados que actualmente estan en estado COMPLETADO.")
    @ApiResponse(responseCode = "200", description = "Lista de traslados completados devuelta exitosamente")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    @GetMapping("/completed")
    public ResponseEntity<List<TransferResponseDTO>> getCompletedTransfers() {
        return ResponseEntity.ok(transferService.findAllCompleted());
    }

    @Operation(summary = "Obtener traslados cancelados",
               description = "Permite obtener una lista de los traslados que actualmente estan en estado CANCELADO.")
    @ApiResponse(responseCode = "200", description = "Lista de traslados cancelados devuelta exitosamente")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    @GetMapping("/cancelled")
    public ResponseEntity<List<TransferResponseDTO>> getCancelledTransfers() {
        return ResponseEntity.ok(transferService.findAllCancelled());
    }

    @Operation(summary = "Obtener traslados reclamados",
               description = "Permite obtener una lista de los traslados que actualmente estan en estado RECLAMADO.")
    @ApiResponse(responseCode = "200", description = "Lista de traslados reclamados devuelta exitosamente")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    @GetMapping("/claimed")
    public ResponseEntity<List<TransferResponseDTO>> getClaimedTransfers() {
        return ResponseEntity.ok(transferService.findAllClaimed());
    }

    @Operation(summary = "Obtener traslados entrantes de la sede destino",
               description = "Permite consultar los traslados que llegan a una sede destino.")
    @ApiResponse(responseCode = "200", description = "Lista de traslados entrantes devuelta exitosamente")
    @GetMapping("/incoming")
    public ResponseEntity<List<TransferResponseDTO>> getIncomingTransfers(@RequestParam String sedeDestino) {
        return ResponseEntity.ok(transferService.findIncoming(sedeDestino));
    }

    @Operation(summary = "Confirmar recepción del traslado",
               description = "Permite a la sede destino confirmar que el traslado llegó correctamente.")
    @ApiResponse(responseCode = "200", description = "Traslado confirmado exitosamente")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida o estado no permitido")
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<TransferResponseDTO> confirmTransferReceipt(@PathVariable Long id,
                                                                      @RequestParam String sedeDestino) {
        return ResponseEntity.ok(transferService.confirmReceipt(id, sedeDestino));
    }

    @Operation(summary = "Reclamar recepción del traslado",
               description = "Permite a la sede destino reclamar novedades o inconsistencias en el traslado recibido.")
    @ApiResponse(responseCode = "200", description = "Reclamo del traslado registrado exitosamente")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida o estado no permitido")
    @PatchMapping("/{id}/claim")
    public ResponseEntity<TransferResponseDTO> claimTransferReceipt(@PathVariable Long id,
                                                                    @RequestParam String sedeDestino,
                                                                    @RequestBody TransferObservacionUpdateDTO observacionUpdate) {
        return ResponseEntity.ok(transferService.claimReceipt(id, sedeDestino, observacionUpdate));
    }
}
