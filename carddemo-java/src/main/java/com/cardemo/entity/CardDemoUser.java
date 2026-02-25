package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Migrated from VSAM USRSEC / Copybook CSUSR01Y (80-byte FB records).
 * COBOL: SEC-USER-DATA
 */
@Entity
@Table(name = "users")
public class CardDemoUser {

    /** SEC-USR-ID PIC X(08) */
    @Id
    @Column(name = "usr_id", length = 8, nullable = false)
    private String usrId;

    /** SEC-USR-FNAME PIC X(20) */
    @Column(name = "usr_fname", length = 20)
    private String usrFname;

    /** SEC-USR-LNAME PIC X(20) */
    @Column(name = "usr_lname", length = 20)
    private String usrLname;

    /** SEC-USR-PWD PIC X(08) */
    @Column(name = "usr_pwd", length = 8)
    private String usrPwd;

    /**
     * SEC-USR-TYPE PIC X(01)
     * 'A' = Admin, 'R' = Regular user
     */
    @Column(name = "usr_type", length = 1)
    private String usrType;

    public CardDemoUser() {
    }

    public String getUsrId() {
        return usrId;
    }

    public void setUsrId(String usrId) {
        this.usrId = usrId;
    }

    public String getUsrFname() {
        return usrFname;
    }

    public void setUsrFname(String usrFname) {
        this.usrFname = usrFname;
    }

    public String getUsrLname() {
        return usrLname;
    }

    public void setUsrLname(String usrLname) {
        this.usrLname = usrLname;
    }

    public String getUsrPwd() {
        return usrPwd;
    }

    public void setUsrPwd(String usrPwd) {
        this.usrPwd = usrPwd;
    }

    public String getUsrType() {
        return usrType;
    }

    public void setUsrType(String usrType) {
        this.usrType = usrType;
    }

    public boolean isAdmin() {
        return "A".equalsIgnoreCase(usrType);
    }
}
