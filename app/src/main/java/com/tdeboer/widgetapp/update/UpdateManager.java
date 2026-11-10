package com.tdeboer.widgetapp.update;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import com.tdeboer.widgetapp.BuildConfig;
import com.tdeboer.widgetapp.R;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// keeps track of the update for the whole app, so the bar survives turning the phone.
// everything here runs on the main thread, the network work runs on its own thread
public final class UpdateManager {
    private static final String TAG = "UpdateManager";
    private static final String APK_NAME = "update.apk";

    public enum State { NONE, AVAILABLE, NEEDS_PERMISSION, DOWNLOADING, INSTALLING, FAILED, DISMISSED }

//    the screen that shows the bar, only while it is on screen
    public interface Listener {
        void onUpdateChanged();
    }

    private static final Executor executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static boolean checked;
    private static State state = State.NONE;
    private static UpdateChecker.Release release;
    private static int progress;
    private static String error;
    private static Intent pendingConfirmation;
    private static Activity activity;
    private static Listener listener;

    private UpdateManager() { }

    public static State getState() {
        return state;
    }

    public static UpdateChecker.Release getRelease() {
        return release;
    }

    public static int getProgress() {
        return progress;
    }

    public static String getError() {
        return error;
    }

//    looks on github once every time the app starts
    public static void checkOnce(Context context) {
        if (checked) {
            return;
        }
        checked = true;
        Context appContext = context.getApplicationContext();
        executor.execute(() -> {
//            a download from last time is not needed anymore
            apkFile(appContext).delete();
            try {
                UpdateChecker.Release newer = UpdateChecker.findNewer(BuildConfig.VERSION_NAME);
                if (newer != null) {
                    mainHandler.post(() -> {
                        release = newer;
                        setState(State.AVAILABLE);
                    });
                }
            } catch (IOException | RuntimeException e) {
//                no internet or github is not reachable: try again next time, no bar
                Log.i(TAG, "Could not check for an update", e);
            }
        });
    }

    public static void attach(Activity screen, Listener listener) {
        activity = screen;
        UpdateManager.listener = listener;
//        coming back from the settings screen where installing apps can be allowed
        if (state == State.NEEDS_PERMISSION && canInstall(screen)) {
            download(screen);
        }
        showPendingConfirmation();
        listener.onUpdateChanged();
    }

    public static void detach() {
        activity = null;
        listener = null;
    }

    public static void dismiss() {
        setState(State.DISMISSED);
    }

    public static void startUpdate(Activity screen) {
        if (release == null) {
            return;
        }
        if (canInstall(screen)) {
            download(screen);
            return;
        }
        setState(State.NEEDS_PERMISSION);
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + screen.getPackageName()));
        try {
            screen.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            fail(screen.getString(R.string.update_failed_install));
        }
    }

    private static boolean canInstall(Context context) {
        return context.getPackageManager().canRequestPackageInstalls();
    }

    private static void download(Context context) {
        Context appContext = context.getApplicationContext();
        UpdateChecker.Release target = release;
        progress = 0;
        setState(State.DOWNLOADING);
        executor.execute(() -> {
            File apk = apkFile(appContext);
            try {
                UpdateInstaller.download(target, apk, percent -> mainHandler.post(() -> {
                    if (state == State.DOWNLOADING) {
                        progress = percent;
                        notifyListener();
                    }
                }));
                mainHandler.post(() -> setState(State.INSTALLING));
                UpdateInstaller.install(appContext, apk);
            } catch (IOException | RuntimeException e) {
                Log.w(TAG, "Update failed", e);
                boolean downloaded = apk.isFile();
                mainHandler.post(() -> fail(appContext.getString(
                        downloaded ? R.string.update_failed_install : R.string.update_failed_download)));
            }
        });
    }

    static void onConfirmationNeeded(Intent confirmation) {
        if (confirmation == null) {
            onInstallResult(PackageInstaller.STATUS_FAILURE, null);
            return;
        }
        pendingConfirmation = confirmation;
        showPendingConfirmation();
    }

//    the confirmation can only be opened from a screen that is in front, otherwise it waits until the app is opened again
    private static void showPendingConfirmation() {
        if (pendingConfirmation == null || activity == null) {
            return;
        }
        Intent confirmation = pendingConfirmation;
        pendingConfirmation = null;
        try {
            activity.startActivity(confirmation);
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "Could not ask to confirm the update", e);
            fail(activity.getString(R.string.update_failed_install));
        }
    }

    static void onInstallResult(int status, String message) {
        if (status == PackageInstaller.STATUS_SUCCESS) {
//            android closes the app to replace it, nothing left to do
            return;
        }
        if (status == PackageInstaller.STATUS_FAILURE_ABORTED) {
//            the user said no, so offer the update again
            setState(State.AVAILABLE);
            return;
        }
        Context context = activity;
        if (context == null) {
            error = null;
            setState(State.FAILED);
            return;
        }
        fail(message != null
                ? context.getString(R.string.update_failed_install_reason, message)
                : context.getString(R.string.update_failed_install));
    }

    private static void fail(String message) {
        error = message;
        setState(State.FAILED);
    }

    private static void setState(State newState) {
        state = newState;
        notifyListener();
    }

    private static void notifyListener() {
        if (listener != null) {
            listener.onUpdateChanged();
        }
    }

    private static File apkFile(Context context) {
        return new File(context.getCacheDir(), APK_NAME);
    }
}
