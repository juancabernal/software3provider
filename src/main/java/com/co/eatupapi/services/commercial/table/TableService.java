package com.co.eatupapi.services.commercial.table;

import com.co.eatupapi.dto.commercial.table.TableDTO;
import com.co.eatupapi.dto.commercial.table.TableReservationDTO;
import com.co.eatupapi.dto.commercial.table.TableSessionDTO;
import com.co.eatupapi.dto.commercial.table.TableSummaryDTO;

import java.util.List;

public interface TableService {

    TableDTO createTable(TableDTO request);
    List<TableDTO> getTables(String status, String venueId, Boolean reserved, Boolean canOpenNow);
    TableDTO getTableById(String tableId);
    List<TableDTO> getTablesByVenue(String venueId);
    TableDTO updateTable(String tableId, TableDTO request);
    void deactivateTable(String tableId);

    TableSessionDTO openSession(String tableId, TableSessionDTO request);
    List<TableSessionDTO> getAllSessions();
    TableSessionDTO getActiveSession(String tableId);
    List<TableSessionDTO> getSessions(String tableId);
    TableSessionDTO updateGuestCount(String tableId, String sessionId, Integer guestCount);
    TableSessionDTO closeSession(String tableId, String sessionId);
    List<TableSessionDTO> getSessionHistory(String tableId);

    TableReservationDTO createReservation(String tableId, TableReservationDTO request);
    List<TableReservationDTO> getAllActiveReservations();
    TableReservationDTO getActiveReservation(String tableId);
    List<TableReservationDTO> getReservations(String tableId);
    TableReservationDTO updateReservation(String tableId, String reservationId, TableReservationDTO request);
    void cancelReservation(String tableId, String reservationId);
    List<TableReservationDTO> searchReservationsByGuestDocumentNumber(String guestDocumentNumber);
    TableSessionDTO seatReservation(String reservationId, TableSessionDTO request);

    TableSummaryDTO getSummary();
    TableSummaryDTO getSummaryByVenue(String venueId);
}
