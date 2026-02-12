package com.carddemo.repository;

import com.carddemo.entity.CrossReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for CrossReference entities.
 * Replaces VSAM KSDS I/O operations on XREFFILE.
 */
@Repository
public interface CrossReferenceRepository extends JpaRepository<CrossReference, Long> {
}
