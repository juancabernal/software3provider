package com.co.eatupapi.services.payment.invoice;

import com.co.eatupapi.domain.payment.invoice.InvoiceNumberSequence;
import com.co.eatupapi.repositories.payment.invoice.InvoiceNumberSequenceRepository;
import com.co.eatupapi.utils.payment.invoice.exceptions.InvoiceBusinessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class InvoiceNumberGenerator {

    private static final int SEQUENCE_ID = 1;
    private static final String PREFIX = "FAC-";
    private static final int NUMBER_WIDTH = 6;

    private final InvoiceNumberSequenceRepository sequenceRepository;

    public InvoiceNumberGenerator(InvoiceNumberSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Transactional
    public String nextInvoiceNumber() {
        long nextValue = reserveNextValue();
        return PREFIX + String.format(Locale.ROOT, "%0" + NUMBER_WIDTH + "d", nextValue);
    }

    private long reserveNextValue() {
        sequenceRepository.ensureSequenceRow();
        InvoiceNumberSequence sequence = sequenceRepository.findByIdForUpdate(SEQUENCE_ID)
                .orElseThrow(() -> new InvoiceBusinessException("Failed to lock invoice sequence"));

        long currentValue = sequence.getNextValue();
        sequence.setNextValue(currentValue + 1);
        sequenceRepository.save(sequence);

        return currentValue;
    }
}
