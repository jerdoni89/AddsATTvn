package com.attvn.adtruevn.util;

/**
 * Created by app on 12/9/16.
 */

public final class Params {
    public static final String IP_SERVER = "adtrue.vn";
    public static final String BASE_URL = "http://" + IP_SERVER;
    public static final String BASE_API_URL = BASE_URL + "/api/";

    public static final String PLAYLIST_API_URL = "http://adtrue.vn/api/playlist.json";
    public static final String APPS_API_URL = "http://adtrue.vn/api/apps.json";

//    public static final String IP_AMQP_SERVER = "124.158.4.224";
    public static final String IP_AMQP_SERVER = "192.168.100.8";
    public static final String PLAY = "play";
    public static final String PAUSE = "pause";
    public static final String REWIND = "rewind";
    public static final String FASTFORWARD = "fast-forward";

    public static final int TYPE_VIDEO = 1;
    public static final int TYPE_LIVE_STREAMING = 2;
    public static final int TYPE_IMAGE = 3;
    public static final int TYPE_HTML5_VIDEO = 4;

    public static final String TOKEN = "token";

    public static final String LAST_SEGMENT_FOLDER_MEDIA = "/media";
    public static final String LAST_SEGMENT_FOLDER_ADS = "/ads";
    public static final String LAST_SEGMENT_FOLDER_APK = "/apk";

    public static final int OPTION_UPDATE = 0;
    public static final int OPTION_SETTINGS = 1;
    public static final int OPTION_OPEN_USB = 2;
    public static final int OPTION_GOOGLE_PLAY = 3;
}
