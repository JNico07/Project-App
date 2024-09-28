package com.pytorch.project.gazeguard.parentdashboard;

public class ParentModel {

    String ScreenTime, name, surl;
    int limitValue;
    ParentModel() {

    }
    public ParentModel(String name, String ScreenTime, String surl, int limitValue) {
        this.name = name;
        this.ScreenTime = ScreenTime;
        this.surl = surl;
        this.limitValue = limitValue;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getScreenTime() {
        return ScreenTime;
    }
    public void setScreenTime(String ScreenTime) {
        this.ScreenTime = ScreenTime;
    }

    public String getSurl() {
        return surl;
    }
    public void setSurl(String surl) {
        this.surl = surl;
    }

    // Set Limit Value
    public int getLimitValue() {
        return limitValue;
    }
    public void setLimitValue(int limitValue) {
        this.limitValue = limitValue;
    }


}