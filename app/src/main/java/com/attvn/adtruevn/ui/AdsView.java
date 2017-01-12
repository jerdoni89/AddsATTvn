package com.attvn.adtruevn.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.attvn.adtruevn.R;
import com.attvn.adtruevn.model.AdvertInfo;
import com.attvn.adtruevn.util.Global;
import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import static com.attvn.adtruevn.model.AdvertInfo.AdsPosition.POPUP;

/**
 * Created by app on 12/15/16.
 */

public class AdsView extends FrameLayout {
    private static final String TAG = "AdsView";
    private static final int DEFAULT_TIMEOUT = 5;
    private static final int DEFAULT_SIZE = 42;
    private static final int DEFAULT_SCROLLABLE_SIZE = 200;

    private Context mContext;
    private FrameLayout mAdsContainerView;
    private View mAdContentView;
    private FrameLayout.LayoutParams mContainerParams;

    private boolean isAttachedToWindow;
    private boolean useScrollableText = false;

    private int mStartTime;
    private int mTimeout;
    private int mPositionMode;
    private int defaultSize;
    private int defaultScrollableSize;

    private Runnable hideAction = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    public AdsView(Context context) {
        this(context, null, 0);
    }

    public AdsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AdsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;
        if(attrs != null) {
            TypedArray ar = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AdsView, 0, 0);

            try {
                mPositionMode = ar.getInt(R.styleable.AdsView_position_mode, AdvertInfo.AdsPosition.TOP);
                useScrollableText = ar.getBoolean(R.styleable.AdsView_use_scrollable_text, useScrollableText);
                mTimeout = ar.getInt(R.styleable.AdsView_timeout, DEFAULT_TIMEOUT);
            } finally {
                ar.recycle();
            }
        } else {
            mTimeout = DEFAULT_TIMEOUT;
        }

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics realMetrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealMetrics(realMetrics);
        }
        defaultSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_SIZE, realMetrics);
        defaultScrollableSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_SCROLLABLE_SIZE, realMetrics);

        LayoutInflater.from(context).inflate(R.layout.advertisement_view, this);
        mAdsContainerView = (FrameLayout) findViewById(R.id.ads_container_view);
        mContainerParams = (FrameLayout.LayoutParams) mAdsContainerView.getLayoutParams();

        setPositionMode();
        addContentView();
    }

    private void addContentView() {
        mAdsContainerView.removeAllViews();

        mAdContentView = useScrollableText ? new AdsScrollableView(getContext()) : new ImageView(getContext());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        mAdContentView.setLayoutParams(params);
        mAdsContainerView.addView(mAdContentView);
        mAdsContainerView.requestLayout();

        if(useScrollableText) {
            setPositionMode();
        }
    }

    public void setUseScrollableText(boolean isScrollableText) {
        this.useScrollableText = isScrollableText;
        addContentView();
    }

    private SimpleExoPlayer mediaPlayer;
    private MediaSource mediaSource;

    public void setMediaPlayer(SimpleExoPlayer player) {
        this.mediaPlayer = player;
    }

    public void setAdsPlayerStart(String url, Handler mainHandler) {
        mAdContentView = new View(getContext());
        DefaultBandwidthMeter bandwidthMeter1 = new DefaultBandwidthMeter();
        String userAgent = Util.getUserAgent(getContext(), getContext().getString(R.string.app_name));

        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter1);
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(getContext(), bandwidthMeter1, httpDataSourceFactory);

        Uri uriVideo = Uri.parse(url);
        mediaSource = Global.buildMediaSource(uriVideo, dataSourceFactory, httpDataSourceFactory, mainHandler, "");
    }

