package com.pytorch.project.gazeguard.parentdashboard;

public class ParentModel {

    String ScreenTime, name, surl;

    ParentModel() {

    }
    public ParentModel(String name, String ScreenTime, String surl) {
        this.name = name;
        this.ScreenTime = ScreenTime;
        this.surl = surl;
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

}