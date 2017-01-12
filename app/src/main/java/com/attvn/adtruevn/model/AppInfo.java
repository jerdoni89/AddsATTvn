package com.attvn.adtruevn.model;

import android.util.JsonReader;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by app on 12/30/16.
 */

public class AppInfo {

    private String filename;
    private int version_code;
    private String version_name;
    private long created;
    private long updated;

    public AppInfo() {
    }

    public AppInfo(String filename, int version_code, String version_name, long created, long updated) {
        this.filename = filename;
        this.version_code = version_code;
        this.version_name = version_name;
        this.created = created;
        this.updated = updated;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getVersion_code() {
        return version_code;
    }

    public void setVersion_code(int version_code) {
        this.version_code = version_code;
    }

    public String getVersion_name() {
        return version_name;
    }

    public void setVersion_name(String version_name) {
        this.version_name = version_name;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    public static AppInfo getAppInfo(JsonReader reader) throws java.io.IOException {
        AppInfo appInfo = new AppInfo();
        reader.beginObject();
        while(reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "filename":
                    appInfo.filename = reader.nextString();
                    break;
                case "version_code":
                    appInfo.version_code = reader.nextInt();
                    break;
                case "version_name":
                    appInfo.version_name = reader.nextString();
                    break;
                case "created":
                    appInfo.created = reader.nextLong();
                    break;
                case "updated":
                    appInfo.updated = reader.nextLong();
                    break;
            }
        }
        reader.endObject();
        return appInfo;
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        return "Thông tin ứng dụng được update: version " + version_name + " vào lúc " + String.valueOf(dateFormat.format(new Date()));
    }
}
