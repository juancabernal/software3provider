package com.co.eatupapi.repositories.inventory.transfer;

import com.co.eatupapi.domain.inventory.transfer.TransferStatus;
import com.co.eatupapi.domain.inventory.transfer.Transfer;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    List<Transfer> findByEstado(TransferStatus estado);
    List<Transfer> findBySedeDestino(String sedeDestino);

    @Modifying
    @Query("""
            update Transfer t
               set t.estado = :nextStatus,
                   t.fechaEnvio = :actualDepartureTime,
                   t.updatedAt = :actualDepartureTime
             where t.estado = :currentStatus
               and t.fechaEnvio <= :departureTime
            """)
    int moveToTransitWhenDepartureTimeArrives(@Param("currentStatus") TransferStatus currentStatus,
                                              @Param("nextStatus") TransferStatus nextStatus,
                                              @Param("departureTime") LocalDateTime departureTime,
                                              @Param("actualDepartureTime") LocalDateTime actualDepartureTime);
}
