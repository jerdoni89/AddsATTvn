package com.attvn.adtruevn.manager;

import com.attvn.adtruevn.model.AdvertInfo;
import com.attvn.adtruevn.model.Video;
import com.attvn.adtruevn.ui.AdsScrollableView;
import com.attvn.adtruevn.ui.AdsView;
import com.attvn.adtruevn.ui.VideoMediaPlayerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import io.realm.Realm;

import static com.attvn.adtruevn.model.AdvertInfo.TypeAds.TYPE_IMAGE;
import static com.attvn.adtruevn.model.AdvertInfo.TypeAds.TYPE_TEXT_SCROLL;
import static com.attvn.adtruevn.model.AdvertInfo.TypeAds.TYPE_VIDEO;

/**
 * Created by app on 12/27/16.
 */

public final class AdsManager {

    private final VideoMediaPlayerView mPlayer;

    // list ads by {video_id}
    private final HashMap<Long, List<AdvertInfo>> mListAds;
    private final HashMap<Long, List<AdsView>> mListAdsView;

    private final Realm realm;

    private AdsManager(VideoMediaPlayerView playerView) {
        this.mPlayer = playerView;
        mListAds = new LinkedHashMap<>();
        mListAdsView = new LinkedHashMap<>();
        this.realm = Realm.getDefaultInstance();
    }

    public static AdsManager initWith(VideoMediaPlayerView player) {
        return new AdsManager(player);
    }

    public void addAdvert(Video video) {
        if(mPlayer != null) {
            mListAds.put(video.getId(), video.getAds());

            // bind data
            List<AdsView> lsAdsView = new ArrayList<>();
            for(AdvertInfo advertInfo: video.getAds()) {
                AdsView adsView = mPlayer.addAdsView();
                if(adsView != null) {
                    lsAdsView.add(adsView);
                    bindData(adsView, advertInfo);
                }
            }
            mListAdsView.put(video.getId(), lsAdsView);
        }
    }

    private void bindData(AdsView adsView, AdvertInfo adsInfo) {
        AdvertInfo advertInfo = realm.copyFromRealm(adsInfo);

        adsView.setStartTime(advertInfo.getStart());
        adsView.setTimeout(advertInfo.getDuration());
        adsView.switchPositionMode(advertInfo.getPosition());
        switch (advertInfo.getType()) {
            case TYPE_IMAGE:
                adsView.setResource(advertInfo.getFile_path());
                break;
            case TYPE_TEXT_SCROLL:
                adsView.setUseScrollableText(true);
                if(adsView.getAdContentView() instanceof AdsScrollableView) {
                    AdsScrollableView adsScrollableView = (AdsScrollableView) adsView.getAdContentView();
                    adsScrollableView.setScrollableMessageText(advertInfo.getContent());
                }
                break;
            case TYPE_VIDEO:
                adsView.setPlayerHandling(mPlayer);
                adsView.setMediaPlayer(mPlayer.getPlayer());
                adsView.setAdsPlayerStart(advertInfo.getFile_path(), mPlayer.getHandler());
                adsView.setResumeListener(mPlayer.getParentActivity());
                break;
        }
    }

    public List<AdsView> getCurrentListAds(long videoId) {
        return mListAdsView.get(videoId);
    }

    public List<AdvertInfo> getListAdInfoInCurrentVideo(long videoId){
        return mListAds.get(videoId);
    }
}
