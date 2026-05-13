package com.co.eatupapi.controllers.commercial.table;

import com.co.eatupapi.dto.commercial.table.*;
import com.co.eatupapi.services.commercial.table.TableService; // Importación de la interfaz
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/commercial/api/v1/tables")
public class TableController {

    private final TableService service;

    public TableController(TableService service) {
        this.service = service;
    }



    @PostMapping
    public ResponseEntity<TableDTO> createTable(@Valid @RequestBody TableDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createTable(request));
    }

    @PutMapping("/{tableId}")
    public ResponseEntity<TableDTO> updateTable(@PathVariable String tableId,
                                                @Valid @RequestBody TableDTO request) {
        return ResponseEntity.ok(service.updateTable(tableId, request));
    }

    @DeleteMapping("/{tableId}")
    public ResponseEntity<Void> deactivateTable(@PathVariable String tableId) {
        service.deactivateTable(tableId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<TableDTO>> getTables(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String venueId,
            @RequestParam(required = false) Boolean reserved,
            @RequestParam(required = false) Boolean canOpenNow) {
        return ResponseEntity.ok(service.getTables(status, venueId, reserved, canOpenNow));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<TableSessionDTO>> getAllSessions() {
        return ResponseEntity.ok(service.getAllSessions());
    }

    @GetMapping("/reservations/active")
    public ResponseEntity<List<TableReservationDTO>> getAllActiveReservations() {
        return ResponseEntity.ok(service.getAllActiveReservations());
    }

    @GetMapping("/{tableId}")
    public ResponseEntity<TableDTO> getTableById(@PathVariable String tableId) {
        return ResponseEntity.ok(service.getTableById(tableId));
    }

    @GetMapping("/venue/{venueId}")
    public ResponseEntity<List<TableDTO>> getTablesByVenue(@PathVariable String venueId) {
        return ResponseEntity.ok(service.getTablesByVenue(venueId));
    }

    // ── SESSIONS ──────────────────────────────────────────

    @PostMapping("/{tableId}/sessions")
    public ResponseEntity<TableSessionDTO> openSession(@PathVariable String tableId,
                                                       @Valid @RequestBody TableSessionDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.openSession(tableId, request));
    }

    @GetMapping("/{tableId}/sessions/active")
    public ResponseEntity<TableSessionDTO> getActiveSession(@PathVariable String tableId) {
        return ResponseEntity.ok(service.getActiveSession(tableId));
    }

    @GetMapping("/{tableId}/sessions")
    public ResponseEntity<List<TableSessionDTO>> getSessions(@PathVariable String tableId) {
        return ResponseEntity.ok(service.getSessions(tableId));
    }

    @PatchMapping("/{tableId}/sessions/{sessionId}/guests")
    public ResponseEntity<TableSessionDTO> updateGuestCount(@PathVariable String tableId,
                                                            @PathVariable String sessionId,
                                                            @RequestParam Integer guestCount) {
        return ResponseEntity.ok(service.updateGuestCount(tableId, sessionId, guestCount));
    }

    @PostMapping("/{tableId}/sessions/{sessionId}/close")
    public ResponseEntity<TableSessionDTO> closeSession(@PathVariable String tableId,
                                                        @PathVariable String sessionId) {
        return ResponseEntity.ok(service.closeSession(tableId, sessionId));
    }

    @GetMapping("/{tableId}/sessions/history")
    public ResponseEntity<List<TableSessionDTO>> getSessionHistory(@PathVariable String tableId) {
        return ResponseEntity.ok(service.getSessionHistory(tableId));
    }

    // ── RESERVATIONS ──────────────────────────────────────

    @PostMapping("/{tableId}/reservations")
    public ResponseEntity<TableReservationDTO> createReservation(@PathVariable String tableId,
                                                                 @Valid @RequestBody TableReservationDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createReservation(tableId, request));
    }

    @PutMapping("/{tableId}/reservations/{reservationId}")
    public ResponseEntity<TableReservationDTO> updateReservation(@PathVariable String tableId,
                                                                 @PathVariable String reservationId,
                                                                 @Valid @RequestBody TableReservationDTO request) {
        return ResponseEntity.ok(service.updateReservation(tableId, reservationId, request));
    }

    @DeleteMapping("/{tableId}/reservations/{reservationId}")
    public ResponseEntity<Void> cancelReservation(@PathVariable String tableId,
                                                  @PathVariable String reservationId) {
        service.cancelReservation(tableId, reservationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{tableId}/reservation")
    public ResponseEntity<TableReservationDTO> getActiveReservation(@PathVariable String tableId) {
        return ResponseEntity.ok(service.getActiveReservation(tableId));
    }

    @GetMapping("/{tableId}/reservations")
    public ResponseEntity<List<TableReservationDTO>> getReservations(@PathVariable String tableId) {
        return ResponseEntity.ok(service.getReservations(tableId));
    }

    @GetMapping("/reservations/search")
    public ResponseEntity<List<TableReservationDTO>> searchReservations(
            @RequestParam String guestDocumentNumber) {
        return ResponseEntity.ok(
                service.searchReservationsByGuestDocumentNumber(guestDocumentNumber)
        );
    }

    @PostMapping("/reservations/{reservationId}/seat")
    public ResponseEntity<TableSessionDTO> seatReservation(@PathVariable String reservationId,
                                                           @Valid @RequestBody TableSessionDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.seatReservation(reservationId, request));
    }

    // ── SUMMARY ───────────────────────────────────────────

    @GetMapping("/summary")
    public ResponseEntity<TableSummaryDTO> getSummary() {
        return ResponseEntity.ok(service.getSummary());
    }

    @GetMapping("/summary/venue/{venueId}")
    public ResponseEntity<TableSummaryDTO> getSummaryByVenue(@PathVariable String venueId) {
        return ResponseEntity.ok(service.getSummaryByVenue(venueId));
    }
}
