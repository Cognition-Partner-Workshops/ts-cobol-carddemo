package com.carddemo.repository;

import com.carddemo.entity.FraudRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudRecordRepository extends JpaRepository<FraudRecord, Long> {

    List<FraudRecord> findByCardNum(String cardNum);

    List<FraudRecord> findByAcctId(Long acctId);
}
