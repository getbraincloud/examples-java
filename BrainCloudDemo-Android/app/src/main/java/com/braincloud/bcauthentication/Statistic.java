package com.braincloud.bcauthentication;

public class Statistic {

    private String name;
    private long value;

    public void setName(String name) {
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }
}
