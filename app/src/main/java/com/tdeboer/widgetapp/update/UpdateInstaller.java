package com.tdeboer.widgetapp.update;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.function.IntConsumer;

// downloads the new apk and hands it to android. android replaces the app and keeps all its data,
// because the new apk is signed with the same key. both methods wait, so never call them on the main thread
final class UpdateInstaller {
    private UpdateInstaller() { }

    // progress gets the percentage every time it changes
    static void download(UpdateChecker.Release release, File target, IntConsumer progress) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(release.apkUrl).openConnection();
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }

        try {
            connection.setConnectTimeout(UpdateChecker.TIMEOUT_MILLIS);
            connection.setReadTimeout(UpdateChecker.TIMEOUT_MILLIS);
            connection.setRequestProperty("User-Agent", UpdateChecker.USER_AGENT);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Download answered " + connection.getResponseCode());
            }
            long total = release.size > 0 ? release.size : connection.getContentLengthLong();
            long written = 0;
            int lastPercent = -1;

            try (InputStream input = connection.getInputStream();
                 OutputStream output = new FileOutputStream(target)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    written += read;
                    if (total > 0 && written > total) {
                        throw new IOException("The download is larger than expected");
                    }
                    output.write(buffer, 0, read);
                    digest.update(buffer, 0, read);
                    int percent = total > 0 ? (int) (written * 100 / total) : 0;
                    if (percent != lastPercent) {
                        lastPercent = percent;
                        progress.accept(percent);
                    }
                }
            }

            if (total > 0 && written != total) {
                throw new IOException("The download is incomplete");
            }
            if (release.sha256 != null && !release.sha256.equals(toHex(digest.digest()))) {
                throw new IOException("The download is damaged");
            }
        } catch (IOException e) {
            target.delete();
            throw e;
        } finally {
            connection.disconnect();
        }
    }

    // the answer arrives in InstallResultReceiver
    static void install(Context context, File apk) throws IOException {
        PackageInstaller installer = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params =
                new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(context.getPackageName());
        params.setSize(apk.length());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            an app that updates itself may skip the confirmation on newer android versions,
//            android still asks when that is not allowed
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        }

        int sessionId = installer.createSession(params);
        boolean committed = false;
        try (PackageInstaller.Session session = installer.openSession(sessionId)) {
            try (InputStream input = new FileInputStream(apk);
                 OutputStream output = session.openWrite("update.apk", 0, apk.length())) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                session.fsync(output);
            }

//            android adds the result to this intent, so it has to be mutable
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }
            Intent result = new Intent(context, InstallResultReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, sessionId, result, flags);
            session.commit(pendingIntent.getIntentSender());
            committed = true;
        } catch (RuntimeException e) {
            throw new IOException(e);
        } finally {
            if (!committed) {
                installer.abandonSession(sessionId);
            }
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format(Locale.ROOT, "%02x", b));
        }
        return hex.toString();
    }
}
