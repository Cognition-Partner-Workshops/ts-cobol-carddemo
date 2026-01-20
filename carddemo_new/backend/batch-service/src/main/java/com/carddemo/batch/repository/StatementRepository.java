package com.carddemo.batch.repository;

import com.carddemo.batch.entity.Statement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StatementRepository extends JpaRepository<Statement, Long> {
    List<Statement> findByAccountId(String accountId);
    
    List<Statement> findByCustomerId(String customerId);
    
    Optional<Statement> findByStatementId(String statementId);
    
    @Query("SELECT s FROM Statement s WHERE s.accountId = :accountId ORDER BY s.statementDate DESC")
    List<Statement> findByAccountIdOrderByDateDesc(@Param("accountId") String accountId);
    
    @Query("SELECT s FROM Statement s WHERE s.statementDate = :date")
    List<Statement> findByStatementDate(@Param("date") LocalDate date);
}
