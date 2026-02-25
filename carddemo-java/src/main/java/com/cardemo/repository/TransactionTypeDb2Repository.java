package com.cardemo.repository;

import com.cardemo.entity.TransactionTypeDb2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionTypeDb2Repository extends JpaRepository<TransactionTypeDb2, String> {
}
