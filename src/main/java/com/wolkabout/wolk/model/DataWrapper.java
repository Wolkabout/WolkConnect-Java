package com.wolkabout.wolk.model;

public class DataWrapper {

    private String data;

    public DataWrapper() {
    }

    public DataWrapper(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "DataWrapper{" +
                "data='" + data + '\'' +
                '}';
    }
}
