package com.co.eatupapi.repositories.payment.invoice;

import com.co.eatupapi.domain.payment.invoice.InvoiceNumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceNumberSequenceRepository extends JpaRepository<InvoiceNumberSequence, Integer> {

    @Modifying
    @Query(
            value = "insert into invoice_number_sequences(id, next_value) values (1, 1) on conflict (id) do nothing",
            nativeQuery = true
    )
    void ensureSequenceRow();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select seq from InvoiceNumberSequence seq where seq.id = :id")
    Optional<InvoiceNumberSequence> findByIdForUpdate(@Param("id") Integer id);
}
