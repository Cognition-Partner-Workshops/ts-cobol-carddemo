package com.aws.carddemo.repository;

import com.aws.carddemo.entity.DisclosureGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DisclosureGroupRepository extends JpaRepository<DisclosureGroup, Integer> {

    Optional<DisclosureGroup> findByGroupCode(String groupCode);

    List<DisclosureGroup> findByActive(Boolean active);

    @Query("SELECT d FROM DisclosureGroup d WHERE d.active = true AND d.effectiveDate <= :date AND (d.expirationDate IS NULL OR d.expirationDate >= :date)")
    List<DisclosureGroup> findEffectiveGroups(@Param("date") LocalDate date);

    @Query("SELECT d FROM DisclosureGroup d WHERE d.expirationDate BETWEEN :startDate AND :endDate")
    List<DisclosureGroup> findExpiringGroups(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    boolean existsByGroupCode(String groupCode);
}
