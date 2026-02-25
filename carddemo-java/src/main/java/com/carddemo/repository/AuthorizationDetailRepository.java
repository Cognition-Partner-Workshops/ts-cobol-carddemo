package com.carddemo.repository;

import com.carddemo.entity.AuthorizationDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorizationDetailRepository extends JpaRepository<AuthorizationDetail, Long> {

    List<AuthorizationDetail> findBySummaryId(Long summaryId);

    Page<AuthorizationDetail> findBySummaryId(Long summaryId, Pageable pageable);

    List<AuthorizationDetail> findByCardNum(String cardNum);

    List<AuthorizationDetail> findByMatchStatus(String matchStatus);

    List<AuthorizationDetail> findBySummaryIdAndMatchStatus(Long summaryId, String matchStatus);
}
