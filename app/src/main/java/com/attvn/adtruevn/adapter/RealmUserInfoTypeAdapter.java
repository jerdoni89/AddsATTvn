package com.attvn.adtruevn.adapter;

import com.attvn.adtruevn.model.DeviceInfo;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Created by app on 1/3/17.
 */

public class RealmUserInfoTypeAdapter extends TypeAdapter<DeviceInfo> {

    public static final TypeAdapter<DeviceInfo> INSTANCE =
            new RealmUserInfoTypeAdapter().nullSafe();

    private RealmUserInfoTypeAdapter() { }

    @Override
    public void write(JsonWriter out, DeviceInfo value) throws IOException {
        out.beginArray();
        out.value(value.getDevice_id());
        out.value(value.getMac());
        out.value(value.getToken());
        out.endArray();
    }

    @Override
    public DeviceInfo read(JsonReader in) throws IOException {
        DeviceInfo deviceInfo = new DeviceInfo();
        in.beginObject();

        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "device_id":
                    deviceInfo.setDevice_id(in.nextString());
                    break;
                case "mac":
                    deviceInfo.setMac(in.nextString());
                    break;
                case "token":
                    deviceInfo.setToken(in.nextString());
                    break;
            }
        }

        in.endObject();
        return deviceInfo;
    }
}
