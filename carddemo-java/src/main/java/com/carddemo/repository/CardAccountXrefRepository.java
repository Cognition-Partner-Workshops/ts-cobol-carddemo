package com.carddemo.repository;

import com.carddemo.entity.CardAccountXref;
import com.carddemo.entity.CardAccountXrefId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardAccountXrefRepository extends JpaRepository<CardAccountXref, CardAccountXrefId> {

    List<CardAccountXref> findByAcctId(Long acctId);

    List<CardAccountXref> findByCustId(Long custId);

    List<CardAccountXref> findByCardNum(String cardNum);
}
