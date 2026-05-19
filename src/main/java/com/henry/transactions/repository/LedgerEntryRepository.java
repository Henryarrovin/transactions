package com.henry.transactions.repository;

import com.henry.transactions.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByUserIdOrderByCreatedAtDesc(String userId);

    // Get the latest balance for a user
    @Query("""
            SELECT l.balance FROM LedgerEntry l
            WHERE l.userId = :userId
            ORDER BY l.createdAt DESC
            LIMIT 1
            """)
    Optional<Long> findLatestBalanceByUserId(@Param("userId") String userId);

}
