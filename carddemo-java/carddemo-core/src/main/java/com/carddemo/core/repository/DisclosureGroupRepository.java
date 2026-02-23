package com.carddemo.core.repository;

import com.carddemo.core.domain.DisclosureGroup;
import com.carddemo.core.domain.DisclosureGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DisclosureGroup entity.
 * Replaces VSAM operations on DISCGRP file.
 * Used by interest calculation batch job (CBACT04C).
 */
@Repository
public interface DisclosureGroupRepository
        extends JpaRepository<DisclosureGroup, DisclosureGroupId> {

    List<DisclosureGroup> findByAcctGroupId(String acctGroupId);
}
