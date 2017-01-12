package com.attvn.adtruevn.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by app on 12/27/16.
 */

public class AdvertInfo extends RealmObject {

    @PrimaryKey
    private long ads_id;

    private int type;
    private int start;
    private String content;
    private int duration;
    private int position;

    private String file_path;

    public AdvertInfo() {
    }

    public long getAds_id() {
        return ads_id;
    }

    public void setAds_id(long ads_id) {
        this.ads_id = ads_id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getFile_path() {
        return file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }

    public final static class TypeAds {
        public final static int TYPE_IMAGE = 1;
        public final static int TYPE_TEXT_SCROLL = 2;
        public final static int TYPE_VIDEO = 3;
    }

    public final static class AdsPosition {
        public final static int TOP = 1;
        public final static int BOTTOM = 2;
        public final static int LEFT = 3;
        public final static int RIGHT = 4;
        public final static int POPUP = 5;
    }
}
