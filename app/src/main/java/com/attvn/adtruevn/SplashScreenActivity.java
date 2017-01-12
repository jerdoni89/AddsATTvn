package com.attvn.adtruevn;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.attvn.adtruevn.model.AdvertInfo;
import com.attvn.adtruevn.model.AuthInfo;
import com.attvn.adtruevn.model.DeviceInfo;
import com.attvn.adtruevn.model.Playlist;
import com.attvn.adtruevn.model.PlaylistStack;
import com.attvn.adtruevn.model.SessionInfo;
import com.attvn.adtruevn.model.Video;
import com.attvn.adtruevn.net.ClientBuilder;
import com.attvn.adtruevn.net.WebService;
import com.attvn.adtruevn.ui.CustomProgressBarView;
import com.attvn.adtruevn.ui.EventLogView;
import com.attvn.adtruevn.ui.OptionsView;
import com.attvn.adtruevn.util.Global;
import com.attvn.adtruevn.util.Logging;
import com.attvn.adtruevn.util.Params;
import com.attvn.adtruevn.util.SelectOption;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class SplashScreenActivity extends Activity implements
        OptionsView.OnItemClickListener {
    private static final String TAG = "SplashScreenActivity";

    private static final int REQUEST_TIMEOUT = 10000;
    private static final int SPLASH_TIME = 3000;
    private static final int START_ANIM = 1;
    private static final int STOP_ANIM = 0;

    public static final int REQUEST_PERMISSION = 100;
    public static final int REQUEST_SETTINGS = 101;

    private static int WIDTH_EVENT_LOG_VIEW;
    private static int HEIGHT_EVENT_LOG_VIEW;
    private DisplayMetrics realMetrics;

    private FrameLayout mFrameLayout;
    private EventLogView mEventLogView;
    private OptionsView mOptionsView;
    private CustomProgressBarView progressBar;
    private ProgressDialog dialogDownloading;

    private ArrayList<Subscription> mListSubscription;

    private Realm realm;
    private PlaylistStack playlistStack;
//    private FFmpegController ffmpegController;

    private ClientBuilder mClientBuilder;
    private boolean hasData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.getScreenSize(this);

        WIDTH_EVENT_LOG_VIEW = (int) Global.WIDTH_SCREEN;
        HEIGHT_EVENT_LOG_VIEW = (int) Global.HEIGHT_SCREEN - (int) Global.convertDpToPixel(45);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Display display = getWindowManager().getDefaultDisplay();
        this.realMetrics = new DisplayMetrics();
        display.getRealMetrics(realMetrics);

        RealmConfiguration configuration = new RealmConfiguration
                .Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(configuration);
        realm = Realm.getDefaultInstance();

        setContentView(R.layout.activity_splash_screen);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        }
        mFrameLayout = (FrameLayout) findViewById(R.id.activity_splash_screen);

        initAnimationView();
        initEventLogView();
        initOptionsView();
        initProgressBar();
        makePlaylistFolder();

        dialogDownloading = new ProgressDialog(this);
        dialogDownloading.setMessage("Downloading ...");
        dialogDownloading.setCancelable(false);
        dialogDownloading.setMax(100);
        dialogDownloading.hide();

