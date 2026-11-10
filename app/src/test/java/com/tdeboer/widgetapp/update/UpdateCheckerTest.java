package com.tdeboer.widgetapp.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UpdateCheckerTest {

    @Test
    public void higherVersionIsNewer() {
        assertTrue(UpdateChecker.isNewer("v1.1", "1.0"));
        assertTrue(UpdateChecker.isNewer("v2.0", "1.9"));
        assertTrue(UpdateChecker.isNewer("1.0.1", "1.0"));
        assertTrue(UpdateChecker.isNewer("V1.1", "1.0"));
    }

    @Test
    public void numbersAreComparedAsNumbers() {
        assertTrue(UpdateChecker.isNewer("v1.10", "1.9"));
        assertFalse(UpdateChecker.isNewer("v1.9", "1.10"));
    }

    @Test
    public void sameOrLowerVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewer("v1.1", "1.1"));
        assertFalse(UpdateChecker.isNewer("v1.1.0", "1.1"));
        assertFalse(UpdateChecker.isNewer("v1.0", "1.1"));
    }

    @Test
    public void tagThatIsNoVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewer("WidgetApp", "1.0"));
        assertFalse(UpdateChecker.isNewer("v1.1-beta", "1.0"));
        assertFalse(UpdateChecker.isNewer("", "1.0"));
        assertFalse(UpdateChecker.isNewer(null, "1.0"));
    }

    private static final String RELEASE_JSON = "{"
            + "\"tag_name\": \"v1.2\", \"draft\": false, \"prerelease\": false,"
            + "\"assets\": ["
            + "  {\"name\": \"notes.txt\", \"size\": 10, \"browser_download_url\": \"https://example.com/notes.txt\"},"
            + "  {\"name\": \"app-release.apk\", \"size\": 1234,"
            + "   \"digest\": \"sha256:ABCDEF0123\","
            + "   \"browser_download_url\": \"https://example.com/app-release.apk\"}"
            + "]}";

    @Test
    public void newerReleaseUsesTheApk() {
        UpdateChecker.Release release = UpdateChecker.newerRelease(RELEASE_JSON, "1.1");
        assertNotNull(release);
        assertEquals("1.2", release.version);
        assertEquals("https://example.com/app-release.apk", release.apkUrl);
        assertEquals(1234, release.size);
        assertEquals("abcdef0123", release.sha256);
    }

    @Test
    public void currentReleaseGivesNoUpdate() {
        assertNull(UpdateChecker.newerRelease(RELEASE_JSON, "1.2"));
    }

    @Test
    public void releaseWithoutApkGivesNoUpdate() {
        String json = "{\"tag_name\": \"v9.0\", \"assets\": [{\"name\": \"source.zip\", \"browser_download_url\": \"x\"}]}";
        assertNull(UpdateChecker.newerRelease(json, "1.0"));
    }

    @Test
    public void prereleaseGivesNoUpdate() {
        String json = RELEASE_JSON.replace("\"prerelease\": false", "\"prerelease\": true");
        assertNull(UpdateChecker.newerRelease(json, "1.0"));
    }

    @Test
    public void brokenJsonGivesNoUpdate() {
        assertNull(UpdateChecker.newerRelease("not json", "1.0"));
        assertNull(UpdateChecker.newerRelease("{}", "1.0"));
    }
}
