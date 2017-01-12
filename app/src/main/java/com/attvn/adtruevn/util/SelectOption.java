package com.attvn.adtruevn.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.util.JsonReader;
import android.util.Log;
import android.widget.Toast;

import com.attvn.adtruevn.R;
import com.attvn.adtruevn.model.AppInfo;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.Util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.attvn.adtruevn.SplashScreenActivity.REQUEST_SETTINGS;
import static com.bumptech.glide.gifdecoder.GifHeaderParser.TAG;

/**
 * Created by app on 12/31/16.
 */

public class SelectOption {

    public static void select(Context context, int option, List<Subscription> listSubs, ProgressDialog dialog) {
        switch (option) {
            case Params.OPTION_UPDATE:
                selectUpdate(context, listSubs, dialog);
                break;
            case Params.OPTION_SETTINGS:
                ((Activity) context).startActivityForResult(new Intent(Settings.ACTION_SETTINGS), REQUEST_SETTINGS);
                break;
            case Params.OPTION_OPEN_USB:
                openUsb(context);
                break;
            case Params.OPTION_GOOGLE_PLAY:
                openPlayStore(context);
                break;
        }
    }

    private static void selectUpdate(final Context context, final List<Subscription> listSubs, final ProgressDialog dialog) {
        Observable<AppInfo> getAppInfoObservable = Observable.create(new Observable.OnSubscribe<AppInfo>() {
            @Override
            public void call(Subscriber<? super AppInfo> subscriber) {
                String userAgent = Util.getUserAgent(context, context.getString(R.string.app_name));
                DataSource dataSource = new DefaultDataSource(context, null, userAgent, false);
                DataSpec dataSpec = new DataSpec(Uri.parse(Params.APPS_API_URL));
                InputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);

                try {
                    JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
                    reader.setLenient(true);
                    AppInfo appinfo = AppInfo.getAppInfo(reader);

                    subscriber.onNext(appinfo);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    subscriber.onError(ex);
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        Subscriber<AppInfo> subscriber = new Subscriber<AppInfo>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, e.getMessage());
                Toast.makeText(context, context.getString(R.string.notice_internet_access), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNext(AppInfo appInfo) {
                Logging.log("[a] " + appInfo.getFilename());
                Log.d(TAG, "[a] " + appInfo.getFilename());

                checkAndUpdateApp(context, appInfo, listSubs, dialog);
            }
        };
        listSubs.add(getAppInfoObservable.subscribe(subscriber));
    }

    private static void checkAndUpdateApp(final Context context, final AppInfo appInfo, final List<Subscription> listSubs,
                                          final ProgressDialog dialogDownload) {
        PackageManager manager = context.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(context.getPackageName(), 0);
            if(info.versionCode < appInfo.getVersion_code()) {
                new AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog_Alert)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(context.getString(R.string.update_new_version))
                        .setMessage(context.getString(R.string.update_new_version_notification))
                        .setPositiveButton(context.getString(R.string.yes_btn), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startUpdate(context, appInfo, listSubs, dialogDownload);
                            }

                        })
                        .setNegativeButton(context.getString(R.string.no_btn), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            } else {
                Toast.makeText(context, context.getString(R.string.no_update_requirement), Toast.LENGTH_LONG).show();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void startUpdate(final Context context, final AppInfo info, List<Subscription> listSubs, final ProgressDialog dialog) {
        final File apkFolder = new File(Global.FOLDER_PLAYLIST_DOWNLOADED.getAbsolutePath() + Params.LAST_SEGMENT_FOLDER_APK);
        if(!apkFolder.exists()) {
            apkFolder.mkdirs();
        }

        final String url = info.getFilename();
        final String filename = Uri.parse(url).getLastPathSegment();

        dialog.show();
        Observable<File> apkDownloadObservable = Observable.create(new Observable.OnSubscribe<File>() {
            @Override
            public void call(Subscriber<? super File> subscriber) {
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                OkHttpClient httpClient = new OkHttpClient();
                try {
                    okhttp3.Response response = httpClient.newCall(request).execute();
                    BufferedInputStream stream = new BufferedInputStream(response.body().byteStream());
                    byte[] buffer = new byte[8192];

                    final File outputFile = new File(apkFolder, filename);
                    if(outputFile.exists()) {
                        outputFile.delete();
                    }

                    FileOutputStream fos = new FileOutputStream(outputFile);
                    int progress = 0;
                    int len = 0;
                    while ((len = stream.read(buffer)) != -1) {
                        progress += len;
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();
                    fos.close();
                    subscriber.onNext(outputFile);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        Subscriber<File> subscriber = new Subscriber<File>() {
            @Override
            public void onCompleted() {
                dialog.dismiss();

                new AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog_Alert)
                    .setIcon(R.drawable.ic_system_update_white_48dp)
                    .setTitle(context.getString(R.string.update_information))
                    .setMessage(info.toString())
                    .setPositiveButton(context.getString(R.string.ok_btn), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }

                    })
                    .show();
            }

            @Override
            public void onError(Throwable e) {
                Logging.log("[e] " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
            }

            @Override
            public void onNext(File f) {
                final Uri uri;
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    uri = FileProvider.getUriForFile(context,
                            context.getApplicationContext().getPackageName() + ".provider", f);
                } else {
//                    uri = Uri.parse("file://" + apkFile.toString());
                    uri = Uri.fromFile(f);
                }

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(uri, "application/vnd.android.package-archive");
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ((Activity) context).startActivity(i);

                Logging.log("[u] " + "Update success");
            }
        };
        listSubs.add(apkDownloadObservable.subscribe(subscriber));
    }

    private static void openUsb(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage("com.cvte.tv.media");
        if (launchIntent != null) {
            ((Activity) context).startActivity(launchIntent);
        } else {
            Toast.makeText(context, "Package not found", Toast.LENGTH_SHORT).show();
        }
    }

    private static void openPlayStore(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage("com.android.vending");
        if (launchIntent != null) {
            ((Activity) context).startActivity(launchIntent);
        } else {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/")));
        }
    }
}
