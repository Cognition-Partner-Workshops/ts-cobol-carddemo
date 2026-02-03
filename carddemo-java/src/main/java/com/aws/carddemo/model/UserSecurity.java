package com.aws.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * User Security entity - migrated from COBOL copybook CSUSR01Y.cpy
 * Original COBOL record length: 80 bytes
 * Used for user authentication and authorization
 */
@Entity
@Table(name = "USRSEC")
public class UserSecurity {

    @Id
    @Column(name = "SEC_USR_ID", length = 8)
    private String secUsrId;

    @Column(name = "SEC_USR_FNAME", length = 20)
    private String secUsrFname;

    @Column(name = "SEC_USR_LNAME", length = 20)
    private String secUsrLname;

    @Column(name = "SEC_USR_PWD", length = 8)
    private String secUsrPwd;

    @Column(name = "SEC_USR_TYPE", length = 1)
    private String secUsrType;

    public UserSecurity() {
    }

    public UserSecurity(String secUsrId) {
        this.secUsrId = secUsrId;
    }

    public String getSecUsrId() {
        return secUsrId;
    }

    public void setSecUsrId(String secUsrId) {
        this.secUsrId = secUsrId;
    }

    public String getSecUsrFname() {
        return secUsrFname;
    }

    public void setSecUsrFname(String secUsrFname) {
        this.secUsrFname = secUsrFname;
    }

    public String getSecUsrLname() {
        return secUsrLname;
    }

    public void setSecUsrLname(String secUsrLname) {
        this.secUsrLname = secUsrLname;
    }

    public String getSecUsrPwd() {
        return secUsrPwd;
    }

    public void setSecUsrPwd(String secUsrPwd) {
        this.secUsrPwd = secUsrPwd;
    }

    public String getSecUsrType() {
        return secUsrType;
    }

    public void setSecUsrType(String secUsrType) {
        this.secUsrType = secUsrType;
    }

    public boolean isAdmin() {
        return "A".equals(secUsrType);
    }

    public boolean isUser() {
        return "U".equals(secUsrType);
    }

    @Override
    public String toString() {
        return "UserSecurity{" +
                "secUsrId='" + secUsrId + '\'' +
                ", secUsrFname='" + secUsrFname + '\'' +
                ", secUsrLname='" + secUsrLname + '\'' +
                ", secUsrType='" + secUsrType + '\'' +
                '}';
    }
}
