package com.henry.transactions.repository;

import com.henry.transactions.entity.Transaction;
import com.henry.transactions.enums.TransactionStatus;
import com.henry.transactions.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByProviderPaymentId(String providerPaymentId);

    Optional<Transaction> findByReferenceId(String referenceId);

    Page<Transaction> findByUserId(String userId, Pageable pageable);

    Page<Transaction> findByUserIdAndType(String userId, TransactionType type, Pageable pageable);

    Page<Transaction> findByUserIdAndStatus(String userId, TransactionStatus status, Pageable pageable);

    Page<Transaction> findByUserIdAndTypeAndStatus(
            String userId,
            TransactionType type,
            TransactionStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.userId = :userId
            AND t.createdAt BETWEEN :start AND :end
            ORDER BY t.createdAt ASC
            """)
    Page<Transaction> findByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.userId = :userId
            AND (:type IS NULL OR t.type = :type)
            AND (:status IS NULL OR t.status = :status)
            AND (:start IS NULL OR t.createdAt >= :start)
            AND (:end IS NULL OR t.createdAt <= :end)
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findWithFilters(
            @Param("userId") String userId,
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    // For statement generation
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.userId = :userId
            AND t.type = 'payment'
            AND t.status = 'completed'
            AND t.createdAt BETWEEN :start AND :end
            """)
    Long sumPaymentsByUserAndDateRange(
            @Param("userId") String userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.userId = :userId
            AND t.type = 'refund'
            AND t.status = 'completed'
            AND t.createdAt BETWEEN :start AND :end
            """)
    Long sumRefundsByUserAndDateRange(
            @Param("userId") String userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            SELECT COUNT(t) FROM Transaction t
            WHERE t.userId = :userId
            AND t.createdAt BETWEEN :start AND :end
            """)
    Long countByUserAndDateRange(
            @Param("userId") String userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}
