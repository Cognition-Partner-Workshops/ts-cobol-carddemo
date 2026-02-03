package com.aws.carddemo.repository;

import com.aws.carddemo.model.RejectRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RejectRecordRepository extends JpaRepository<RejectRecord, Long> {

    List<RejectRecord> findByValidationFailReason(Integer reason);
}
