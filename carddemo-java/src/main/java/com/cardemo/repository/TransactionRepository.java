package com.cardemo.repository;

import com.cardemo.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByTranCardNum(String tranCardNum);
    Page<Transaction> findByTranCardNum(String tranCardNum, Pageable pageable);
    List<Transaction> findByTranCardNumOrderByTranOrigTsDesc(String tranCardNum);
}
