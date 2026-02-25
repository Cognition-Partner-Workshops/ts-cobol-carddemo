package com.cardemo.repository;

import com.cardemo.entity.CardDemoUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardDemoUserRepository extends JpaRepository<CardDemoUser, String> {
}
