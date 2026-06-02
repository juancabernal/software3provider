package com.co.eatupapi.services.commercial.table;

import com.co.eatupapi.domain.commercial.table.ReservationStatus;
import com.co.eatupapi.domain.commercial.table.TableDomain;
import com.co.eatupapi.domain.commercial.table.TableReservationDomain;
import com.co.eatupapi.domain.commercial.table.TableSessionDomain;
import com.co.eatupapi.domain.commercial.table.TableStatus;
import com.co.eatupapi.dto.commercial.table.TableDTO;
import com.co.eatupapi.dto.commercial.table.TableReservationDTO;
import com.co.eatupapi.dto.commercial.table.TableSessionDTO;
import com.co.eatupapi.dto.commercial.table.TableSummaryDTO;
import com.co.eatupapi.messaging.commercial.table.TableEventPublisher;
import com.co.eatupapi.repositories.commercial.table.TableRepository;
import com.co.eatupapi.repositories.commercial.table.TableReservationRepository;
import com.co.eatupapi.repositories.commercial.table.TableSessionRepository;
import com.co.eatupapi.utils.commercial.table.exceptions.TableBusinessException;
import com.co.eatupapi.utils.commercial.table.exceptions.TableResourceNotFoundException;
import com.co.eatupapi.utils.commercial.table.exceptions.TableValidationException;
import com.co.eatupapi.utils.commercial.table.mapper.TableMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TableServiceImpl implements TableService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long RESERVATION_LOCK_HOURS = 1;
    private static final long RESERVATION_GRACE_MINUTES = 15;
    private static final Set<ReservationStatus> RESERVATION_STATUSES_TO_EXPIRE = EnumSet.of(ReservationStatus.PENDING);
    private static final Set<ReservationStatus> RESERVATION_STATUSES_FOR_OVERLAP = EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

    private final TableRepository tableRepository;
    private final TableSessionRepository sessionRepository;
    private final TableReservationRepository reservationRepository;
    private final TableMapper tableMapper;
    private final TableEventPublisher publisher;

    public TableServiceImpl(TableRepository tableRepository,
                            TableSessionRepository sessionRepository,
                            TableReservationRepository reservationRepository,
                            TableMapper tableMapper,
                            TableEventPublisher publisher) {
        this.tableRepository = tableRepository;
        this.sessionRepository = sessionRepository;
        this.reservationRepository = reservationRepository;
        this.tableMapper = tableMapper;
        this.publisher = publisher;
    }

    // ── MESAS ─────────────────────────────────────────────────────────────────
    // Lee BD para validar → NO guarda → publica evento

    @Override
    public TableDTO createTable(TableDTO request) {
        validateCreateTableRequest(request);
        UUID venueId = parseUUID(request.getVenueId(), "venueId");
        validateTableNumberNotDuplicated(venueId, request.getTableNumber(), null);

        if (request.getLocation() != null) {
            request.setLocation(normalizeText(request.getLocation()));
        }
        if (request.getIsVip() == null) request.setIsVip(false);
        if (request.getHasView() == null) request.setHasView(false);
        if (request.getIsAccessible() == null) request.setIsAccessible(false);

        // ✅ NO guarda — publica para que comercialservice guarde
        publisher.publishTableCreated(request);
        return request;
    }

    @Override
    public TableDTO updateTable(String tableId, TableDTO request) {
        validateUpdateTableRequest(request);
        TableDomain existing = findTableById(tableId); // valida que exista y esté activa
        UUID venueId = parseUUID(request.getVenueId(), "venueId");
        validateTableNumberNotDuplicated(venueId, request.getTableNumber(), existing.getId());

        if (request.getLocation() != null) {
            request.setLocation(normalizeText(request.getLocation()));
        }
        request.setId(tableId);

        // ✅ NO guarda — publica para que comercialservice guarde
        publisher.publishTableUpdated(request);
        return request;
    }

    @Override
    public void deactivateTable(String tableId) {
        TableDomain table = findTableById(tableId);
        LocalDateTime now = now();

        // Valida que no haya sesión activa
        TableSessionDomain activeSession = findActiveSessionDomainByTableId(table.getId());
        if (activeSession != null) {
            throw new TableBusinessException("No se puede desactivar la mesa " + table.getTableNumber() + " porque tiene una sesión activa abierta");
        }

        // Valida que no haya reserva pendiente
        TableReservationDomain nextPendingReservation = findNextPendingReservation(table.getId(), now);
        if (nextPendingReservation != null) {
            throw new TableBusinessException(
                    "No se puede desactivar la mesa " + table.getTableNumber() +
                            " porque tiene una reserva pendiente para " + formatReservationDateTime(nextPendingReservation)
            );
        }

        // ✅ NO guarda — publica para que comercialservice guarde
        publisher.publishTableDeactivated(tableId);
    }

    // ── SESIONES ──────────────────────────────────────────────────────────────

    @Override
    public TableSessionDTO openSession(String tableId, TableSessionDTO request) {
        validateSessionPayload(request);
        TableDomain table = findTableById(tableId);
        LocalDateTime now = now();
        expireOverdueReservations(table.getId(), now);

        // Valida que no haya sesión activa
        TableSessionDomain activeSession = findActiveSessionDomainByTableId(table.getId());
        if (activeSession != null) {
            throw new TableBusinessException("La mesa " + table.getTableNumber() + " ya tiene una sesión activa. Debe cerrarla antes de abrir una nueva");
        }

        // Valida reserva si viene en el request
        if (isNotBlank(request.getReservationId())) {
            TableReservationDomain reservationToUse = findReservationById(request.getReservationId());
            if (!reservationToUse.getTableId().equals(table.getId())) {
                throw new TableBusinessException("La reserva indicada no pertenece a la mesa " + table.getTableNumber());
            }
            validateReservationCanBeUsedToOpen(reservationToUse, now);
        } else {
            TableReservationDomain blockingReservation = findBlockingPendingReservation(table.getId(), now);
            if (blockingReservation != null) {
                throw new TableBusinessException(buildBlockedTableMessage(table, blockingReservation));
            }
        }

        request.setTableId(tableId);
        request.setId(UUID.randomUUID().toString());

        // ✅ NO guarda — publica para que comercialservice guarde
        publisher.publishSessionOpened(request);
        return request;
    }

    @Override
    public TableSessionDTO getActiveSession(String tableId) {
        TableDomain table = findTableById(tableId);
        TableSessionDTO activeSession = findActiveSessionByTableId(table.getId(), now());
        if (activeSession == null) {
            throw new TableResourceNotFoundException("No se encontró una sesión activa para la mesa: " + tableId);
        }
        return activeSession;
    }

    @Override
    public List<TableSessionDTO> getAllSessions() {
        LocalDateTime now = now();
        List<TableSessionDTO> result = new ArrayList<>();
        for (TableSessionDomain session : sessionRepository.findAllByOrderByOpenedAtDesc()) {
            result.add(toSessionDtoWithDuration(session, now));
        }
        return result;
    }

    @Override
    public List<TableSessionDTO> getSessions(String tableId) {
        TableDomain table = findTableById(tableId);
        LocalDateTime now = now();
        List<TableSessionDTO> result = new ArrayList<>();
        for (TableSessionDomain session : sessionRepository.findAllByTableIdOrderByOpenedAtDesc(table.getId())) {
            result.add(toSessionDtoWithDuration(session, now));
        }
        return result;
    }

    @Override
    public TableSessionDTO updateGuestCount(String tableId, String sessionId, Integer guestCount) {
        TableDomain table = findTableById(tableId);
        if (guestCount == null || guestCount < 1) {
            throw new TableValidationException("El campo 'guestCount' debe ser un número positivo mayor que cero");
        }

        // Valida que la sesión exista y pertenezca a la mesa
        TableSessionDomain session = findSessionById(sessionId);
        if (!session.getTableId().equals(table.getId())) {
            throw new TableBusinessException("La sesión indicada no pertenece a la mesa " + table.getTableNumber());
        }
        if (session.getClosedAt() != null) {
            throw new TableBusinessException("No se puede actualizar el número de comensales de una sesión que ya está cerrada");
        }

        // ✅ NO guarda — publica para que comercialservice guarde
        publisher.publishSessionUpdated(tableId, sessionId, guestCount);

        TableSessionDTO response = tableMapper.toSessionDto(session);
        response.setGuestCount(guestCount);
        return response;
    }

    @Override
    public TableSessionDTO closeSession(String tableId, String sessionId) {
        TableDomain table = findTableById(tableId);
        TableSessionDomain session = findSessionById(sessionId);

        if (!session.getTableId().equals(table.getId())) {
            throw new TableBusinessException("La sesión indicada no pertenece a la mesa " + table.getTableNumber());
        }
        if (session.getClosedAt() != null) {
            throw new TableBusinessException("La sesión indicada ya se encuentra cerrada");
        }

        LocalDateTime closedAt = now();
        long durationMinutes = ChronoUnit.MINUTES.between(session.getOpenedAt(), closedAt);

        TableSessionDTO dto = tableMapper.toSessionDto(session);
        dto.setClosedAt(closedAt);
        dto.setDurationMinutes(durationMinutes);
        dto.setDurationText(formatDuration(durationMinutes));

        // ✅ NO guarda — publica para que comercialservice guarde
        publisher.publishSessionClosed(dto);
        return dto;
    }

    @Override
    public List<TableSessionDTO> getSessionHistory(String tableId) {
        return getSessions(tableId);
    }

    private TableSessionDTO toSessionDtoWithDuration(TableSessionDomain session, LocalDateTime now) {
        TableSessionDTO dto = tableMapper.toSessionDto(session);
        if (session.getClosedAt() != null && session.getDurationMinutes() != null) {
            dto.setDurationText(formatDuration(session.getDurationMinutes()));
        } else if (session.getClosedAt() == null) {
            long elapsed = ChronoUnit.MINUTES.between(session.getOpenedAt(), now);
            dto.setDurationMinutes(elapsed);
            dto.setDurationText(formatDuration(elapsed));
        }
        return dto;
    }

    // ── RESERVAS ──────────────────────────────────────────────────────────────

    @Override
    public TableReservationDTO createReservation(String tableId, TableReservationDTO request) {
        validateReservationPayload(request, true);
        TableDomain table = findTableById(tableId);
        LocalDateTime now = now();
        expireOverdueReservations(table.getId(), now);

        LocalDateTime reservationDateTime = buildReservationDateTime(request.getReservationDate(), request.getReservationTime());
        validateReservationDateTimeNotPast(reservationDateTime, now);
        validateReservationMinimumAdvance(reservationDateTime, now);
        validateReservationOverlap(table, reservationDateTime, null);

        if (request.getGuestName() != null) request.setGuestName(normalizeText(request.getGuestName()));
        if (request.getGuestDocumentNumber() != null) request.setGuestDocumentNumber(normalizeText(request.getGuestDocumentNumber()));
        request.setTableId(tableId);

        request.setId(UUID.randomUUID().toString());

        publisher.publishReservationCreated(request);
        return request;
    }

    @Override
    public TableReservationDTO getActiveReservation(String tableId) {
        TableDomain table = findTableById(tableId);
        LocalDateTime now = now();
        expireOverdueReservations(table.getId(), now);

        TableReservationDomain reservation = findNextRelevantReservation(table.getId(), now);
        if (reservation == null) {
            throw new TableResourceNotFoundException("No se encontró una reserva activa o pendiente para la mesa: " + tableId);
        }
        return tableMapper.toReservationDto(reservation);
    }

    @Override
    public List<TableReservationDTO> getAllActiveReservations() {
        LocalDateTime now = now();
        List<TableReservationDTO> result = new ArrayList<>();

        List<TableReservationDomain> reservations = reservationRepository
                .findAllByStatusInOrderByReservationDateAscReservationTimeAsc(
                        List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
                );

        for (TableReservationDomain reservation : reservations) {
            if (isActiveReservation(reservation, now)) {
                result.add(tableMapper.toReservationDto(reservation));
            }
        }

        return result;
    }

    @Override
    public List<TableReservationDTO> getReservations(String tableId) {
        TableDomain table = findTableById(tableId);
        LocalDateTime now = now();
        expireOverdueReservations(table.getId(), now);

        List<TableReservationDTO> result = new ArrayList<>();
        for (TableReservationDomain reservation : reservationRepository
                .findAllByTableIdOrderByReservationDateAscReservationTimeAsc(table.getId())) {
            result.add(tableMapper.toReservationDto(reservation));
        }
        return result;
    }

    @Override
    public TableReservationDTO updateReservation(String tableId, String reservationId, TableReservationDTO request) {
        validateReservationPayload(request, false);
        TableDomain table = findTableById(tableId);
        TableReservationDomain existing = findReservationById(reservationId);

        if (!existing.getTableId().equals(table.getId())) {
            throw new TableBusinessException("La reserva indicada no pertenece a la mesa " + table.getTableNumber());
        }

        LocalDateTime now = now();
        expireOverdueReservations(table.getId(), now);

        if (existing.getStatus() != ReservationStatus.PENDING) {
            throw new TableBusinessException("Solo se pueden modificar reservas pendientes. Estado actual: " + existing.getStatus());
        }

        LocalDateTime reservationDateTime = buildReservationDateTime(request.getReservationDate(), request.getReservationTime());
        validateReservationDateTimeNotPast(reservationDateTime, now);
        validateReservationMinimumAdvance(reservationDateTime, now);
        validateReservationOverlap(table, reservationDateTime, existing.getId());

        if (request.getGuestName() != null) request.setGuestName(normalizeText(request.getGuestName()));
        if (request.getGuestDocumentNumber() != null) request.setGuestDocumentNumber(normalizeText(request.getGuestDocumentNumber()));
        request.setId(reservationId);
        request.setTableId(tableId);

        // ✅ NO guarda — publica para que comercialservice guarde
        publisher.publishReservationUpdated(request);
        return request;
    }

    @Override
    public void cancelReservation(String tableId, String reservationId) {
        TableDomain table = findTableById(tableId);
        TableReservationDomain existing = findReservationById(reservationId);

        if (!existing.getTableId().equals(table.getId())) {
            throw new TableBusinessException("La reserva indicada no pertenece a la mesa " + table.getTableNumber());
        }

        LocalDateTime now = now();
        expireOverdueReservations(table.getId(), now);

        if (existing.getStatus() == ReservationStatus.CANCELLED) {
            throw new TableBusinessException("La reserva indicada ya se encuentra cancelada");
        }
        if (existing.getStatus() == ReservationStatus.COMPLETED) {
            throw new TableBusinessException("No se puede cancelar una reserva que ya fue completada");
        }
        if (existing.getStatus() == ReservationStatus.NO_SHOW) {
            throw new TableBusinessException("No se puede cancelar una reserva que ya fue marcada como no show");
        }
        if (existing.getStatus() == ReservationStatus.CONFIRMED) {
            throw new TableBusinessException("No se puede cancelar una reserva que ya fue confirmada en mesa");
        }

        // ✅ NO guarda — publica para que comercialservice guarde
        publisher.publishReservationCancelled(tableId, reservationId);
    }

    @Override
    public List<TableReservationDTO> searchReservationsByGuestDocumentNumber(String guestDocumentNumber) {
        validateRequiredText(guestDocumentNumber, "guestDocumentNumber");

        List<TableReservationDomain> reservations = reservationRepository
                .findAllByGuestDocumentNumberAndStatusInOrderByReservationDateAscReservationTimeAsc(
                        guestDocumentNumber.trim(),
                        List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
                );

        List<TableReservationDTO> result = new ArrayList<>();
        for (TableReservationDomain reservation : reservations) {
            result.add(tableMapper.toReservationDto(reservation));
        }
        return result;
    }

    @Override
    public TableSessionDTO seatReservation(String reservationId, TableSessionDTO request) {
        validateSessionPayload(request);

        TableReservationDomain reservation = findReservationById(reservationId);
        LocalDateTime now = now();
        LocalDateTime reservationDateTime = reservationDateTime(reservation);

        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.COMPLETED
                || reservation.getStatus() == ReservationStatus.NO_SHOW) {
            throw new TableBusinessException(
                    "La reserva no está disponible para sentar al cliente. Estado actual: " + reservation.getStatus());
        }

        if (now.isBefore(reservationDateTime)) {
            throw new TableBusinessException(
                    "El cliente aún no puede ser sentado. La reserva está programada para " + formatDateTime(reservationDateTime));
        }

        if (now.isAfter(graceEnd(reservation))) {
            throw new TableBusinessException("La reserva venció por no show y ya no puede utilizarse");
        }

        TableDomain table = findTableById(reservation.getTableId().toString());
        TableSessionDomain activeSession = findActiveSessionDomainByTableId(table.getId());
        if (activeSession != null) {
            throw new TableBusinessException("La mesa " + table.getTableNumber() + " aún se encuentra ocupada y no puede sentarse la reserva");
        }

        request.setTableId(table.getId().toString());
        request.setReservationId(reservationId);
        request.setId(UUID.randomUUID().toString());

        // ✅ NO guarda — publica para que comercialservice guarde
        publisher.publishReservationSeated(request);
        return request;
    }

    // ── RESUMEN (lectura directa de BD) ───────────────────────────────────────

    @Override
    public TableSummaryDTO getSummary() {
        return buildSummary(null);
    }

    @Override
    public TableSummaryDTO getSummaryByVenue(String venueId) {
        return buildSummary(parseUUID(venueId, "venueId"));
    }

    // ── GETs (lectura directa de BD) ──────────────────────────────────────────

    @Override
    public List<TableDTO> getTables(String status, String venueId, Boolean reserved, Boolean canOpenNow) {
        LocalDateTime now = now();
        TableStatus parsedStatus = parseStatus(status);
        UUID parsedVenueId = isNotBlank(venueId) ? parseUUID(venueId, "venueId") : null;

        List<TableDomain> domains = parsedVenueId != null
                ? tableRepository.findAllByActiveTrueAndVenueId(parsedVenueId)
                : tableRepository.findAllByActiveTrue();

        domains.sort(Comparator.comparing(TableDomain::getTableNumber));

        List<TableDTO> result = new ArrayList<>();
        for (TableDomain domain : domains) {
            TableDTO dto = enrichTableDto(domain, now);
            if (parsedStatus != null && dto.getStatus() != parsedStatus) continue;
            if (reserved != null && !reserved.equals(dto.getReserved())) continue;
            if (canOpenNow != null && !canOpenNow.equals(dto.getCanOpenNow())) continue;
            result.add(dto);
        }
        return result;
    }

    @Override
    public TableDTO getTableById(String tableId) {
        LocalDateTime now = now();
        TableDomain domain = findTableById(tableId);
        return enrichTableDto(domain, now);
    }

    @Override
    public List<TableDTO> getTablesByVenue(String venueId) {
        return getTables(null, venueId, null, null);
    }

    // ── HELPERS PRIVADOS ──────────────────────────────────────────────────────

    private TableDomain findTableById(String tableId) {
        UUID id = parseUUID(tableId, "tableId");
        TableDomain table = tableRepository.findById(id)
                .orElseThrow(() -> new TableResourceNotFoundException("No se encontró la mesa con id: " + tableId));
        if (!Boolean.TRUE.equals(table.getActive())) {
            throw new TableBusinessException("La mesa con id " + tableId + " existe pero está inactiva");
        }
        return table;
    }

    private TableSessionDomain findSessionById(String sessionId) {
        UUID id = parseUUID(sessionId, "sessionId");
        return sessionRepository.findById(id)
                .orElseThrow(() -> new TableResourceNotFoundException("No se encontró la sesión con id: " + sessionId));
    }

    private TableReservationDomain findReservationById(String reservationId) {
        UUID id = parseUUID(reservationId, "reservationId");
        return reservationRepository.findById(id)
                .orElseThrow(() -> new TableResourceNotFoundException("No se encontró la reserva con id: " + reservationId));
    }

    private TableSessionDomain findActiveSessionDomainByTableId(UUID tableId) {
        return sessionRepository.findByTableIdAndClosedAtIsNull(tableId).orElse(null);
    }

    private TableSessionDTO findActiveSessionByTableId(UUID tableId, LocalDateTime now) {
        TableSessionDomain session = findActiveSessionDomainByTableId(tableId);
        if (session == null) return null;
        TableSessionDTO dto = tableMapper.toSessionDto(session);
        long elapsed = ChronoUnit.MINUTES.between(session.getOpenedAt(), now);
        dto.setDurationMinutes(elapsed);
        dto.setDurationText(formatDuration(elapsed));
        return dto;
    }

    private TableDTO enrichTableDto(TableDomain table, LocalDateTime now) {
        expireOverdueReservations(table.getId(), now);

        TableSessionDTO activeSession = findActiveSessionByTableId(table.getId(), now);
        TableReservationDomain nextPendingReservation = findNextPendingReservation(table.getId(), now);
        TableReservationDomain nextRelevantReservation = findNextRelevantReservation(table.getId(), now);
        TableReservationDomain blockingPendingReservation = findBlockingPendingReservation(table.getId(), now);

        TableDTO dto = tableMapper.toDto(table);
        dto.setStatus(activeSession != null ? TableStatus.OCCUPIED : TableStatus.AVAILABLE);
        dto.setActiveSession(activeSession);
        dto.setActiveReservation(nextRelevantReservation != null ? tableMapper.toReservationDto(nextRelevantReservation) : null);
        dto.setReserved(nextPendingReservation != null);
        dto.setCanOpenNow(activeSession == null && blockingPendingReservation == null);

        boolean canOpenWithReservation = false;
        if (activeSession == null && blockingPendingReservation != null) {
            LocalDateTime rdv = reservationDateTime(blockingPendingReservation);
            canOpenWithReservation = !now.isBefore(rdv) && !now.isAfter(graceEnd(blockingPendingReservation));
        }
        dto.setCanOpenWithReservation(canOpenWithReservation);

        if (nextPendingReservation != null) {
            dto.setNextReservationAt(reservationDateTime(nextPendingReservation));
            dto.setReservationLockStartsAt(lockStart(nextPendingReservation));
            dto.setReservationGraceEndsAt(graceEnd(nextPendingReservation));
        }

        dto.setDisplayStatus(buildDisplayStatus(activeSession != null, nextPendingReservation, blockingPendingReservation, now));
        return dto;
    }

    private TableSummaryDTO buildSummary(UUID venueId) {
        LocalDateTime now = now();
        List<TableDomain> domains = venueId != null
                ? tableRepository.findAllByActiveTrueAndVenueId(venueId)
                : tableRepository.findAllByActiveTrue();

        long available = 0, occupied = 0, reserved = 0, blocked = 0;
        for (TableDomain domain : domains) {
            TableDTO dto = enrichTableDto(domain, now);
            if (dto.getStatus() == TableStatus.OCCUPIED) occupied++;
            else available++;
            if (Boolean.TRUE.equals(dto.getReserved())) reserved++;
            if (isBlockedForReservation(dto)) blocked++;
        }

        TableSummaryDTO summary = new TableSummaryDTO();
        summary.setTotalRegistered((long) domains.size());
        summary.setAvailable(available);
        summary.setOccupied(occupied);
        summary.setReserved(reserved);
        summary.setBlockedForReservation(blocked);
        summary.setVenueId(venueId != null ? venueId.toString() : null);
        return summary;
    }

    private boolean isBlockedForReservation(TableDTO dto) {
        return dto.getStatus() == TableStatus.AVAILABLE
                && Boolean.TRUE.equals(dto.getReserved())
                && !Boolean.TRUE.equals(dto.getCanOpenNow());
    }

    private String buildDisplayStatus(boolean occupied, TableReservationDomain nextPendingReservation,
                                      TableReservationDomain blockingPendingReservation, LocalDateTime now) {
        if (occupied) {
            if (blockingPendingReservation != null) return "OCCUPIED_WITH_IMMINENT_RESERVATION";
            if (nextPendingReservation != null) return "OCCUPIED_WITH_RESERVATION";
            return "OCCUPIED";
        }
        if (blockingPendingReservation != null) {
            if (now.isBefore(reservationDateTime(blockingPendingReservation))) return "BLOCKED_FOR_RESERVATION";
            return "WAITING_RESERVED_GUEST";
        }
        if (nextPendingReservation != null) return "AVAILABLE_WITH_RESERVATION";
        return "AVAILABLE";
    }

    private void validateCreateTableRequest(TableDTO request) {
        validateRequiredObject(request, "body");
        validateTableClientManagedFields(request);
        validateTablePayload(request);
    }

    private void validateUpdateTableRequest(TableDTO request) {
        validateRequiredObject(request, "body");
        validateTableClientManagedFields(request);
        validateTablePayload(request);
    }

    private void validateTableClientManagedFields(TableDTO request) {
        if (isNotBlank(request.getId()))
            throw new TableValidationException("El campo 'id' es administrado por el servidor y no debe enviarse");
        if (request.getStatus() != null)
            throw new TableValidationException("El campo 'status' es administrado por el servidor y no puede enviarse manualmente");
        if (request.getActive() != null)
            throw new TableValidationException("El campo 'active' es administrado por el servidor y no puede enviarse manualmente");
        if (request.getActiveSession() != null)
            throw new TableValidationException("El campo 'activeSession' es calculado por el sistema y no puede enviarse manualmente");
        if (request.getActiveReservation() != null)
            throw new TableValidationException("El campo 'activeReservation' es calculado por el sistema y no puede enviarse manualmente");
        if (request.getCreatedDate() != null || request.getModifiedDate() != null)
            throw new TableValidationException("Las fechas de creación y modificación son administradas por el servidor");
    }

    private void validateTablePayload(TableDTO request) {
        validateRequiredObject(request.getTableNumber(), "tableNumber");
        validateRequiredText(request.getLocation(), "location");
        validateRequiredText(request.getVenueId(), "venueId");
        if (request.getTableNumber() < 1)
            throw new TableValidationException("El campo 'tableNumber' debe ser un número positivo mayor que cero");
        if (normalizeText(request.getLocation()).length() > 100)
            throw new TableValidationException("El campo 'location' no puede superar los 100 caracteres");
    }

    private void validateSessionPayload(TableSessionDTO request) {
        validateRequiredObject(request, "body");
        if (isNotBlank(request.getId()))
            throw new TableValidationException("El campo 'id' de la sesión es administrado por el servidor y no debe enviarse");
        if (isNotBlank(request.getTableId()))
            throw new TableValidationException("El campo 'tableId' de la sesión se toma del path y no debe enviarse en el body");
        if (request.getOpenedAt() != null || request.getClosedAt() != null
                || request.getDurationMinutes() != null || isNotBlank(request.getDurationText()))
            throw new TableValidationException("Los campos de fechas y duración de la sesión son administrados por el servidor");
        validateRequiredObject(request.getGuestCount(), "guestCount");
        if (request.getGuestCount() < 1)
            throw new TableValidationException("El campo 'guestCount' debe ser un número positivo mayor que cero");
        if (request.getObservations() != null && request.getObservations().trim().length() > 500)
            throw new TableValidationException("El campo 'observations' no puede superar los 500 caracteres");
    }

    private void validateReservationPayload(TableReservationDTO request, boolean creating) {
        validateRequiredObject(request, "body");
        if (isNotBlank(request.getId()))
            throw new TableValidationException("El campo 'id' de la reserva es administrado por el servidor y no debe enviarse");
        if (isNotBlank(request.getTableId()))
            throw new TableValidationException("El campo 'tableId' de la reserva se toma del path y no debe enviarse en el body");
        if (request.getStatus() != null)
            throw new TableValidationException("El campo 'status' de la reserva es administrado por el servidor y no puede enviarse manualmente");
        if (request.getCreatedDate() != null || request.getReservationDateTime() != null
                || request.getReservationLockStartsAt() != null || request.getReservationGraceEndsAt() != null)
            throw new TableValidationException("Las fechas calculadas de la reserva son administradas por el servidor");
        validateRequiredObject(request.getReservationDate(), "reservationDate");
        validateRequiredObject(request.getReservationTime(), "reservationTime");
        validateRequiredText(request.getGuestName(), "guestName");
        validateRequiredText(request.getGuestDocumentNumber(), "guestDocumentNumber");
        if (request.getGuestCount() != null && request.getGuestCount() < 1)
            throw new TableValidationException("El campo 'guestCount' de la reserva debe ser un número positivo mayor que cero");
        if (request.getGuestName() != null && request.getGuestName().trim().length() > 100)
            throw new TableValidationException("El campo 'guestName' no puede superar los 100 caracteres");
        if (request.getGuestDocumentNumber() != null && request.getGuestDocumentNumber().trim().length() > 50)
            throw new TableValidationException("El campo 'guestDocumentNumber' no puede superar los 50 caracteres");
        if (!creating && request.getReservationDate() == null)
            throw new TableValidationException("El campo 'reservationDate' es requerido para actualizar la reserva");
    }

    private void validateReservationDateTimeNotPast(LocalDateTime reservationDateTime, LocalDateTime now) {
        if (reservationDateTime.isBefore(now))
            throw new TableValidationException("La fecha y hora de la reserva no pueden estar en el pasado");
    }

    private void validateReservationMinimumAdvance(LocalDateTime reservationDateTime, LocalDateTime now) {
        if (!reservationDateTime.isAfter(now.plusHours(RESERVATION_LOCK_HOURS)))
            throw new TableValidationException("La reserva debe registrarse con al menos 1 hora de anticipación");
    }

    private void validateReservationOverlap(TableDomain table, LocalDateTime requestedReservationDateTime, UUID reservationToExclude) {
        LocalDateTime requestedWindowStart = requestedReservationDateTime.minusHours(RESERVATION_LOCK_HOURS);
        LocalDateTime requestedWindowEnd = requestedReservationDateTime.plusMinutes(RESERVATION_GRACE_MINUTES);

        TableReservationDomain overlappingReservation = findOverlappingReservation(
                table.getId(), requestedWindowStart, requestedWindowEnd, reservationToExclude);

        if (overlappingReservation != null) {
            throw new TableBusinessException(
                    "La reserva solicitada para la mesa " + table.getTableNumber() +
                            " se cruza con la reserva " + overlappingReservation.getId() +
                            " programada para " + formatReservationDateTime(overlappingReservation) +
                            ". Cada reserva protege la mesa desde " + formatDateTime(lockStart(overlappingReservation)) +
                            " hasta " + formatDateTime(graceEnd(overlappingReservation))
            );
        }
    }

    private void validateReservationCanBeUsedToOpen(TableReservationDomain reservation, LocalDateTime now) {
        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.COMPLETED
                || reservation.getStatus() == ReservationStatus.NO_SHOW) {
            throw new TableBusinessException(
                    "La reserva indicada no puede usarse para abrir la mesa porque su estado actual es " + reservation.getStatus());
        }
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new TableBusinessException("La reserva indicada ya fue confirmada previamente y no puede reutilizarse para abrir otra sesión");
        }
        LocalDateTime reservationDateTime = reservationDateTime(reservation);
        LocalDateTime graceEnd = graceEnd(reservation);
        if (now.isBefore(reservationDateTime)) {
            throw new TableBusinessException(
                    "La reserva " + reservation.getId() + " solo puede sentarse a partir de la hora reservada: " + formatDateTime(reservationDateTime));
        }
        if (now.isAfter(graceEnd)) {
            throw new TableBusinessException(
                    "La reserva " + reservation.getId() + " venció por no show. El tiempo de tolerancia terminó a las " + formatDateTime(graceEnd));
        }
    }

    private void expireOverdueReservations(UUID tableId, LocalDateTime now) {
        // Solo lee para validar — NO guarda cambios de estado
        // comercialservice es responsable de expirar reservas en su BD
        List<TableReservationDomain> reservations = reservationRepository
                .findAllByTableIdOrderByReservationDateAscReservationTimeAsc(tableId);
        for (TableReservationDomain reservation : reservations) {
            if (RESERVATION_STATUSES_TO_EXPIRE.contains(reservation.getStatus()) && now.isAfter(graceEnd(reservation))) {
                reservation.setStatus(ReservationStatus.NO_SHOW);
            }
        }
        // ✅ Sin saveAll — solo mutamos en memoria para que las validaciones siguientes sean correctas
    }

    private TableReservationDomain findNextPendingReservation(UUID tableId, LocalDateTime now) {
        List<TableReservationDomain> reservations = reservationRepository
                .findAllByTableIdAndStatusInOrderByReservationDateAscReservationTimeAsc(tableId, List.of(ReservationStatus.PENDING));
        for (TableReservationDomain reservation : reservations) {
            if (!now.isAfter(graceEnd(reservation))) return reservation;
        }
        return null;
    }

    private TableReservationDomain findNextRelevantReservation(UUID tableId, LocalDateTime now) {
        List<TableReservationDomain> reservations = reservationRepository
                .findAllByTableIdAndStatusInOrderByReservationDateAscReservationTimeAsc(
                        tableId, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));
        for (TableReservationDomain reservation : reservations) {
            if (isActiveReservation(reservation, now)) return reservation;
        }
        return null;
    }

    private boolean isActiveReservation(TableReservationDomain reservation, LocalDateTime now) {
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return true;
        }
        return reservation.getStatus() == ReservationStatus.PENDING && !now.isAfter(graceEnd(reservation));
    }

    private TableReservationDomain findBlockingPendingReservation(UUID tableId, LocalDateTime now) {
        List<TableReservationDomain> reservations = reservationRepository
                .findAllByTableIdAndStatusInOrderByReservationDateAscReservationTimeAsc(tableId, List.of(ReservationStatus.PENDING));
        for (TableReservationDomain reservation : reservations) {
            if (!now.isBefore(lockStart(reservation)) && !now.isAfter(graceEnd(reservation))) return reservation;
        }
        return null;
    }

    private TableReservationDomain findOverlappingReservation(UUID tableId, LocalDateTime requestedWindowStart,
                                                              LocalDateTime requestedWindowEnd, UUID reservationToExclude) {
        List<TableReservationDomain> reservations = reservationRepository
                .findAllByTableIdAndStatusInOrderByReservationDateAscReservationTimeAsc(tableId, RESERVATION_STATUSES_FOR_OVERLAP);
        for (TableReservationDomain existing : reservations) {
            if (reservationToExclude != null && reservationToExclude.equals(existing.getId())) continue;
            boolean overlaps = requestedWindowStart.isBefore(graceEnd(existing)) && requestedWindowEnd.isAfter(lockStart(existing));
            if (overlaps) return existing;
        }
        return null;
    }

    private void validateTableNumberNotDuplicated(UUID venueId, Integer tableNumber, UUID excludeId) {
        boolean exists = excludeId != null
                ? tableRepository.existsByVenueIdAndTableNumberAndActiveTrueAndIdNot(venueId, tableNumber, excludeId)
                : tableRepository.existsByVenueIdAndTableNumberAndActiveTrue(venueId, tableNumber);
        if (exists)
            throw new TableBusinessException("Ya existe una mesa activa con el número " + tableNumber + " en la sede indicada");
    }

    private TableStatus parseStatus(String status) {
        if (!isNotBlank(status)) return null;
        try {
            return TableStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new TableValidationException("Estado de mesa inválido. Valores aceptados: AVAILABLE, OCCUPIED");
        }
    }

    private UUID parseUUID(String value, String fieldName) {
        validateRequiredText(value, fieldName);
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new TableValidationException("El campo '" + fieldName + "' tiene un formato de UUID inválido");
        }
    }


    private LocalDateTime buildReservationDateTime(java.time.LocalDate date, java.time.LocalTime time) {
        return LocalDateTime.of(date, time);
    }

    private LocalDateTime reservationDateTime(TableReservationDomain reservation) {
        return LocalDateTime.of(reservation.getReservationDate(), reservation.getReservationTime());
    }

    private LocalDateTime lockStart(TableReservationDomain reservation) {
        return reservationDateTime(reservation).minusHours(RESERVATION_LOCK_HOURS);
    }

    private LocalDateTime graceEnd(TableReservationDomain reservation) {
        return reservationDateTime(reservation).plusMinutes(RESERVATION_GRACE_MINUTES);
    }

    private String buildBlockedTableMessage(TableDomain table, TableReservationDomain reservation) {
        return "La mesa " + table.getTableNumber() +
                " tiene una reserva pendiente para " + formatReservationDateTime(reservation) +
                " y no puede abrirse para terceros desde " + formatDateTime(lockStart(reservation)) +
                " hasta " + formatDateTime(graceEnd(reservation));
    }

    private String formatReservationDateTime(TableReservationDomain reservation) {
        return formatDateTime(reservationDateTime(reservation));
    }

    private String formatDateTime(LocalDateTime value) {
        return value.format(DATE_TIME_FORMATTER);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }

    private String normalizeText(String value) {
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Boolean normalizeBoolean(Boolean value) {
        return value != null ? value : Boolean.FALSE;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private void validateRequiredText(String value, String fieldName) {
        if (!isNotBlank(value))
            throw new TableValidationException("El campo '" + fieldName + "' es requerido y no puede estar vacío");
    }

    private void validateRequiredObject(Object value, String fieldName) {
        if (value == null)
            throw new TableValidationException("El campo '" + fieldName + "' es requerido y no puede estar vacío");
    }

    private String formatDuration(long totalMinutes) {
        if (totalMinutes < 60) return totalMinutes + " minuto" + (totalMinutes != 1 ? "s" : "");
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (minutes == 0) return hours + " hora" + (hours != 1 ? "s" : "");
        return hours + " hora" + (hours != 1 ? "s" : "") + " " + minutes + " minuto" + (minutes != 1 ? "s" : "");
    }
}
