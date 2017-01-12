package com.attvn.adtruevn;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.attvn.adtruevn.manager.AdsManager;
import com.attvn.adtruevn.model.AdvertInfo;
import com.attvn.adtruevn.model.Playlist;
import com.attvn.adtruevn.model.PlaylistStack;
import com.attvn.adtruevn.model.Video;
import com.attvn.adtruevn.ui.AdsView;
import com.attvn.adtruevn.ui.EventLogView;
import com.attvn.adtruevn.ui.OptionsView;
import com.attvn.adtruevn.ui.VideoMediaPlayerView;
import com.attvn.adtruevn.util.Global;
import com.attvn.adtruevn.util.Logging;
import com.attvn.adtruevn.util.SelectOption;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import rx.Subscription;

import static com.attvn.adtruevn.util.Params.TYPE_LIVE_STREAMING;

public class VideoMediaPlayer extends Activity implements
        TrackSelector.InvalidationListener,
        ExoPlayer.EventListener,
        AdsView.ResumeListener,
        OptionsView.OnItemClickListener {
    private static final String TAG = "VideoMediaPlayer";

    public static final String LAST_VIDEO_SUSPENDED_POSITION = "last_video_suspended";
    public static final String LAST_TIME_VIDEO_SUSPENDED = "last_time_video_suspended";
    public static final String LAST_VIDEO_DURATION_SUSPENDED = "last_video_duration_suspended";
    public static final String OFFLINE_MODE = "offline";
    public static final int FASTFORWARD_MS = 3000;
    public static final int REWIND_MS = 2000;

    private boolean isOnline;

    private VideoMediaPlayerView mVideoMediaPlayerView;
    private EventLogView mEventLogView;
    private OptionsView optionsView;
    private ProgressDialog dialogLoading;
    private FrameLayout mediaContainer;

    private Handler mainHandler;
    private AdsManager adsManager;

    private TrackSelector mTrackSelector;
    private LoadControl mLoadControl;
    private LoopingMediaSource playlistMediaSource;

    private SimpleExoPlayer player;
    private VideoControlRunnable videoControlRunnable;

    private Playlist mCurrentPlaylist;
    private ArrayList<Video> mCurrentVideoMedia;
    private int mMediaSize;

    private Realm realm;
    private SharedPreferences sharedPreferences;
    private boolean isPlayingVideoAds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE); //Remove title bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        mediaContainer = (FrameLayout) findViewById(R.id.video_media_container);
        dialogLoading = new ProgressDialog(this);
        showDialogLoading();

        mainHandler = new Handler();
        mVideoMediaPlayerView = (VideoMediaPlayerView) findViewById(R.id.video_media_player);
        mVideoMediaPlayerView.setFastForwardIncrementMs(FASTFORWARD_MS);
        mVideoMediaPlayerView.setRewindIncrementMs(REWIND_MS);
        mVideoMediaPlayerView.setParentActivity(this);
