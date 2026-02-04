package com.aws.carddemo.domain.repository;

import com.aws.carddemo.domain.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, String> {

    List<TransactionType> findByActive(Boolean active);

    List<TransactionType> findByActiveTrue();

    Page<TransactionType> findByActive(Boolean active, Pageable pageable);

    List<TransactionType> findByCategoryCode(Integer categoryCode);

    @Query("SELECT tt FROM TransactionType tt WHERE LOWER(tt.typeDescription) LIKE LOWER(CONCAT('%', :description, '%'))")
    List<TransactionType> findByDescriptionContaining(@Param("description") String description);

    @Query("SELECT tt FROM TransactionType tt WHERE LOWER(tt.typeDescription) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<TransactionType> searchByDescription(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT tt.categoryCode FROM TransactionType tt WHERE tt.active = true")
    List<Integer> findDistinctActiveCategoryCodes();

    boolean existsByTypeCode(String typeCode);
}
