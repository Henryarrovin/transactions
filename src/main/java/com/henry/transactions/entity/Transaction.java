package com.henry.transactions.entity;

import com.henry.transactions.enums.TransactionStatus;
import com.henry.transactions.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "transaction_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "transaction_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.pending;

    @Column(nullable = false)
    private Long amount; // in paise

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "provider_order_id")
    private String providerOrderId;

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    @Column(name = "provider_refund_id")
    private String providerRefundId;

    @Column(name = "reference_id")
    private String referenceId; // internal order/refund UUID from payment-gateway

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "JSONB")
    private String metadata;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LedgerEntry> ledgerEntries;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
