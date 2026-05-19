package com.co.eatupapi.domain.payment.invoice;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "invoices",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_invoices_invoice_number_location",
                        columnNames = {"invoice_number", "location_id"}
                )
        }
)
public class Invoice {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "invoice_date", nullable = false)
    private LocalDateTime invoiceDate;

    @Column(name = "sales_id", nullable = false)
    private UUID salesId;

    @Column(name = "customer_discount_id")
    private UUID customerDiscountId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "discount_id")
    private UUID discountId;

    @Column(name = "table_id")
    private String tableId;

    @Column(name = "table_session_id")
    private String tableSessionId;

    @Column(name = "location_name", length = 150)
    private String locationName;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "discount_description", length = 255)
    private String discountDescription;

    @Column(name = "subtotal", precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_price", precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceDetail> details = new ArrayList<>();

    public void addDetail(InvoiceDetail detail) {
        details.add(detail);
        detail.setInvoice(this);
    }
}
