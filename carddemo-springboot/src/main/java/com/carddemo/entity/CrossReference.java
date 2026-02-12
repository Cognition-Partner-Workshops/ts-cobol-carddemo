package com.carddemo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Replaces VSAM KSDS file: XREFFILE
 * Maps to COBOL copybook: CVACT03Y (CARD-XREF-RECORD)
 *
 * Stores the cross-reference mapping between cards and accounts.
 * Fields will be populated during migration from the mainframe data structures.
 */
@Entity
@Table(name = "cross_references")
public class CrossReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
