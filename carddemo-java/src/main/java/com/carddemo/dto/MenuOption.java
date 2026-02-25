package com.carddemo.dto;

/**
 * Menu option DTO - replaces COMEN02Y copybook menu option structure.
 */
public class MenuOption {

    private int optionNum;
    private String optionName;
    private String programName;
    private String userType;
    private String url;

    public MenuOption() {
    }

    public MenuOption(int optionNum, String optionName, String programName, String userType, String url) {
        this.optionNum = optionNum;
        this.optionName = optionName;
        this.programName = programName;
        this.userType = userType;
        this.url = url;
    }

    public int getOptionNum() { return optionNum; }
    public void setOptionNum(int optionNum) { this.optionNum = optionNum; }
    public String getOptionName() { return optionName; }
    public void setOptionName(String optionName) { this.optionName = optionName; }
    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public boolean isAdminOnly() {
        return "A".equals(userType);
    }
}
