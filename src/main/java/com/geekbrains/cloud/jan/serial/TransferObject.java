package com.geekbrains.cloud.jan.serial;

import java.io.Serializable;

public class TransferObject implements Serializable {

    private String tag;
    private String message;

    public TransferObject() {
    }

    public TransferObject(String tag, String message) {
        this.tag = tag;
        this.message = message;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "TransferObject{" +
                "tag='" + tag + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
