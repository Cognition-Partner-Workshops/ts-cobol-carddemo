package com.aws.carddemo.repository;

import com.aws.carddemo.model.DisclosureGroup;
import com.aws.carddemo.model.DisclosureGroup.DisclosureGroupKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisclosureGroupRepository extends JpaRepository<DisclosureGroup, DisclosureGroupKey> {

    List<DisclosureGroup> findByIdDisAcctGroupId(String acctGroupId);
}
