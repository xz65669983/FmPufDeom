package com.fm.zhangzhengchao.fmpufdeom.model;

/**
 * Created by zhangzhengchao on 2017/8/26.
 */

public class Data {
    private String uid;
    private String ramData;
    private String ramRandom;
    private String authData;
    private String authRandom;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getRamData() {
        return ramData;
    }

    public void setRamData(String ramData) {
        this.ramData = ramData;
    }

    public String getRamRandom() {
        return ramRandom;
    }

    public void setRamRandom(String ramRandom) {
        this.ramRandom = ramRandom;
    }

    public String getAuthData() {
        return authData;
    }

    public void setAuthData(String authData) {
        this.authData = authData;
    }

    public String getAuthRandom() {
        return authRandom;
    }

    public void setAuthRandom(String authRandom) {
        this.authRandom = authRandom;
    }
}
