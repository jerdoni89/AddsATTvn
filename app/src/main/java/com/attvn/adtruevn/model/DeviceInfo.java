package com.attvn.adtruevn.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by app on 12/21/16.
 */

public class DeviceInfo extends RealmObject {
    @PrimaryKey
    private String device_id;

    private String mac;
    private String token;

    public DeviceInfo() {
    }

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return mac;
    }
}
