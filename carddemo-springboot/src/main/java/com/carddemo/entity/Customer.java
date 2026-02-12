package com.carddemo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Replaces VSAM KSDS file: CUSTFILE
 * Maps to COBOL copybook: CVCUS01Y (CUSTOMER-RECORD)
 *
 * Fields will be populated during migration from the mainframe data structures.
 */
@Entity
@Table(name = "customers")
public class Customer {

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
