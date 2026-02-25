package com.cardemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user CRUD operations.
 * Migrated from COUSR01C/02C/03C (CU01/CU02/CU03 transactions).
 */
public class UserRequest {

    @NotBlank(message = "User ID is required")
    @Size(max = 8, message = "User ID must be at most 8 characters")
    private String usrId;

    @Size(max = 20)
    private String usrFname;

    @Size(max = 20)
    private String usrLname;

    @Size(max = 8)
    private String usrPwd;

    @Size(max = 1)
    private String usrType;

    public UserRequest() {
    }

    public String getUsrId() { return usrId; }
    public void setUsrId(String usrId) { this.usrId = usrId; }
    public String getUsrFname() { return usrFname; }
    public void setUsrFname(String usrFname) { this.usrFname = usrFname; }
    public String getUsrLname() { return usrLname; }
    public void setUsrLname(String usrLname) { this.usrLname = usrLname; }
    public String getUsrPwd() { return usrPwd; }
    public void setUsrPwd(String usrPwd) { this.usrPwd = usrPwd; }
    public String getUsrType() { return usrType; }
    public void setUsrType(String usrType) { this.usrType = usrType; }
}
