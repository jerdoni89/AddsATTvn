package com.attvn.adtruevn.manager;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.attvn.adtruevn.model.Playlist;
import com.attvn.adtruevn.model.Video;
import com.attvn.adtruevn.ui.CustomProgressBarView;
import com.attvn.adtruevn.ui.EventLogView;
import com.attvn.adtruevn.util.Global;
import com.attvn.adtruevn.util.Logging;
import com.attvn.adtruevn.util.Params;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by app on 12/28/16.
 */

public final class DownloadManager {
    private static final String TAG = "DownloadManager";

    private Context mContext;
    private File mMediaFolder;
    private File mAdsFolder;
    private Playlist mData;
    private List<Subscription> mListSubs;

    private CustomProgressBarView mProgressBar;
    private EventLogView mEventLogView;

    private PostDownloadExecution postDownloadExecution;

    private DownloadManager(Context context){
        this.mContext = context;
        this.mMediaFolder = new File(Global.FOLDER_PLAYLIST_DOWNLOADED.getAbsolutePath() + Params.LAST_SEGMENT_FOLDER_MEDIA);
        if(!mMediaFolder.exists()) {
            mMediaFolder.mkdirs();
        }

        this.mAdsFolder = new File(Global.FOLDER_PLAYLIST_DOWNLOADED.getAbsolutePath() + Params.LAST_SEGMENT_FOLDER_ADS);
        if(!mAdsFolder.exists()) {
            mAdsFolder.mkdirs();
        }

        mListSubs = new ArrayList<>();
    }

    public static DownloadManager initWith(Context context) {
        return new DownloadManager(context);
    }

    public void initData(Playlist playlist){
        this.mData = playlist;
    }

    public void setProgressBar(CustomProgressBarView customProgressBarView){
        this.mProgressBar = customProgressBarView;
    }

    public void setEventLogView(EventLogView eventLogView) {
        this.mEventLogView = eventLogView;
    }

    public void setPostDownloadExecution(PostDownloadExecution execution) {
        this.postDownloadExecution = execution;
    }

    public void release() {
        for(Subscription sub : mListSubs) {
            if(sub != null && sub.isUnsubscribed()) {
                sub.unsubscribe();
            }
        }
    }

    public void startDownloadProcess() {
        if(mData == null) {
            return;
        }

        downloadMediaFile();
    }

    private void downloadMediaFile() {
        final ArrayList<String> filePaths = new ArrayList<>();
        Observable<String> mediaDownloadObservable = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                long totalBytes = getTotalBytes(mData.getPlaylist());
                long currentBytesDownload = 0;
                for(Video video: mData.getPlaylist()) {
                    if (video.getType() == Params.TYPE_VIDEO) {
                        String type;
                        String[] section = Uri.parse(video.getVideo_url()).getLastPathSegment().split("\\.");
                        type = section[section.length - 1];
                        Log.d(TAG, type);

                        final String fileName = String.valueOf(video.getId()) + "." + type;
                        currentBytesDownload = downloadSingleUrl(video.getVideo_url(), fileName, currentBytesDownload,
                                totalBytes, subscriber, filePaths, mMediaFolder);
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
                postDownloadExecution.updateFilePath(mData.getId(), filePaths);
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
        mListSubs.add(mediaDownloadObservable.subscribe(subscriber));
    }

    private long downloadSingleUrl(String url, String fileName, long currentBytesDownload, long totalBytes
            , Subscriber<? super String> subscriber, List<String> filePaths, File folder) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        OkHttpClient httpClient = new OkHttpClient();
        try {
            okhttp3.Response response = httpClient.newCall(request).execute();
            final File outputFile = new File(folder, fileName);

            if (!outputFile.exists()) {
                BufferedInputStream stream = new BufferedInputStream(response.body().byteStream());
                byte[] buffer = new byte[8192];

                FileOutputStream fos = new FileOutputStream(outputFile);
                int len = 0;
                while ((len = stream.read(buffer)) != -1) {
                    currentBytesDownload += len;
                    final int percent = (int) (currentBytesDownload * 100L/ totalBytes);
                    updateProcess(percent);
                    fos.write(buffer, 0, len);
                }

                fos.flush();
                fos.close();
                Logging.log("[d] " + outputFile.getName() + " download success");
            } else {
                currentBytesDownload += response.body().contentLength();
                updateProcess((int) (currentBytesDownload * 100L/ totalBytes));
            }
            subscriber.onNext(outputFile.getAbsolutePath());
            filePaths.add(outputFile.getAbsolutePath());

            return currentBytesDownload;
        } catch (FileNotFoundException e) {
            Logging.log("[e] " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
        } catch (IOException e) {
            Logging.log("[e] " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
        } catch (OutOfMemoryError error) {
            Logging.log("[e] " + error.getClass().getSimpleName() + ": " + error.getLocalizedMessage());
        }
        return 0;
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
                        mProgressBar.setProgress(integer);
                    }
                });
        subscription.unsubscribe();
    }

    private long getTotalBytes(List<Video> videos) {
        long totalByte = 0;
        if(videos != null) {
            for (Video video : videos) {
                Request request = new Request.Builder()
                        .url(video.getVideo_url())
                        .head()
                        .build();

                OkHttpClient httpClient = new OkHttpClient();
                try {
                    okhttp3.Response response = httpClient.newCall(request).execute();
                    totalByte += response.body().contentLength();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            Logging.log("[d] total length: " + totalByte);
        }
        return totalByte;
    }

    public interface PostDownloadExecution {
        void updateFilePath(long playlistId, List<String> listFilePaths);
        void updateFileAdsPath(long playlistId, List<String> listFileAdsPaths);
    }
}
