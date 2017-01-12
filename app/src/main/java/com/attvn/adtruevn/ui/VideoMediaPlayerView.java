package com.attvn.adtruevn.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.attvn.adtruevn.R;
import com.attvn.adtruevn.VideoMediaPlayer;
import com.attvn.adtruevn.util.Global;
import com.attvn.adtruevn.util.Logging;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SubtitleView;

import java.util.List;

/**
 * Created by app on 12/6/16.
 */

public final class VideoMediaPlayerView extends FrameLayout {
    private static final String TAG = VideoMediaPlayerView.class.getSimpleName();

    private final FrameLayout container;
    private final View surfaceView;
    private final View shutterView;
    private EventLogView eventLogView;
    private OptionsView optionsView;
    private final SubtitleView subtitleLayout;
//    private final AdsView adsView;
//    private final SubMenuView subMenuLayout;
    private final AspectRatioFrameLayout layout;

    private final PlaybackControlView controller;
    private final ComponentListener componentListener;

    private SimpleExoPlayer player;
    private boolean useController = true;
    private boolean useAds = false;
    private int controllerShowTimeoutMs;

    public VideoMediaPlayerView(Context context) {
        this(context, null, 0);
    }

    public VideoMediaPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoMediaPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        boolean useTextureView = false;
        boolean useEventLogView = false;
        int resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
        int rewindMs = PlaybackControlView.DEFAULT_REWIND_MS;
        int fastForwardMs = PlaybackControlView.DEFAULT_FAST_FORWARD_MS;
        int controllerShowTimeoutMs = PlaybackControlView.DEFAULT_SHOW_TIMEOUT_MS;

        if(attrs != null) {
            TypedArray ar = context.getTheme().obtainStyledAttributes(attrs,
                    R.styleable.VideoMediaPlayerView, 0, 0);
            try {
                useController = ar.getBoolean(R.styleable.VideoMediaPlayerView_use_controller, useController);
                useTextureView = ar.getBoolean(R.styleable.VideoMediaPlayerView_use_texture_view, useTextureView);
                useEventLogView = ar.getBoolean(R.styleable.VideoMediaPlayerView_use_event_log_view, useEventLogView);
                useAds = ar.getBoolean(R.styleable.VideoMediaPlayerView_use_ads, useAds);
                resizeMode = ar.getInt(com.google.android.exoplayer2.R.styleable.AspectRatioFrameLayout_resize_mode,
                        resizeMode);
                rewindMs = ar.getInt(R.styleable.VideoMediaPlayerView_rewind_increment, rewindMs);
                fastForwardMs = ar.getInt(R.styleable.VideoMediaPlayerView_fastforward_increment, fastForwardMs);
                controllerShowTimeoutMs = ar.getInt(R.styleable.VideoMediaPlayerView_show_timeout, controllerShowTimeoutMs);
            } finally {
                ar.recycle();
            }
        }

        LayoutInflater.from(context).inflate(R.layout.video_media_player_view, this);

        container = (FrameLayout) findViewById(R.id.container);
        componentListener = new ComponentListener();
        layout = (AspectRatioFrameLayout) findViewById(R.id.video_frame);
        setResizeModeRaw(layout, resizeMode);

        shutterView = findViewById(R.id.shutter);
        subtitleLayout = (SubtitleView) findViewById(R.id.subtitles);
        subtitleLayout.setUserDefaultStyle();
        subtitleLayout.setUserDefaultTextSize();

//        adsView = (AdsView) findViewById(R.id.ads_view);
//        if(!useAds) removeView(adsView);
//        adsView.hide();

        controller = (PlaybackControlView) findViewById(R.id.control);
        controller.hide();
        controller.setRewindIncrementMs(rewindMs);
        controller.setFastForwardIncrementMs(fastForwardMs);
        this.controllerShowTimeoutMs = controllerShowTimeoutMs;

        surfaceView = useTextureView ? new TextureView(context) : new SurfaceView(context);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        surfaceView.setLayoutParams(params);
        layout.addView(surfaceView, 0);

        if(useEventLogView) {
            setEventLogView(context);
        }
        initOptionsView(context);
    }

    @SuppressWarnings("ResourceType")
    private static void setResizeModeRaw(AspectRatioFrameLayout aspectRatioFrame, int resizeMode) {
        aspectRatioFrame.setResizeMode(resizeMode);
    }

    private void setEventLogView(Context context) {
        eventLogView = new EventLogView(context);
        FrameLayout.LayoutParams params = new LayoutParams(Global.WIDTH_SCREEN,(int) (0.25f * Global.HEIGHT_SCREEN));
        params.gravity = Gravity.TOP | Gravity.START;
        container.addView(eventLogView, params);
    }

    private void initOptionsView(Context context) {
        optionsView = new OptionsView(context);
        optionsView.setOrientation(OptionsView.HORIZONTAL);
        optionsView.setIsDownloadScreen(false);
        optionsView.setVisibility(GONE);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.BOTTOM | Gravity.END;
        container.addView(optionsView, params);
    }

    public OptionsView getOptionsView() {
        return optionsView;
    }

    public EventLogView getEventLogView() {
        return eventLogView;
    }

    public AdsView addAdsView() {
        if(!useAds) return null;

        AdsView adsView = new AdsView(getContext());
        FrameLayout.LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        adsView.setLayoutParams(params);
        adsView.hide();
        container.addView(adsView);

        return adsView;
    }

    private SkipableView skipableView;

    public void showSkipableView(boolean isOpen) {
        if(skipableView == null)
            skipableView = (SkipableView) findViewById(R.id.skipable_view);

        skipableView.setVisibility(VISIBLE);
        skipableView.countTime(isOpen);
//        postDelayed(playSkipableViewRunnable, 3*1000);
    }

