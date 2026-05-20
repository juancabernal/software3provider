package com.co.eatupapi.domain.payment.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoice_number_sequences")
public class InvoiceNumberSequence {

    @Id
    private Integer id;

    @Column(name = "next_value", nullable = false)
    private Long nextValue;
}
