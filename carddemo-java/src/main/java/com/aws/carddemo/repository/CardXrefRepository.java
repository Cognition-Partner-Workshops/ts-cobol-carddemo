package com.aws.carddemo.repository;

import com.aws.carddemo.model.CardXref;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardXrefRepository extends JpaRepository<CardXref, String> {

    List<CardXref> findByXrefCustId(Long custId);

    List<CardXref> findByXrefAcctId(Long acctId);
}
