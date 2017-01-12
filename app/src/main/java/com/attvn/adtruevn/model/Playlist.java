package com.attvn.adtruevn.model;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by app on 11/21/16.
 */

public class Playlist extends RealmObject {

    @PrimaryKey
    private long id;

    private String description;
    private long created;
    private long modified;

    private RealmList<Video> playlist;

    public Playlist() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public RealmList<Video> getPlaylist() {
        return playlist;
    }

    public void setPlaylist(RealmList<Video> playlist) {
        this.playlist = playlist;
    }
}
