package com.vincenthzr.locationspoofer.utils

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GithubUpdateSourceTest {
    private fun release(body: String = "## Changes\nFixed update downloads") = JSONObject().apply {
        put("tag_name", "v3.0.2")
        put("body", body)
        put("html_url", "https://github.com/rD227/LocationSpoofer/releases/tag/v3.0.2")
        put("assets", org.json.JSONArray().apply {
            for (name in listOf("app-global-arm64-v8a-release.apk", "app-global-armeabi-v7a-release.apk",
                "app-scoped-arm64-v8a-release.apk", "app-global-universal-clean-130-release.apk")) {
                put(JSONObject().put("name", name).put("browser_download_url", "https://github.com/rD227/LocationSpoofer/releases/download/v3.0.2/$name"))
            }
        })
    }

    @Test fun latestUsesBothExactGlobalAssetsAndReleaseNotes() {
        val result = GithubUpdateSource.parseRelease(release(), isLatest = true)
        assertEquals("${GithubUpdateSource.LATEST_PAGE}/download/app-global-arm64-v8a-release.apk", result.downloadUrl)
        assertEquals("${GithubUpdateSource.LATEST_PAGE}/download/app-global-armeabi-v7a-release.apk", result.downloadUrl32Bit)
        assertEquals("## Changes\nFixed update downloads", result.body)
        assertEquals("v3.0.2", result.versionName)
    }

    @Test fun historicalDownloadsRemainPinnedToTheirRelease() {
        val result = GithubUpdateSource.parseRelease(release())
        assertTrue(result.downloadUrl!!.contains("/download/v3.0.2/"))
        assertTrue(result.downloadUrl32Bit!!.contains("/download/v3.0.2/"))
    }

    @Test fun missingArm64DoesNotMislabelArm32As64Bit() {
        val obj = release()
        val assets = obj.getJSONArray("assets")
        assets.remove(0)
        val result = GithubUpdateSource.parseRelease(obj, isLatest = true)
        assertNull(result.downloadUrl)
        assertNotNull(result.downloadUrl32Bit)
    }

    @Test fun nullNotesAreEmptyAndKeepBrowserFallback() {
        val result = GithubUpdateSource.parseRelease(release().put("body", JSONObject.NULL))
        assertEquals("", result.body)
        assertEquals("https://github.com/rD227/LocationSpoofer/releases/tag/v3.0.2", result.htmlUrl)
    }

    @Test fun missingMetadataUsesLatestBrowserPageWithoutInventingAssets() {
        val result = GithubUpdateSource.parseRelease(JSONObject().put("tag_name", "v3.0.2"), isLatest = true)
        assertEquals("", result.body)
        assertEquals(GithubUpdateSource.LATEST_PAGE, result.htmlUrl)
        assertNull(result.downloadUrl)
        assertNull(result.downloadUrl32Bit)
    }

    private fun gitlabRelease(
        description: Any? = "## Changes\nGitLab release notes",
        releasedAt: String = "2026-05-01T12:00:00.000Z",
        upcomingRelease: Boolean = false,
        links: List<Pair<String, String>> = listOf(
            "app-global-arm64-v8a-release.apk" to "https://gitlab.com/xusu616/LocationSpoofer/uploads/arm64.apk",
            "app-global-armeabi-v7a-release.apk" to "https://gitlab.com/xusu616/LocationSpoofer/uploads/arm32.apk"
        ),
        selfUrl: String? = "https://gitlab.com/xusu616/LocationSpoofer/-/releases/v3.0.2"
    ) = JSONObject().apply {
        put("tag_name", "v3.0.2")
        if (description == null) {
            put("description", JSONObject.NULL)
        } else {
            put("description", description)
        }
        put("released_at", releasedAt)
        put("upcoming_release", upcomingRelease)
        if (selfUrl != null) {
            put("_links", JSONObject().put("self", selfUrl))
        }
        put("assets", JSONObject().apply {
            put("links", org.json.JSONArray().apply {
                for ((name, url) in links) {
                    put(JSONObject().put("name", name).put("url", url))
                }
            })
        })
    }

    @Test fun gitlabReleaseParsesAllFieldsCorrectly() {
        val result = GithubUpdateSource.parseGitLabRelease(gitlabRelease())
        assertEquals("v3.0.2", result.versionName)
        assertEquals("## Changes\nGitLab release notes", result.body)
        assertEquals("https://gitlab.com/xusu616/LocationSpoofer/uploads/arm64.apk", result.downloadUrl)
        assertEquals("https://gitlab.com/xusu616/LocationSpoofer/uploads/arm32.apk", result.downloadUrl32Bit)
        assertEquals("2026-05-01T12:00:00.000Z", result.publishedAt)
        assertFalse(result.isPrerelease)
        assertEquals("https://gitlab.com/xusu616/LocationSpoofer/-/releases/v3.0.2", result.htmlUrl)
    }

    @Test fun gitlabReleaseHandlesUpcomingRelease() {
        val result = GithubUpdateSource.parseGitLabRelease(gitlabRelease(upcomingRelease = true))
        assertTrue(result.isPrerelease)
    }

    @Test fun gitlabReleaseHandlesNullDescriptionAndMissingLinks() {
        val result = GithubUpdateSource.parseGitLabRelease(gitlabRelease(description = null, selfUrl = null))
        assertEquals("", result.body)
        assertEquals(GithubUpdateSource.GITLAB_RELEASES_PAGE, result.htmlUrl)
    }

    @Test fun gitlabReleaseHandlesMissingAssets() {
        val obj = JSONObject().put("tag_name", "v3.0.2")
        val result = GithubUpdateSource.parseGitLabRelease(obj)
        assertEquals("v3.0.2", result.versionName)
        assertEquals("", result.body)
        assertNull(result.downloadUrl)
        assertNull(result.downloadUrl32Bit)
        assertEquals(GithubUpdateSource.GITLAB_RELEASES_PAGE, result.htmlUrl)
    }
}
