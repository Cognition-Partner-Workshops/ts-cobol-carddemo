package com.carddemo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Replaces VSAM file: USRSEC (User Security File)
 * Maps to COBOL copybook: CSUSR01Y (USER-SECURITY-RECORD)
 *
 * Supports two roles: Regular User and Admin User.
 * Fields will be populated during migration from the mainframe data structures.
 */
@Entity
@Table(name = "users")
public class User {

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
