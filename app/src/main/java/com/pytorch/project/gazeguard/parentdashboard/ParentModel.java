package com.pytorch.project.gazeguard.parentdashboard;

public class ParentModel {

    String ScreenTime, name, surl, deviceUnlockTime;
    int screenTimeLimit;

    ParentModel() {
    }
    public ParentModel(String name, String ScreenTime, String surl, int screenTimeLimit) {
        this.name = name;
        this.ScreenTime = ScreenTime;
        this.surl = surl;
        this.screenTimeLimit = screenTimeLimit;
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
    public int getScreenTimeLimit() {
        return screenTimeLimit;
    }
    public void setScreenTimeLimit(int screenTimeLimit) {
        this.screenTimeLimit = screenTimeLimit;
    }

    // Set Limit Value
    public String getDeviceUnlockTime() {
        return deviceUnlockTime;
    }
    public void setDeviceUnlockTime(String deviceUnlockTime) {
        this.deviceUnlockTime = deviceUnlockTime;
    }

}