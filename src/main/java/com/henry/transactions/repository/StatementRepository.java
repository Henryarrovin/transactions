package com.henry.transactions.repository;

import com.henry.transactions.entity.Statement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatementRepository extends JpaRepository<Statement, UUID> {

    Optional<Statement> findByUserIdAndMonthAndYear(String userId, Integer month, Integer year);

}
