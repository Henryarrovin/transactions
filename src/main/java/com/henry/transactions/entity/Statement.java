package com.henry.transactions.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "statements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "month", "year"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private Integer month; // 1-12

    @Column(nullable = false)
    private Integer year;

    @Column(name = "opening_balance", nullable = false)
    @Builder.Default
    private Long openingBalance = 0L;

    @Column(name = "closing_balance", nullable = false)
    @Builder.Default
    private Long closingBalance = 0L;

    @Column(name = "total_credits", nullable = false)
    @Builder.Default
    private Long totalCredits = 0L;

    @Column(name = "total_debits", nullable = false)
    @Builder.Default
    private Long totalDebits = 0L;

    @Column(name = "transaction_count", nullable = false)
    @Builder.Default
    private Integer transactionCount = 0;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
