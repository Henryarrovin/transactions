package com.henry.transactions.entity;

import com.henry.transactions.enums.ReconciliationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "provider_payment_id", nullable = false)
    private String providerPaymentId;

    @Column(name = "provider_order_id")
    private String providerOrderId;

    @Column(name = "expected_amount", nullable = false)
    private Long expectedAmount;

    @Column(name = "actual_amount", nullable = false)
    private Long actualAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "reconciliation_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ReconciliationStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}