//    private Runnable playSkipableViewRunnable = new Runnable() {
//        @Override
//        public void run() {
//            if(skipableView != null) {
//                skipableView.setVisibility(GONE);
//            }
//        }
//    };

    private VideoMediaPlayer parentActivity;

    public VideoMediaPlayer getParentActivity() {
        return parentActivity;
    }

    public void setParentActivity(VideoMediaPlayer activity) {
        this.parentActivity = activity;
    }

    public SimpleExoPlayer getPlayer() {
        return player;
    }

    public void setPlayer(SimpleExoPlayer player) {
        if(this.player == player) return;

        if(this.player != null) {
            this.player.setTextOutput(null);
            this.player.setVideoListener(null);
            this.player.removeListener(componentListener);
            this.player.setVideoSurface(null);
        }

        this.player = player;

        if(useController) {
            controller.setPlayer(player);
        }

        if(player != null) {
            if(surfaceView instanceof TextureView) {
                player.setVideoTextureView((TextureView) surfaceView);
            } else if(surfaceView instanceof SurfaceView) {
                player.setVideoSurfaceView((SurfaceView) surfaceView);
            }

            player.setVideoListener(componentListener);
            player.addListener(componentListener);
            player.setTextOutput(componentListener);
        }
    }

    public void setUseController(boolean useController) {
        if(this.useController == useController) return;

        this.useController = useController;

        if(useController) {
            controller.setPlayer(player);
        } else {
            controller.hide();
            controller.setPlayer(null);
        }
    }

    public void setControllerShowTimeoutMs(int controllerShowTimeoutMs) {
        this.controllerShowTimeoutMs = controllerShowTimeoutMs;
    }

    public void setControllerVisibilityListener(PlaybackControlView.VisibilityListener listener) {
        controller.setVisibilityListener(listener);
    }

    public void setRewindIncrementMs(int rewindMs) {
        controller.setRewindIncrementMs(rewindMs);
    }

    public void setFastForwardIncrementMs(int fastForwardMs) {
        controller.setFastForwardIncrementMs(fastForwardMs);
    }

    public View getVideoSurfaceView() {
        return surfaceView;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!useController || player == null || event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        if (controller.isVisible()) {
            controller.hide();
        } else {
            maybeShowController(true);
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!useController || player == null) {
            return false;
        }
        maybeShowController(true);
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return useController ? controller.dispatchKeyEvent(event) : super.dispatchKeyEvent(event);
    }

    private void maybeShowController(boolean isForced) {
        if (!useController || player == null) {
            return;
        }
        int playbackState = player.getPlaybackState();
        boolean showIndefinitely = playbackState == ExoPlayer.STATE_IDLE
                || playbackState == ExoPlayer.STATE_ENDED || !player.getPlayWhenReady();
        boolean wasShowingIndefinitely = controller.isVisible() && controller.getShowTimeoutMs() <= 0;
        controller.setShowTimeoutMs(showIndefinitely ? 0 : controllerShowTimeoutMs);
        if (isForced || showIndefinitely || wasShowingIndefinitely) {
            controller.show();
        }
    }

    private PlayerListener playerListener;

    public void setPlayerListener(PlayerListener listener) {
        this.playerListener = listener;
    }

    private final class ComponentListener implements SimpleExoPlayer.VideoListener, TextRenderer.Output, ExoPlayer.EventListener {

        // ExoPlayer.EventListener implementation

        @Override
        public void onLoadingChanged(boolean isLoading) {
            Log.d(TAG, isLoading ? "loading" : "done loading");
            Logging.log("[p]" + (isLoading ? " loading" : " done loading"));
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            maybeShowController(false);
            switch (playbackState) {
                case ExoPlayer.STATE_IDLE:
                    Logging.log("[p] state: idle");
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    Logging.log("[p] state: buffering");
                    break;
                case ExoPlayer.STATE_READY:
                    Logging.log("[p] state: ready");
                    break;
                case ExoPlayer.STATE_ENDED:
                    Logging.log("[p] state: ended");
                    break;
            }
        }

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            if(playerListener != null) {
                playerListener.onContinueWhenError();
                String s = error.getCause().getMessage();
                Logging.log("[e] " + s);
                Log.d(TAG, s);
            }
        }

        @Override
        public void onPositionDiscontinuity() {
            Log.d(TAG, "[p] discontinuity");
            Logging.log("[p] discontinuity");
        }

        // SimpleExoPlayer.VideoListener implementation

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            if (layout != null) {
                float aspectRatio = height == 0 ? 1 : (width * pixelWidthHeightRatio) / height;
                layout.setAspectRatio(aspectRatio);
            }
        }

        @Override
        public void onRenderedFirstFrame() {
            if (shutterView != null) {
                shutterView.setVisibility(INVISIBLE);
            }
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            Log.d(TAG, "tracked");
            Logging.log("[p] video tracked");

            if (shutterView != null) {
                shutterView.setVisibility(VISIBLE);
            }
            if(playerListener != null) {
                playerListener.onSelectedTrack(trackGroups, trackSelections);
            }
        }

        // TextRenderer.Output implementation

        @Override
        public void onCues(List<Cue> cues) {
            subtitleLayout.onCues(cues);
        }
    }

    public interface PlayerListener {
        void onSelectedTrack(TrackGroupArray trackGroups, TrackSelectionArray trackSelections);
        void onContinueWhenError();
    }
}