//        mVideoMediaPlayerView.setPlayerListener(this);
        mEventLogView = mVideoMediaPlayerView.getEventLogView();
        optionsView = mVideoMediaPlayerView.getOptionsView();
        optionsView.setOnItemClickListener(this);

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);

        // track selector for handling video track
        mTrackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        mTrackSelector.init(this);

        mLoadControl = new DefaultLoadControl();
        player = ExoPlayerFactory.newSimpleInstance(this, mTrackSelector, mLoadControl);
        player.addListener(this);

        //load data
        realm = Realm.getDefaultInstance();
        RealmResults<PlaylistStack> realmResults = realm.where(PlaylistStack.class).findAll();
        if(realmResults.size() > 0) {
            mCurrentPlaylist = realmResults.first().playlistStack.get(0);
        } else {
            finishAffinity();
        }

        isOnline = !getIntent().getBooleanExtra(OFFLINE_MODE, false);
        if(isOnline) {
            // TODO: do something while online
        }
        if(mCurrentPlaylist != null) {
            checkFilesExists();
            mVideoMediaPlayerView.setPlayer(player);
            adsManager = AdsManager.initWith(mVideoMediaPlayerView);
            if(isOnline) prepareAds();
            prepareMediaSource();
        } else {
            dialogLoading.dismiss();
            Intent intent = new Intent(this, SplashScreenActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        player.setPlayWhenReady(true);
        isPlayingVideoAds = false;
        resumeAndResetPreference();
    }

    private void checkFilesExists() {
        for(Video video: mCurrentPlaylist.getPlaylist()) {
            File videoFile = new File(video.getFilePath());
            if(!videoFile.exists()) {
                dialogLoading.dismiss();
                Intent intent = new Intent(this, SplashScreenActivity.class);
                startActivity(intent);
                break;
            }
        }
    }

    private void resumeAndResetPreference() {
        int lastPosition = sharedPreferences.getInt(LAST_VIDEO_SUSPENDED_POSITION, 0);
        long lastTimePosition = sharedPreferences.getLong(LAST_TIME_VIDEO_SUSPENDED, 0);
        long duration = sharedPreferences.getLong(LAST_VIDEO_DURATION_SUSPENDED, 0);

        if(lastPosition != 0 || lastTimePosition != 0) {
            Logging.log("[p] " + String.valueOf(lastPosition));
            Logging.log("[p] " + String.valueOf(lastTimePosition));

            Video lastVideo = mCurrentVideoMedia.get(lastPosition);
            if(lastVideo.getType() != TYPE_LIVE_STREAMING) {
                player.seekTo(lastPosition, lastTimePosition);
            } else {
                player.seekToDefaultPosition(lastPosition);
            }

            long remainTime = (lastVideo.getDuration() == 0 ?
                    duration : ((lastVideo.getDuration() + lastVideo.getStart()) * 1000))
                    - Math.abs(lastTimePosition);

            Logging.log("[p] remain time after ad played: " + remainTime);
            if(remainTime > 0) {
                mainHandler.removeCallbacks(videoControlRunnable);
                Logging.log("[p] reset runnable");
                videoControlRunnable = new VideoControlRunnable(lastPosition);
                mainHandler.postDelayed(videoControlRunnable, remainTime);
            }

            sharedPreferences.edit()
                    .putInt(LAST_VIDEO_SUSPENDED_POSITION, 0)
                    .putLong(LAST_TIME_VIDEO_SUSPENDED, 0)
                    .apply();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.exit_app_notification))
                .setMessage(getString(R.string.exit_app_question, getString(R.string.app_name)))
                .setPositiveButton(getString(R.string.yes_btn), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exitToApp();
                    }

                })
                .setNegativeButton(getString(R.string.no_btn), null)
                .show();
    }

    private void exitToApp() {
        player.setPlayWhenReady(false);
        player.release();
        if (videoControlRunnable != null) {
            mainHandler.removeCallbacks(videoControlRunnable);
        }
        finishAffinity();
    }

    private void prepareAds() {
        for(Video video: mCurrentPlaylist.getPlaylist()) {
            adsManager.addAdvert(video);
        }
    }

    private void prepareMediaSource() {
        mCurrentVideoMedia = new ArrayList<>();
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        String userAgent = Util.getUserAgent(this, getString(R.string.app_name));
        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter);

        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, bandwidthMeter, httpDataSourceFactory);

        ArrayList<MediaSource> listMediaSource = new ArrayList<>();
        RealmResults<Video> videos = mCurrentPlaylist.getPlaylist().sort("sortorder", Sort.ASCENDING);
        for(Video video: videos) {
            if(!isOnline) {
                if(video.getType() == TYPE_LIVE_STREAMING) continue;
            }

            Uri uri;
            if(video.getType() == TYPE_LIVE_STREAMING) {
                uri = Uri.parse(video.getVideo_url());
            } else {
                try {
                    Log.d(TAG, "[file path] " + video.getFilePath());
//                        Logging.log("[n] " + video.getFilePath());
                    uri = Uri.parse(video.getFilePath());
                } catch (NullPointerException ex) {
                    ex.printStackTrace();
                    continue;
                }
            }

            if (Util.maybeRequestReadExternalStoragePermission(this, uri)) {
                // The player will be reinitialized if the permission is granted.
                return;
            }

            MediaSource mediaSource = buildMediaSource(uri, dataSourceFactory, httpDataSourceFactory, "");
            if(video.getSubtitles_url() != null) {
                Format textFormat = Format.createTextSampleFormat(null, MimeTypes.APPLICATION_SUBRIP,
                        null, Format.NO_VALUE, Format.NO_VALUE, "vi", null);
                MediaSource textMediaSource = new SingleSampleMediaSource(Uri.parse(video.getSubtitles_url()),
                        dataSourceFactory, textFormat, C.TIME_UNSET);
                mediaSource = new MergingMediaSource(mediaSource, textMediaSource);
            }
            listMediaSource.add(mediaSource);
            mCurrentVideoMedia.add(video);
        }

        mMediaSize = listMediaSource.size();
        MediaSource [] m = new MediaSource[mMediaSize];

        ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource(listMediaSource.toArray(m));
        playlistMediaSource = new LoopingMediaSource(concatenatingMediaSource);
        player.prepare(playlistMediaSource);
    }

    private MediaSource buildMediaSource(Uri uri, DefaultDataSourceFactory defaultDataSourceFactory,
                                         DataSource.Factory mediaDataSourceFactory, String overrideExtension) {
        int type = Util.inferContentType(!TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
                : uri.getLastPathSegment());
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, defaultDataSourceFactory,
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, null);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, defaultDataSourceFactory,
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, null);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, null);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, defaultDataSourceFactory,
                        new DefaultExtractorsFactory(),
                        mainHandler, null);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    @Override
    public void onTrackSelectionsInvalidated() {
        Log.d(TAG, "invalidate");
        Logging.log("[n] invalidate");
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
//        Logging.log("[n] " + timeline.getWindowCount());
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        if(!isPlayingVideoAds) {
            if (dialogLoading.isShowing()) {
                dialogLoading.hide();
            }
            Log.d(TAG, "[n] Current window position " + player.getCurrentWindowIndex());
            Logging.log("[n] Current window position " + player.getCurrentWindowIndex());

            final int currentWindowIndex = player.getCurrentWindowIndex() % mMediaSize;
            final Video currentVideo = mCurrentVideoMedia.get(currentWindowIndex);

            Logging.log("[n] " + currentVideo.getVideo_url());
            Log.d(TAG, String.valueOf(currentVideo.getStart()));
            Log.d(TAG, String.valueOf(currentVideo.getDuration()));
            Logging.log("[n] " + "duration: " + String.valueOf(player.getDuration()));

            final int startTime = (int) currentVideo.getStart();
            int desiredDuration = (int) currentVideo.getDuration();

            if (currentVideo.getType() != TYPE_LIVE_STREAMING) {
                mLoadControl.shouldStartPlayback(startTime * 1000, false);
                if (startTime + desiredDuration > player.getDuration() / 1000) return;
            }

            if (currentWindowIndex == 0)
                player.seekTo(startTime * 1000);

            final long currentDuration = desiredDuration == 0 ? player.getDuration() : desiredDuration * 1000;

            if (videoControlRunnable == null && player.getCurrentPosition() >= startTime * 1000) {
                videoControlRunnable = new VideoControlRunnable(currentWindowIndex);
                mainHandler.postDelayed(videoControlRunnable, currentDuration);
            }

            List<AdsView> lsAdsView = adsManager.getCurrentListAds(currentVideo.getId());
            final List<AdvertInfo> lsAdsInfo = adsManager.getListAdInfoInCurrentVideo(currentVideo.getId());

            if(lsAdsInfo != null && lsAdsInfo.size() > 0) {
                int i = 0;
                for (final AdsView adsView : lsAdsView) {
                    final AdvertInfo advertInfo = lsAdsInfo.get(i);
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mVideoMediaPlayerView.showSkipableView(true);
                        }
                    }, (adsView.getStartTime() - 3) * 1000);
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (advertInfo.getType() == AdvertInfo.TypeAds.TYPE_VIDEO) {
                                isPlayingVideoAds = true;
                                player.setPlayWhenReady(false);
                                Global.saveLastPositionVideoPlay(
                                        VideoMediaPlayer.this,
                                        player,
                                        currentVideo,
                                        currentDuration,
                                        currentWindowIndex
                                );
                            }
                            adsView.show();
                        }
                    }, adsView.getStartTime() * 1000);
                    i++;
                }
            }
        }
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Logging.log("[e] " + error.type);
        Logging.log("[e] " + error.toString());

        int currentPosition = player.getCurrentWindowIndex() % mMediaSize;
        Logging.log("[a] save last position");

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(LAST_VIDEO_SUSPENDED_POSITION, currentPosition);
        if(mCurrentVideoMedia.get(currentPosition).getType() == TYPE_LIVE_STREAMING) {
            long position = player.getCurrentPosition();
            Timeline.Period period = new Timeline.Period();
            Timeline currentTimeline = player.getCurrentTimeline();
            if (!currentTimeline.isEmpty()) {
                position -= currentTimeline.getPeriod(player.getCurrentPeriodIndex(), period)
                        .getPositionInWindowMs();
            }
            editor.putLong(LAST_TIME_VIDEO_SUSPENDED, position);
        } else {
            editor.putLong(LAST_VIDEO_DURATION_SUSPENDED, player.getDuration());
            editor.putLong(LAST_TIME_VIDEO_SUSPENDED, player.getCurrentPosition());
        }
        editor.apply();
        Logging.log("[a] restart activity");
        finish();
        startActivity(getIntent());
    }

    @Override
    public void onPositionDiscontinuity() {

    }

    private void showDialogLoading(){
        dialogLoading.setMessage("Loading ...");
        dialogLoading.setCancelable(false);
        dialogLoading.setInverseBackgroundForced(false);
        dialogLoading.show();
    }

    @Override
    public void resumePlaylist() {
        player.prepare(playlistMediaSource);
//        resumePlay();
        resumeAndResetPreference();
        player.setPlayWhenReady(true);
    }

    @Override
    public void onClickOptionItem(int position) {
        List<Subscription> listSubscription = new ArrayList<>();

        ProgressDialog dialogDownloading = new ProgressDialog(this);
        dialogDownloading.setMessage("Downloading ...");
        dialogDownloading.setCancelable(false);
        dialogDownloading.setMax(100);
        dialogDownloading.hide();

        SelectOption.select(this, position, listSubscription, dialogDownloading);
    }

