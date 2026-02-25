package com.cardemo.repository;

import com.cardemo.entity.CardAccountXref;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardAccountXrefRepository extends JpaRepository<CardAccountXref, String> {
    List<CardAccountXref> findByXrefAcctId(Long xrefAcctId);
    Optional<CardAccountXref> findFirstByXrefAcctId(Long xrefAcctId);
}
