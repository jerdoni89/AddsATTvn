package com.attvn.adtruevn.model;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by app on 11/18/16.
 */

public class Video extends RealmObject {

    @PrimaryKey
    private  long id;

    private  int type;
    private String video_url;
    private String subtitles_url;
    private  long sortorder;
    private  long created;
    private  long modified;
    private  int active;
    private  String schedule;
    private  long start;
    private  long duration;
    private  long filesize;
    private RealmList<AdvertInfo> ads;

    // used for offline mode when user start offline service
    private String filePath;
    private String subtitlePath;

    public Video() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getVideo_url() {
        return video_url;
    }

    public void setVideo_url(String video_url) {
        this.video_url = video_url;
    }

    public String getSubtitles_url() {
        return subtitles_url;
    }

    public void setSubtitles_url(String subtitles_url) {
        this.subtitles_url = subtitles_url;
    }

    public long getSortorder() {
        return sortorder;
    }

    public void setSortorder(long sortorder) {
        this.sortorder = sortorder;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getModified() {
        return modified;
    }

    public void setModified(long modified) {
        this.modified = modified;
    }

    public int getActive() {
        return active;
    }

    public void setActive(int active) {
        this.active = active;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getFilesize() {
        return filesize;
    }

    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    public RealmList<AdvertInfo> getAds() {
        return ads;
    }

    public void setAds(RealmList<AdvertInfo> ads) {
        this.ads = ads;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getSubtitlePath() {
        return subtitlePath;
    }

    public void setSubtitlePath(String subtitlePath) {
        this.subtitlePath = subtitlePath;
    }
}