//    private void resumePlay() {
//        int currentIndex = player.getCurrentWindowIndex() % mMediaSize;
//        Video currentVideo = mCurrentVideoMedia.get(currentIndex);
//
//        long maxTimePotential = (currentVideo.getStart() + currentVideo.getDuration()) * 1000;
//        if(player.getCurrentPosition() < maxTimePotential) {
//            Log.d(TAG, "remaining time: " + String.valueOf(maxTimePotential - player.getCurrentPosition()));
//            videoControlRunnable = new VideoControlRunnable(currentIndex);
//            mainHandler.postDelayed(videoControlRunnable, maxTimePotential - player.getCurrentPosition());
//        } else {
//            int nextIndex = currentIndex == mMediaSize - 1 ? 0 : currentIndex + 1;
//            player.seekTo(nextIndex, mCurrentVideoMedia.get(nextIndex).getStart() * 1000);
//        }
//
//        player.setPlayWhenReady(true);
//    }

    private class VideoControlRunnable implements Runnable {
        int currentWindowIndex;

        VideoControlRunnable(int index) {
            this.currentWindowIndex = index;
        }

        @Override
        public void run() {
            Video nextVideo;
            if (currentWindowIndex + 1 == mCurrentVideoMedia.size()) {
                nextVideo = mCurrentVideoMedia.get(0);
            } else {
                nextVideo = mCurrentVideoMedia.get(currentWindowIndex + 1);
            }
            player.seekTo(currentWindowIndex + 1, nextVideo.getStart() * 1000);
            mainHandler.removeCallbacks(videoControlRunnable);
            videoControlRunnable = null;
            isPlayingVideoAds = false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN) {
            optionsView.showWithTimeout(20);
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Logging.log("[p] key pressed: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                optionsView.showWithTimeout(20);
                return true;
            case KeyEvent.KEYCODE_BACK:
                onBackPressed();
                return true;
        }
        return false;
    }
}
