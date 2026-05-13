package com.co.eatupapi.repositories.commercial.table;

import com.co.eatupapi.domain.commercial.table.TableSessionDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TableSessionRepository extends JpaRepository<TableSessionDomain, UUID> {

    Optional<TableSessionDomain> findByTableIdAndClosedAtIsNull(UUID tableId);

    List<TableSessionDomain> findAllByTableId(UUID tableId);

    List<TableSessionDomain> findAllByOrderByOpenedAtDesc();

    List<TableSessionDomain> findAllByTableIdOrderByOpenedAtDesc(UUID tableId);
}
