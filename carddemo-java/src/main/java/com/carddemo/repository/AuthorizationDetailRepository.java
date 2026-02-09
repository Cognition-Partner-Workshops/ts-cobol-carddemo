package com.carddemo.repository;

import com.carddemo.entity.AuthorizationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorizationDetailRepository extends JpaRepository<AuthorizationDetail, Long> {

    List<AuthorizationDetail> findByAuthIdOrderByAuthDateDescAuthTimeDesc(Long authId);
}
