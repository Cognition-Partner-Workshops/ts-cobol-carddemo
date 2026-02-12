package com.carddemo.repository;

import com.carddemo.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for Card entities.
 * Replaces VSAM KSDS I/O operations on CARDFILE.
 */
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
}