//    private AdsPlayerBuilder adsPlayerBuilder;
//
//    public AdsPlayerBuilder getAdsPlayerBuilder(String uri) {
//        if(adsPlayerBuilder == null)
//            adsPlayerBuilder = new AdsPlayerBuilder(uri);
//        return adsPlayerBuilder;
//    }
//
//    public class AdsPlayerBuilder {
//        private DefaultHttpDataSourceFactory httpDataSourceFactory;
//        private DefaultDataSourceFactory dataSourceFactory;
//        private Handler adsHandler;
//        private SimpleExoPlayer AdsPlayer;
//        private String uri;
//
//        private AdsPlayerBuilder(String uri) {
//            this.uri = uri;
//            adsHandler = new Handler();
//            setVideoFrame1();
//        }
//
//        private VideoView videoView;
//        private MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(MediaPlayer mp) {
//                videoView.start();
//            }
//        };
//
//        private void setVideoFrame1() {
//            mAdsContainerView.removeAllViews();
//            mAdContentView = new VideoView(getContext());
//            videoView = (VideoView) mAdContentView;
//
//            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
//                    (int) (Global.WIDTH_SCREEN * 0.8f),
//                    (int) (Global.HEIGHT_SCREEN * 0.8f)
//            );
//
//            mAdContentView.setLayoutParams(params);
//            mContainerParams.gravity = Gravity.CENTER;
//
//            mAdsContainerView.addView(mAdContentView);
//            mAdsContainerView.requestLayout();
//        }
//
//        private void setVideoFrame() {
//            mAdsContainerView.removeAllViews();
//            mAdContentView = new VideoMediaPlayerView(getContext());
//
//            // init video ads
//            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
//            TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);
//            DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
//
//            LoadControl loadControl = new DefaultLoadControl();
//            AdsPlayer = ExoPlayerFactory.newSimpleInstance(getContext(), trackSelector, loadControl);
//
//            VideoMediaPlayerView videoFrameAds = (VideoMediaPlayerView) mAdContentView;
//            videoFrameAds.setUseController(false);
//            videoFrameAds.setPlayer(AdsPlayer);
//
//            DefaultBandwidthMeter bandwidthMeter1 = new DefaultBandwidthMeter();
//            String userAgent = Util.getUserAgent(getContext(), getContext().getString(R.string.app_name));
//            httpDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter1);
//            dataSourceFactory = new DefaultDataSourceFactory(getContext(), bandwidthMeter1, httpDataSourceFactory);
//
//            // set frame for ads video
//            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
//                    (int) (Global.WIDTH_SCREEN * 0.85f),
//                    (int) (Global.HEIGHT_SCREEN * 0.85f)
//            );
//
//            mAdContentView.setLayoutParams(params);
//            mContainerParams.gravity = Gravity.CENTER;
//
//            mAdsContainerView.addView(mAdContentView);
//            mAdsContainerView.requestLayout();
//        }
//
//        private void playVideoUri() {
////            Uri uriVideo = Uri.parse(uri);
////            MediaSource mediaSource = Global.buildMediaSource(uriVideo, dataSourceFactory, httpDataSourceFactory, adsHandler, "");
////            AdsPlayer.prepare(mediaSource);
////            if(!AdsPlayer.getPlayWhenReady()) {
////                AdsPlayer.setPlayWhenReady(true);
////            }
//            try {
//                videoView.setVideoURI(Uri.parse(uri));
//                videoView.setOnTouchListener(null);
//            } catch (Exception e) {
//                Log.e(TAG, e.getMessage());
//            }
//            videoView.requestFocus();
//            videoView.setOnPreparedListener(onPreparedListener);
//            adsHandler.postDelayed(stopPlay, mTimeout * 1000);
//        }
//
//        private Runnable stopPlay = new Runnable() {
//            @Override
//            public void run() {
//                stopAndReleasePlayer();
//            }
//        };
//
//        private void stopAndReleasePlayer() {
////            AdsPlayer.setPlayWhenReady(false);
////            AdsPlayer.release();
//            videoView.pause();
//            videoView.stopPlayback();
//            mAdsContainerView.removeAllViews();
//            hide();
//        }
//    }


    private void setPositionMode() {
        switch (mPositionMode) {
            case AdvertInfo.AdsPosition.TOP:
                mContainerParams.gravity = Gravity.TOP;
                mContainerParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                mContainerParams.height = useScrollableText ? defaultScrollableSize : defaultSize;
                break;
            case AdvertInfo.AdsPosition.LEFT:
                if (useScrollableText) break;
                mContainerParams.gravity = Gravity.START;
                mContainerParams.width = defaultSize;
                mContainerParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                break;
            case AdvertInfo.AdsPosition.BOTTOM:
                mContainerParams.gravity = Gravity.BOTTOM;
                mContainerParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                mContainerParams.height = useScrollableText ? defaultScrollableSize : defaultSize;
                break;
            case AdvertInfo.AdsPosition.RIGHT:
                if (useScrollableText) break;
                mContainerParams.gravity = Gravity.END;
                mContainerParams.width = defaultSize;
                mContainerParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            case AdvertInfo.AdsPosition.POPUP:
                if (useScrollableText) break;
                mContainerParams.gravity = Gravity.CENTER;
                mContainerParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                mContainerParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                break;
        }
    }

    public boolean isVisibility() {
        return getVisibility() == VISIBLE;
    }

    public void setTimeout(int timeout) {
        mTimeout = timeout;
    }

    public void show() {
        removeCallbacks(hideAction);
        setVisibility(VISIBLE);

        if (isAttachedToWindow /*&& !useScrollableText*/) {
            if(!(mAdContentView instanceof ImageView) && !(mAdContentView instanceof AdsScrollableView)) {
                if(mediaPlayer != null) {
                    mediaPlayer.prepare(mediaSource);
                    mediaPlayer.setPlayWhenReady(true);
                }
                if(mPlayerView != null) {
                    postDelayed(waitingRunnable, (mTimeout-3) * 1000);
                }
            }
            postDelayed(hideAction, mTimeout * 1000);
        }
    }

    public void hide() {
        if(isVisibility()) {
            setVisibility(GONE);
            removeCallbacks(hideAction);
            if(mediaPlayer != null) {
                if(mediaPlayer.getPlayWhenReady()) {
                    mediaPlayer.setPlayWhenReady(false);
                    resumeListener.resumePlaylist();
                }
            }
        }
    }

    private ResumeListener resumeListener;

    public void setResumeListener(ResumeListener resumeListener) {
        this.resumeListener = resumeListener;
    }

    private VideoMediaPlayerView mPlayerView;

    public void setPlayerHandling(VideoMediaPlayerView mPlayer) {
        mPlayerView = mPlayer;
    }

    private Runnable waitingRunnable = new Runnable() {
        @Override
        public void run() {
            showSkipable();
        }
    };

    private void showSkipable() {
        mPlayerView.showSkipableView(false);
    }

    public interface ResumeListener {
        void resumePlaylist();
    }

    public void setResource(Drawable resource) {
        if(!useScrollableText) {
            ImageView gifImageView = ((ImageView) mAdContentView);
            gifImageView.setImageDrawable(resource);
            gifImageView.setScaleType(ImageView.ScaleType.FIT_XY);
        }
    }

    public void setResource(final String url)  {
        if(!useScrollableText) {
            final ImageView gifImageView = ((ImageView) mAdContentView);
            Glide.with(getContext())
                    .load(url)
                    .error(R.drawable.ads_1)
                    .override((int) (Global.WIDTH_SCREEN * 0.6f),
                            mPositionMode < POPUP ? (int) (0.4f * Global.HEIGHT_SCREEN) : Global.HEIGHT_SCREEN)
                    .crossFade()
                    .into(gifImageView);
//            GifDrawable gifDrawable = null;
//            try {
//                gifDrawable = new GifDrawable(url);
//                gifImageView.setImageDrawable(gifDrawable);
//                gifImageView.setScaleType(ImageView.ScaleType.FIT_XY);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    public void switchPositionMode(int positionMode) {
        if (positionMode > POPUP) return;
        mPositionMode = positionMode;
        setPositionMode();
    }

    public View getAdContentView() {
        return mAdContentView;
    }

    public void setStartTime(int startTime) {
        mStartTime = startTime;
    }

    public int getStartTime() {
        return mStartTime;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttachedToWindow = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttachedToWindow = false;
        removeCallbacks(hideAction);
    }
}
