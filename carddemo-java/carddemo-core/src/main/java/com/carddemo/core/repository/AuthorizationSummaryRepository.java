package com.carddemo.core.repository;

import com.carddemo.core.domain.AuthorizationSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for AuthorizationSummary entity.
 * Replaces IMS DL/I calls to PAUTSUM0 segment in DBPAUTP0 database.
 */
@Repository
public interface AuthorizationSummaryRepository
        extends JpaRepository<AuthorizationSummary, Long> {
}
