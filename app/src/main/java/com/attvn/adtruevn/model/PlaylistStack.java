package com.attvn.adtruevn.model;

import java.io.File;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by app on 11/24/16.
 */

public class PlaylistStack extends RealmObject {

    public RealmList<Playlist> playlistStack;

    public PlaylistStack() {
        playlistStack = new RealmList<>();
    }

    public void push(Playlist playlist) {
        if(playlistStack != null) {
            playlistStack.add(0, playlist);
        }
    }

    public void pop() {
        if(playlistStack != null) {
            playlistStack.remove(0);
        }
    }

    public void deleteLastItem() {
        if(playlistStack != null && playlistStack.size() > 0) {
            deleteVideoPlaylistFiles(playlistStack.last());
            playlistStack.deleteLastFromRealm();
        }
    }

    private void deleteVideoPlaylistFiles(Playlist playlist) {
        for(Video video: playlist.getPlaylist()) {
            File videoFile = new File(video.getFilePath());
            if(videoFile.exists()) {
                videoFile.delete();
            }
            for(AdvertInfo advertInfo: video.getAds()) {
                if(advertInfo.getType() == AdvertInfo.TypeAds.TYPE_TEXT_SCROLL) continue;
                File adFile = new File(advertInfo.getFile_path());
                if(adFile.exists()) {
                    adFile.delete();
                }
            }
        }
    }
}
