package com.aws.carddemo.transaction;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {

    List<TransactionRecord> findByCardCardNumber(String cardNumber);
}
