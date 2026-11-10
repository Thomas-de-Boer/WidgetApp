package com.tdeboer.widgetapp.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

// asks github for the newest release and tells if it is newer than this app.
// a release is used when its tag is the version name with a v in front (v1.2) and it has an apk attached.
// the network part waits for github, so never call findNewer on the main thread
public final class UpdateChecker {
    private static final String LATEST_RELEASE_URL =
            "https://api.github.com/repos/Thomas-de-Boer/WidgetApp/releases/latest";
    static final String USER_AGENT = "WidgetApp-updater";
    static final int TIMEOUT_MILLIS = 15_000;
    private static final int MAX_JSON_BYTES = 1024 * 1024;

    private UpdateChecker() { }

    public static final class Release {
        public final String version;
        public final String apkUrl;
        public final long size;
        // lowercase hex, or null when github did not give one
        public final String sha256;

        Release(String version, String apkUrl, long size, String sha256) {
            this.version = version;
            this.apkUrl = apkUrl;
            this.size = size;
            this.sha256 = sha256;
        }
    }

    // the newest release when it is newer than currentVersion, otherwise null
    public static Release findNewer(String currentVersion) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_RELEASE_URL).openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("GitHub answered " + connection.getResponseCode());
            }
            String json;
            try (InputStream input = connection.getInputStream()) {
                json = readText(input);
            }
            return newerRelease(json, currentVersion);
        } finally {
            connection.disconnect();
        }
    }

    // the release from the github json when it is newer than currentVersion and has an apk, otherwise null
    static Release newerRelease(String json, String currentVersion) {
        try {
            JsonObject release = JsonParser.parseString(json).getAsJsonObject();
            if (isTrue(release, "draft") || isTrue(release, "prerelease")) {
                return null;
            }
            String tag = release.get("tag_name").getAsString();
            if (!isNewer(tag, currentVersion)) {
                return null;
            }
            for (JsonElement element : release.getAsJsonArray("assets")) {
                JsonObject asset = element.getAsJsonObject();
                String name = asset.get("name").getAsString();
                if (name.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                    return new Release(stripPrefix(tag), asset.get("browser_download_url").getAsString(),
                            asset.has("size") ? asset.get("size").getAsLong() : -1, sha256(asset));
                }
            }
            return null;
        } catch (RuntimeException e) {
            // not the json we expected, so act as if there is no update
            return null;
        }
    }

    // true when the tag is a higher version than current. tags that are not a version are never newer
    public static boolean isNewer(String tag, String current) {
        int[] tagVersion = parseVersion(tag);
        int[] currentVersion = parseVersion(current);
        if (tagVersion == null || currentVersion == null) {
            return false;
        }
        for (int i = 0; i < Math.max(tagVersion.length, currentVersion.length); i++) {
            int a = i < tagVersion.length ? tagVersion[i] : 0;
            int b = i < currentVersion.length ? currentVersion[i] : 0;
            if (a != b) {
                return a > b;
            }
        }
        return false;
    }

    // "v1.10.2" -> {1, 10, 2}, or null when it is not a version
    static int[] parseVersion(String version) {
        if (version == null) {
            return null;
        }
        String[] parts = stripPrefix(version.trim()).split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].matches("[0-9]{1,9}")) {
                return null;
            }
            numbers[i] = Integer.parseInt(parts[i]);
        }
        return numbers;
    }

    private static String stripPrefix(String version) {
        return version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
    }

    private static boolean isTrue(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && !element.isJsonNull() && element.getAsBoolean();
    }

    private static String sha256(JsonObject asset) {
        JsonElement digest = asset.get("digest");
        if (digest == null || digest.isJsonNull()) {
            return null;
        }
        String value = digest.getAsString();
        return value.startsWith("sha256:") ? value.substring("sha256:".length()).toLowerCase(Locale.ROOT) : null;
    }

    private static String readText(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (bytes.size() + read > MAX_JSON_BYTES) {
                throw new IOException("The answer from GitHub is too large");
            }
            bytes.write(buffer, 0, read);
        }
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }
}