//        ffmpegController = FFmpegController.getInstance();
//        ffmpegController.setListEventListener(this);
//        ffmpegController.setEventLogView(mEventLogView);

        mListSubscription = new ArrayList<>();
        mClientBuilder = ClientBuilder.getInstance();
        hasData = checkDataExists();
        initializeData();
    }

    private void initializeData() {
        Observable<Boolean> checkInternetObservable = Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                if(checkInternet()){
                    Logging.log("[i] Connected To Server");
                    subscriber.onNext(true);
                } else {
                    Logging.log("[i] Disconnected From Server");
                    subscriber.onNext(false);
                    subscriber.onError(new Exception());
                }
            }
        });
        mListSubscription.add(checkInternetObservable
                                .retryWhen(new RetryConnectWithDelay(5000))
                                .subscribeOn(Schedulers.computation())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Action1<Boolean>() {
                                    @Override
                                    public void call(Boolean reachable) {
                                        if(reachable) {
                                            mEventLogView.append("[i] Connected To Server");
                                            // v2.0.4
                                            startRetrieveDataWithoutCookie();

                                            // v2.1.2
//                                            retrieveDataWithAuthorization();
                                        } else {
                                            mEventLogView.append("[i] Disconnected From Server");
                                        }
                                    }
                                }));
    }



    private class RetryConnectWithDelay implements Func1<Observable<? extends Throwable>, Observable<?>> {

        private final int retryDelayMillis;

        RetryConnectWithDelay(final int retryDelayMillis) {
            this.retryDelayMillis = retryDelayMillis;
        }

        @Override
        public Observable<?> call(Observable<? extends Throwable> attemps) {
            return attemps.flatMap(new Func1<Throwable, Observable<?>>() {
                @Override
                public Observable<?> call(Throwable throwable) {
                    if(!checkInternet()) {
                        Logging.log("[i] Disconnected From Server");
                        if(hasData) {
                            startOfflineMode();
                        } else {
                            return Observable.timer(retryDelayMillis, TimeUnit.MILLISECONDS);
                        }
                    }
                    return Observable.error(throwable);
                }
            });
        }
    }

    private boolean checkDataExists() {
        RealmResults<PlaylistStack> realmResults = realm.where(PlaylistStack.class).findAll();
        if(realmResults.size() > 0) {
            Playlist playlist = realmResults.first().playlistStack.get(0);
            if(playlist != null) {
                return checkFilesExists(playlist);
            }
        }
        return false;
    }

    private boolean checkFilesExists(Playlist playlist) {
        for(Video video: playlist.getPlaylist()) {
            if(video.getFilePath() != null) {
                File videoFile = new File(video.getFilePath());
                if (!videoFile.exists()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkInternet() {
        try {
            return InetAddress.getByName(Params.IP_SERVER).isReachable(REQUEST_TIMEOUT);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private void startOfflineMode() {
        Logging.log("[i] Start offline mode");
        mEventLogView.append("[i] Start offline mode");
        redirectToMainView(true);
    }

    private void retrieveDataWithAuthorization() {
        final WebService webService = mClientBuilder.createApplicationWebService();
        RealmResults<DeviceInfo> rs = realm.where(DeviceInfo.class).findAll();
        DeviceInfo deviceInfo;
        if(rs.size() > 0 || Global.getToken(SplashScreenActivity.this) != null) {
            deviceInfo = rs.first();

            AuthInfo authInfo =  new AuthInfo();
            authInfo.setName(deviceInfo.getMac());
            authInfo.setPass(Global.getToken(SplashScreenActivity.this));
            login(webService, authInfo);
        } else {
            realm.beginTransaction();
            deviceInfo = realm.createObject(DeviceInfo.class, getDeviceId());
            deviceInfo.setMac(getMacAddress());
            realm.commitTransaction();

            deviceInfo = realm.copyFromRealm(deviceInfo, 0);
            mListSubscription.add(webService.register(deviceInfo)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<DeviceInfo>() {
                        @Override
                        public void onCompleted() {
                            mEventLogView.append("[r] register successfully");
                            Logging.log("[r] register successfully");
                        }

                        @Override
                        public void onError(Throwable e) {
                            Logging.log("[e] " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
                        }

                        @Override
                        public void onNext(final DeviceInfo deviceInfo) {
                            Logging.log("[r] result: " + deviceInfo.getToken());
                            Global.saveToken(SplashScreenActivity.this, deviceInfo.getToken());
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    realm.copyToRealmOrUpdate(deviceInfo);
                                }
                            });

                            AuthInfo authInfo =  new AuthInfo();
                            authInfo.setName(getMacAddress());
                            authInfo.setPass(deviceInfo.getToken());
                            login(webService, authInfo);
                        }
                    }));
        }
    }

    private void login(WebService webService, AuthInfo authInfo) {
        mListSubscription.add(webService.login(authInfo)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<SessionInfo>() {
                    @Override
                    public void onCompleted() {
                        mEventLogView.append("[r] logged in");
                        Logging.log("[r] logged in");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Logging.log("[e] " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
                    }

                    @Override
                    public void onNext(SessionInfo sessionInfo) {
                        String cookie = sessionInfo.getSession_name() + "=" + sessionInfo.getSessid();
                        startRetrieveData(cookie);
                    }
                })
        );
    }

    private String getDeviceId() {
        String deviceId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        Logging.log("[a] Device Id: " + deviceId);
        return deviceId;
    }

    private String getMacAddress() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        String mac = wInfo.getMacAddress();
        String [] splitMac = mac.split(":");

        StringBuilder sb = new StringBuilder();
        for(String sp: splitMac) {
            sb.append(sp.trim());
        }

        Logging.log("[a] Mac address: " + mac);
        return sb.toString();
    }

    private void startRetrieveDataWithoutCookie() {
        Logging.log("[i] Start initialize data");
        mEventLogView.append("[i] Start initialize data");

        Subscription subs = mClientBuilder.createApplicationWebService().getPlaylist()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Playlist>() {
                    @Override
                    public void onCompleted() {
                        Logging.log("[i] Initialize data complete");
                        mEventLogView.append("[i] Initialize data complete");
                        mEventLogView.append("[d] Downloading ...");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Logging.log("[e] " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
                    }

                    @Override
                    public void onNext(Playlist playlist) {
                        if(playlist != null) {
                            updatePlaylistToLocalDatabase(playlist);
                            executeDownloadProcess(playlist);
                        }
                    }
                });
        mListSubscription.add(subs);
    }

    private void startRetrieveData(final String cookie) {
        Logging.log("[i] Start initialize data");
        mEventLogView.append("[i] Start initialize data");

        Subscription subs = mClientBuilder.createApplicationWebServiceWithCookieHeader(cookie).getPlaylist()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Playlist>() {
                    @Override
                    public void onCompleted() {
                        Logging.log("[i] Initialize data complete");
                        mEventLogView.append("[i] Initialize data complete");
                        mEventLogView.append("[d] Downloading ...");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Logging.log("[e] " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
                    }

                    @Override
                    public void onNext(Playlist playlist) {
                        if(playlist != null) {
                            updatePlaylistToLocalDatabase(playlist);
                            executeDownloadProcess(playlist);
                        }
                    }
                });
        mListSubscription.add(subs);
    }

    private long currentBytesDownload;
    private synchronized void executeDownloadProcess(final Playlist playlist) {
        final HashMap<String, List<String>> urlMapKey = new LinkedHashMap<>();
        Observable<String> observableDownload = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                // create media folder
                File mediaFolder = new File(Global.FOLDER_PLAYLIST_DOWNLOADED.getAbsolutePath() + Params.LAST_SEGMENT_FOLDER_MEDIA);
                if(!mediaFolder.exists()) {
                    mediaFolder.mkdirs();
                }

                long totalBytes = getTotalBytes(playlist.getPlaylist());
                currentBytesDownload = 0;
                for(Video video: playlist.getPlaylist()) {
                    Request request = new Request.Builder()
                            .url(video.getVideo_url())
                            .build();

                    OkHttpClient httpClient = new OkHttpClient();
                    try {
                        okhttp3.Response response = httpClient.newCall(request).execute();
                        String type;
                        if (video.getType() == Params.TYPE_IMAGE) {
                            type = "mp4";
                        } else {
                            String[] section = Uri.parse(video.getVideo_url()).getLastPathSegment().split("\\.");
                            type = section[section.length - 1];
                        }
                        Log.d(TAG, type);

                        final String fileName = String.valueOf(video.getId()) + "." + type;
                        final File outputFile = new File(mediaFolder, fileName);

                        if (!outputFile.exists() || !Global.checkFileSize(outputFile, response.body().contentLength())) {
                            if (video.getType() == Params.TYPE_VIDEO) {
                                BufferedInputStream stream = new BufferedInputStream(response.body().byteStream());
                                byte[] buffer = new byte[8192];

                                FileOutputStream fos = new FileOutputStream(outputFile);
                                int len = 0;
                                while ((len = stream.read(buffer)) != -1) {
                                    currentBytesDownload += len;
                                    int percent = (int) (currentBytesDownload * 100L/ totalBytes);
                                    updateProcess(percent);
                                    if(1.0f* (currentBytesDownload * 100L/ totalBytes) % 5 == 0) {
                                        Logging.log("[d] download process: " + percent + "%");
                                    }
                                    fos.write(buffer, 0, len);
                                }

                                fos.flush();
                                fos.close();
                                Logging.log("[d] " + outputFile.getName() + " download success");
                            }
                        } else {
                            currentBytesDownload += response.body().contentLength();
                            updateProcess((int) (currentBytesDownload * 100L/ totalBytes));
                        }
                        String videoPath = outputFile.getAbsolutePath();
                        subscriber.onNext(videoPath);

                        List<String> listAds = new ArrayList<>();
                        if(video.getAds() != null) {
                            for (AdvertInfo advertInfo : video.getAds()) {
                                String filePath = downloadAdsData(advertInfo, totalBytes);
                                if (filePath != null)
                                    listAds.add(filePath);
                            }
                        }
                        urlMapKey.put(videoPath, listAds);
                    } catch (FileNotFoundException e) {
                        Logging.log("[e] " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
                    } catch (IOException e) {
                        Logging.log("[e] " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
                    } catch (OutOfMemoryError error) {
                        Logging.log("[e] " + error.getClass().getSimpleName() + ": " + error.getLocalizedMessage());
                    }
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        Subscriber<String> subscriber = new Subscriber<String>() {
            @Override
            public void onCompleted() {
                mEventLogView.append("[d] Download process finish");
                Logging.log("[d] Download process finish");
                updateFilePath(playlist.getId(), urlMapKey);
            }

            @Override
            public void onError(Throwable e) {
                Logging.log("[e] " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
            }

            @Override
            public void onNext(String path) {
                File f = new File(path);
                if(f.exists()) {
                    Logging.log("[d] download process: " + path + " success");
                    mEventLogView.append("[d] download process: " + f.getName() + " success");
                }
            }
        };
        mListSubscription.add(observableDownload.subscribe(subscriber));
    }

    private synchronized String downloadAdsData(AdvertInfo adsInfo, long total) {
        if(adsInfo.getType() == AdvertInfo.TypeAds.TYPE_TEXT_SCROLL) {
            return null;
        }

        File adsFolder = new File(Global.FOLDER_PLAYLIST_DOWNLOADED.getAbsolutePath() + Params.LAST_SEGMENT_FOLDER_ADS);
        if(!adsFolder.exists()) {
            adsFolder.mkdirs();
        }

        Request request = new Request.Builder()
                .url(adsInfo.getContent())
                .build();

        OkHttpClient httpClient = new OkHttpClient();
        try {
            okhttp3.Response response = httpClient.newCall(request).execute();
            String type;
            String[] section = Uri.parse(adsInfo.getContent()).getLastPathSegment().split("\\.");
            type = section[section.length - 1];
            Log.d(TAG, type);

            final String fileName = String.valueOf(adsInfo.getAds_id()) + "." + type;
            final File outputFile = new File(adsFolder, fileName);

            if (!outputFile.exists() || !Global.checkFileSize(outputFile, response.body().contentLength())) {
                BufferedInputStream stream = new BufferedInputStream(response.body().byteStream());
                byte[] buffer = new byte[8192];

                FileOutputStream fos = new FileOutputStream(outputFile);
                int len = 0;
                while((len = stream.read(buffer)) != -1) {
                    currentBytesDownload += len;
                    int percent = (int) (currentBytesDownload * 100L/ total);
                    updateProcess(percent);
                    if(1.0f* (currentBytesDownload * 100L/ total) % 5 == 0) {
                        Logging.log("[d] download process: " + percent + "%");
                    }
                    fos.write(buffer, 0, len);
                }

                fos.flush();
                fos.close();
            } else {
                currentBytesDownload += response.body().contentLength();
                updateProcess((int) (currentBytesDownload * 100L/ total));
            }
            return outputFile.getAbsolutePath();
        } catch (IOException e) {
            Logging.log("[e] " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
        }
        return null;
    }

    private void updateProcess(int process) {
        Subscription subscription = Observable.just(process)
                .observeOn(Schedulers.immediate())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Logging.log("[e] " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
                    }

                    @Override
                    public void onNext(Integer integer) {
                        progressBar.setProgress(integer);
                    }
                });
        subscription.unsubscribe();
    }

    private long getTotalBytes(List<Video> videos) {
        long totalByte = 0;
        for (Video video: videos) {
            totalByte += getBytesFromUrl(video.getVideo_url());
            for(AdvertInfo advertInfo: video.getAds()) {
                if(advertInfo.getType() == AdvertInfo.TypeAds.TYPE_TEXT_SCROLL) continue;
                totalByte += getBytesFromUrl(advertInfo.getContent());
            }
        }
        Logging.log("[d] total length: " + totalByte);
        return totalByte;
    }

    private long getBytesFromUrl(String url) {
        Request request = new Request.Builder()
                .url(url)
                .head()
                .build();

        OkHttpClient httpClient = new OkHttpClient();
        try {
            okhttp3.Response response = httpClient.newCall(request).execute();
            return response.body().contentLength();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void checkPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
        }, REQUEST_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions is allowed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void initAnimationView() {
        ImageView gifImageView = new ImageView(this);
        FrameLayout.LayoutParams gifParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        gifParams.gravity = Gravity.CENTER;
        gifImageView.setLayoutParams(gifParams);
        mFrameLayout.addView(gifImageView);

//        gifImageView.setImageResource(R.drawable.reindeer);
        GlideDrawableImageViewTarget imageViewTarget = new GlideDrawableImageViewTarget(gifImageView);

        Glide.with(this)
                .load(R.drawable.anim)
                .crossFade()
                .into(imageViewTarget);


//        GifImageView gifImageView = new GifImageView(this);
//        FrameLayout.LayoutParams gifParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT);
//        gifParams.gravity = Gravity.CENTER;
//        gifImageView.setLayoutParams(gifParams);
//        GifDrawable gifFromAssets = null;
//        try {
//            gifFromAssets = new GifDrawable(getAssets(), "anim.gif");
//            gifImageView.setImageDrawable(gifFromAssets);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        mFrameLayout.addView(gifImageView);
    }

    private void initEventLogView() {
        mEventLogView = new EventLogView(this);
        FrameLayout.LayoutParams eventLogParams = new FrameLayout.LayoutParams(WIDTH_EVENT_LOG_VIEW, HEIGHT_EVENT_LOG_VIEW);
        eventLogParams.gravity = Gravity.START | Gravity.BOTTOM;
        eventLogParams.bottomMargin = (int) Global.convertDpToPixel(45);
        mEventLogView.setLayoutParams(eventLogParams);
        mFrameLayout.addView(mEventLogView);
    }

    private void initProgressBar() {
        progressBar = new CustomProgressBarView(this);
        FrameLayout.LayoutParams pgParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        pgParams.gravity = Gravity.BOTTOM;
        progressBar.setLayoutParams(pgParams);
        mFrameLayout.addView(progressBar);
    }

    private void initOptionsView() {
        mOptionsView = new OptionsView(this);
        FrameLayout.LayoutParams opParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        opParams.gravity = Gravity.END | Gravity.TOP;
        opParams.setMargins(0, 16, 16, 0);
        mOptionsView.setLayoutParams(opParams);
        mOptionsView.setFocusable(true);
        mFrameLayout.addView(mOptionsView);
        mOptionsView.setOnItemClickListener(this);
    }

    private void makePlaylistFolder(){
        String playlistLocation = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        if(Global.FOLDER_PLAYLIST_DOWNLOADED == null) {
            Global.FOLDER_PLAYLIST_DOWNLOADED = new File(playlistLocation);
            if(!Global.FOLDER_PLAYLIST_DOWNLOADED.exists())
                Global.FOLDER_PLAYLIST_DOWNLOADED.mkdirs();
        }
    }



    private void redirectToMainView(boolean offlineMode) {
        for(Subscription subscription: mListSubscription) {
            if (subscription != null && !subscription.isUnsubscribed()) {
                subscription.unsubscribe();
            }
        }

        Intent intent = new Intent(this, VideoMediaPlayer.class);
        if (offlineMode)
            intent.putExtra(VideoMediaPlayer.OFFLINE_MODE, offlineMode);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private synchronized void updatePlaylistToLocalDatabase(Playlist playlist) {
        realm.beginTransaction();
        RealmResults<PlaylistStack> realmResults = realm.where(PlaylistStack.class).findAll();

        if (realmResults.size() <= 0) {
            playlistStack = new PlaylistStack();
            playlistStack.push(playlist);
            realm.copyToRealm(playlistStack);
        } else {
            playlistStack = realmResults.first();
            // update playlist info
            realm.copyToRealmOrUpdate(playlist);

            // push playlist to stack
            RealmResults<Playlist> c = playlistStack.playlistStack.where().equalTo("id", playlist.getId()).findAll();
            if(c.size() <= 0) {
                playlistStack.push(playlist);
            }

            // delete last playlist if stack size > 3
            if(playlistStack.playlistStack.size() >= 3) {
                playlistStack.deleteLastItem();
            }
        }
        realm.commitTransaction();
    }

    public void updateFilePath(long playlistId, final HashMap<String, List<String>> mapUrl) {
        if(mapUrl.size() > 0) {
            RealmResults<PlaylistStack> realmResults = realm.where(PlaylistStack.class).findAll();
            final Playlist playlist = realmResults.first().playlistStack.where().equalTo("id", playlistId).findFirst();

            String [] urlVideos = new String[mapUrl.size()];
            urlVideos = mapUrl.keySet().toArray(urlVideos);

            if(urlVideos.length < playlist.getPlaylist().size()) {
                initializeData();
                return;
            }

            realm.beginTransaction();
            int i = 0;
            for (final Video video : playlist.getPlaylist()) {
                if (video.getType() == Params.TYPE_LIVE_STREAMING) continue;
                final String videoKey = urlVideos[i];
                video.setFilePath(videoKey);

                List<String> adUrls = mapUrl.get(videoKey);
                int j = 0;
                if(adUrls.size() > 0) {
                    for (final AdvertInfo advertInfo : video.getAds()) {
                        if (advertInfo.getType() == AdvertInfo.TypeAds.TYPE_TEXT_SCROLL) continue;
                        advertInfo.setFile_path(adUrls.get(j));
                        j++;
                    }
                }
                i++;
            }
            realm.commitTransaction();
            redirectToMainView(false);
        }
    }

    // check and convert image files to video mp4
//    private void checkPlaylist(Playlist playlist) {
//        if(playlist != null) {
//            ArrayList<Video> imageVideos = new ArrayList<>();
//            for(Video video: playlist.getPlaylist()) {
//                if(video.getType() == Params.TYPE_IMAGE) {
//                    imageVideos.add(video);
//                }
//            }
//
//            if(imageVideos.size() > 0) {
//                ffmpegController.executeConvertListImages(imageVideos);
//            } else {
//                redirectToMainView(false);
//            }
//        }
//    }
//
//    @Override
//    public void onExecuteDone(final ArrayList<Video> videos, final ArrayList<String> filePaths) {
//        realm.executeTransaction(new Realm.Transaction() {
//            @Override
//            public void execute(Realm realm) {
//                int i = 0;
//                for(Video video: videos) {
//                    Video v = realm.where(Video.class).equalTo("id", video.getId()).findFirst();
//                    v.setFilePath(filePaths.get(i));
//                    i++;
//                }
//            }
//        });
//        redirectToMainView(false);
//    }
//
//    @Override
//    public void onExistFiles() {
//        redirectToMainView(false);
//    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.exit_app_notification))
                .setMessage(getString(R.string.exit_app_question, getString(R.string.app_name)))
                .setPositiveButton(getString(R.string.yes_btn), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishAffinity();
                    }

                })
                .setNegativeButton(getString(R.string.no_btn), null)
                .show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mOptionsView.requestFocus();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Logging.log("[p] key pressed: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                onBackPressed();
                return true;
        }
        return false;
    }

    @Override
    public void onClickOptionItem(final int position) {
        SelectOption.select(SplashScreenActivity.this, position, mListSubscription, dialogDownloading);
    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SETTINGS:
                Log.d(TAG, "settings change");
//                if(isOnline())
//                    initializeData();
                break;
        }
    }
}
