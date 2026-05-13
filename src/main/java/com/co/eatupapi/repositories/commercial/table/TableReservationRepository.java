package com.co.eatupapi.repositories.commercial.table;

import com.co.eatupapi.domain.commercial.table.ReservationStatus;
import com.co.eatupapi.domain.commercial.table.TableReservationDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TableReservationRepository extends JpaRepository<TableReservationDomain, UUID> {

    List<TableReservationDomain> findAllByTableIdOrderByReservationDateAscReservationTimeAsc(UUID tableId);

    List<TableReservationDomain> findAllByStatusInOrderByReservationDateAscReservationTimeAsc(
            Collection<ReservationStatus> statuses
    );

    List<TableReservationDomain> findAllByTableIdAndStatusInOrderByReservationDateAscReservationTimeAsc(
            UUID tableId,
            Collection<ReservationStatus> statuses
    );

    List<TableReservationDomain> findAllByGuestDocumentNumberAndStatusInOrderByReservationDateAscReservationTimeAsc(
            String guestDocumentNumber,
            Collection<ReservationStatus> statuses
    );
}
