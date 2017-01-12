package com.attvn.adtruevn.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;

import com.attvn.adtruevn.R;
import com.attvn.adtruevn.model.Video;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.lang.reflect.Method;

import retrofit2.Retrofit;

import static android.content.Context.MODE_PRIVATE;
import static com.attvn.adtruevn.VideoMediaPlayer.LAST_TIME_VIDEO_SUSPENDED;
import static com.attvn.adtruevn.VideoMediaPlayer.LAST_VIDEO_DURATION_SUSPENDED;
import static com.attvn.adtruevn.VideoMediaPlayer.LAST_VIDEO_SUSPENDED_POSITION;
import static com.attvn.adtruevn.util.Params.TYPE_LIVE_STREAMING;

/**
 * Created by app on 12/9/16.
 */

public final class Global {

    public static File FOLDER_PLAYLIST_DOWNLOADED;
    public static int WIDTH_SCREEN, HEIGHT_SCREEN;
    public static Retrofit retrofit;

    private static DisplayMetrics realMetrics;

    public static void getScreenSize(Context _context) {
        Display display = ((Activity) _context).getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= 17){
            //new pleasant way to get real metrics
            realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            WIDTH_SCREEN = realMetrics.widthPixels;
            HEIGHT_SCREEN = realMetrics.heightPixels;

        } else if (Build.VERSION.SDK_INT >= 14) {
            //reflection for this weird in-between time
            try {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                Method mGetRawW = Display.class.getMethod("getRawWidth");
                WIDTH_SCREEN = (Integer) mGetRawW.invoke(display);
                HEIGHT_SCREEN = (Integer) mGetRawH.invoke(display);
            } catch (Exception e) {
                //this may not be 100% accurate, but it's all we've got
                WIDTH_SCREEN = display.getWidth();
                HEIGHT_SCREEN = display.getHeight();
                Log.e("Display Info", "Couldn't use reflection to get the real display metrics.");
            }

        } else {
            //This should be close, as lower API devices should not have window navigation bars
            WIDTH_SCREEN = display.getWidth();
            HEIGHT_SCREEN = display.getHeight();
        }
    }

    public static MediaSource buildMediaSource(Uri uri, DefaultDataSourceFactory defaultDataSourceFactory,
                                        DataSource.Factory mediaDataSourceFactory, Handler mainHandler,
                                        String overrideExtension) {
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

    public static float convertDpToPixel(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, realMetrics);
    }

    public static void saveLastPositionVideoPlay(Context context, SimpleExoPlayer player, Video lastVideoPlayed, long duration, int windowIndex) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(LAST_VIDEO_SUSPENDED_POSITION, windowIndex);
        if(lastVideoPlayed.getType() == TYPE_LIVE_STREAMING) {
            long position = player.getCurrentPosition();
            Timeline.Period period = new Timeline.Period();
            Timeline currentTimeline = player.getCurrentTimeline();
            if (!currentTimeline.isEmpty()) {
                position -= currentTimeline.getPeriod(player.getCurrentPeriodIndex(), period)
                        .getPositionInWindowMs();
            }
            editor.putLong(LAST_TIME_VIDEO_SUSPENDED, position);
        } else {
            editor.putLong(LAST_VIDEO_DURATION_SUSPENDED, duration);
            editor.putLong(LAST_TIME_VIDEO_SUSPENDED, player.getCurrentPosition());
        }
        editor.apply();
    }

    public static String getToken(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
        return sharedPreferences.getString(Params.TOKEN, null);
    }

    public static void saveToken(Context context, String token) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Params.TOKEN, token);
        editor.apply();
    }

    public static boolean checkFileSize(File file, long realSize) {
        return file.exists() && file.length() >= realSize;
    }
}
