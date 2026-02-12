package com.aws.carddemo.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscountGroupRepository extends JpaRepository<DiscountGroup, String> {
}
