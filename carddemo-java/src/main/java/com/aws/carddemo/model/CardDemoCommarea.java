package com.aws.carddemo.model;

/**
 * CardDemo Communication Area - migrated from COBOL copybook COCOM01Y.cpy
 * Used for passing data between programs/transactions in the original CICS application
 * In Java, this serves as a session/context object
 */
public class CardDemoCommarea {

    private String cdemoFromTranid;
    private String cdemoFromProgram;
    private String cdemoToTranid;
    private String cdemoToProgram;
    private String cdemoUserId;
    private String cdemoUserType;
    private Integer cdemoPgmContext;

    private Long cdemoCustId;
    private String cdemoCustFname;
    private String cdemoCustMname;
    private String cdemoCustLname;

    private Long cdemoAcctId;
    private String cdemoAcctStatus;

    private Long cdemoCardNum;

    private String cdemoLastMap;
    private String cdemoLastMapset;

    public static final String USER_TYPE_ADMIN = "A";
    public static final String USER_TYPE_USER = "U";

    public static final int PGM_CONTEXT_ENTER = 0;
    public static final int PGM_CONTEXT_REENTER = 1;

    public CardDemoCommarea() {
    }

    public String getCdemoFromTranid() {
        return cdemoFromTranid;
    }

    public void setCdemoFromTranid(String cdemoFromTranid) {
        this.cdemoFromTranid = cdemoFromTranid;
    }

    public String getCdemoFromProgram() {
        return cdemoFromProgram;
    }

    public void setCdemoFromProgram(String cdemoFromProgram) {
        this.cdemoFromProgram = cdemoFromProgram;
    }

    public String getCdemoToTranid() {
        return cdemoToTranid;
    }

    public void setCdemoToTranid(String cdemoToTranid) {
        this.cdemoToTranid = cdemoToTranid;
    }

    public String getCdemoToProgram() {
        return cdemoToProgram;
    }

    public void setCdemoToProgram(String cdemoToProgram) {
        this.cdemoToProgram = cdemoToProgram;
    }

    public String getCdemoUserId() {
        return cdemoUserId;
    }

    public void setCdemoUserId(String cdemoUserId) {
        this.cdemoUserId = cdemoUserId;
    }

    public String getCdemoUserType() {
        return cdemoUserType;
    }

    public void setCdemoUserType(String cdemoUserType) {
        this.cdemoUserType = cdemoUserType;
    }

    public boolean isAdmin() {
        return USER_TYPE_ADMIN.equals(cdemoUserType);
    }

    public boolean isUser() {
        return USER_TYPE_USER.equals(cdemoUserType);
    }

    public Integer getCdemoPgmContext() {
        return cdemoPgmContext;
    }

    public void setCdemoPgmContext(Integer cdemoPgmContext) {
        this.cdemoPgmContext = cdemoPgmContext;
    }

    public Long getCdemoCustId() {
        return cdemoCustId;
    }

    public void setCdemoCustId(Long cdemoCustId) {
        this.cdemoCustId = cdemoCustId;
    }

    public String getCdemoCustFname() {
        return cdemoCustFname;
    }

    public void setCdemoCustFname(String cdemoCustFname) {
        this.cdemoCustFname = cdemoCustFname;
    }

    public String getCdemoCustMname() {
        return cdemoCustMname;
    }

    public void setCdemoCustMname(String cdemoCustMname) {
        this.cdemoCustMname = cdemoCustMname;
    }

    public String getCdemoCustLname() {
        return cdemoCustLname;
    }

    public void setCdemoCustLname(String cdemoCustLname) {
        this.cdemoCustLname = cdemoCustLname;
    }

    public Long getCdemoAcctId() {
        return cdemoAcctId;
    }

    public void setCdemoAcctId(Long cdemoAcctId) {
        this.cdemoAcctId = cdemoAcctId;
    }

    public String getCdemoAcctStatus() {
        return cdemoAcctStatus;
    }

    public void setCdemoAcctStatus(String cdemoAcctStatus) {
        this.cdemoAcctStatus = cdemoAcctStatus;
    }

    public Long getCdemoCardNum() {
        return cdemoCardNum;
    }

    public void setCdemoCardNum(Long cdemoCardNum) {
        this.cdemoCardNum = cdemoCardNum;
    }

    public String getCdemoLastMap() {
        return cdemoLastMap;
    }

    public void setCdemoLastMap(String cdemoLastMap) {
        this.cdemoLastMap = cdemoLastMap;
    }

    public String getCdemoLastMapset() {
        return cdemoLastMapset;
    }

    public void setCdemoLastMapset(String cdemoLastMapset) {
        this.cdemoLastMapset = cdemoLastMapset;
    }

    @Override
    public String toString() {
        return "CardDemoCommarea{" +
                "cdemoFromTranid='" + cdemoFromTranid + '\'' +
                ", cdemoFromProgram='" + cdemoFromProgram + '\'' +
                ", cdemoToTranid='" + cdemoToTranid + '\'' +
                ", cdemoToProgram='" + cdemoToProgram + '\'' +
                ", cdemoUserId='" + cdemoUserId + '\'' +
                ", cdemoUserType='" + cdemoUserType + '\'' +
                ", cdemoPgmContext=" + cdemoPgmContext +
                ", cdemoCustId=" + cdemoCustId +
                ", cdemoCustFname='" + cdemoCustFname + '\'' +
                ", cdemoCustMname='" + cdemoCustMname + '\'' +
                ", cdemoCustLname='" + cdemoCustLname + '\'' +
                ", cdemoAcctId=" + cdemoAcctId +
                ", cdemoAcctStatus='" + cdemoAcctStatus + '\'' +
                ", cdemoCardNum=" + cdemoCardNum +
                ", cdemoLastMap='" + cdemoLastMap + '\'' +
                ", cdemoLastMapset='" + cdemoLastMapset + '\'' +
                '}';
    }
}
