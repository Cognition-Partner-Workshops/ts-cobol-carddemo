package com.aws.carddemo.repository;

import com.aws.carddemo.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, Integer> {

    Optional<TransactionType> findByTypeCode(String typeCode);

    List<TransactionType> findByActive(Boolean active);

    List<TransactionType> findByCategoryCategoryId(Integer categoryId);

    @Query("SELECT t FROM TransactionType t JOIN FETCH t.category WHERE t.typeCode = :typeCode")
    Optional<TransactionType> findByTypeCodeWithCategory(@Param("typeCode") String typeCode);

    @Query("SELECT t FROM TransactionType t WHERE t.active = true ORDER BY t.typeCode")
    List<TransactionType> findAllActiveTypes();

    boolean existsByTypeCode(String typeCode);
}
