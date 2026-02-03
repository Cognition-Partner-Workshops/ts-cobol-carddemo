package com.aws.carddemo.repository;

import com.aws.carddemo.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    List<Card> findByCardAcctId(Long acctId);

    List<Card> findByCardActiveStatus(String status);

    List<Card> findByCardAcctIdAndCardActiveStatus(Long acctId, String status);
}
