package com.cardemo.repository;

import com.cardemo.entity.AuthDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuthDetailRepository extends JpaRepository<AuthDetail, Long> {
    List<AuthDetail> findByAuthSummaryId(Long authSummaryId);
    List<AuthDetail> findByAuthSummaryIdOrderByAuthTsDesc(Long authSummaryId);
}
