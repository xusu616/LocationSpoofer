package com.vincenthzr.locationspoofer.utils

import com.vincenthzr.locationspoofer.data.model.GithubRelease
import org.json.JSONObject

object GithubUpdateSource {
    const val API_URL = "https://api.github.com/repos/rD227/LocationSpoofer/releases"
    const val LATEST_PAGE = "https://github.com/rD227/LocationSpoofer/releases/latest"
    const val GITLAB_API_URL = "https://gitlab.com/api/v4/projects/xusu616%2FLocationSpoofer/releases?per_page=5"
    const val GITLAB_RELEASES_PAGE = "https://gitlab.com/xusu616/LocationSpoofer/-/releases"
    private const val ARM64_APK = "app-global-arm64-v8a-release.apk"
    private const val ARM32_APK = "app-global-armeabi-v7a-release.apk"

    fun parseRelease(obj: JSONObject, isLatest: Boolean = false): GithubRelease {
        val assets = obj.optJSONArray("assets")
        fun downloadUrl(name: String): String? {
            for (i in 0 until (assets?.length() ?: 0)) {
                val asset = assets!!.getJSONObject(i)
                if (asset.optString("name") == name) {
                    return if (isLatest) "$LATEST_PAGE/download/$name"
                    else asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                }
            }
            return null
        }
        return GithubRelease(
            versionName = obj.getString("tag_name"),
            body = if (obj.isNull("body")) "" else obj.optString("body"),
            downloadUrl = downloadUrl(ARM64_APK),
            downloadUrl32Bit = downloadUrl(ARM32_APK),
            publishedAt = obj.optString("published_at"),
            isPrerelease = obj.optBoolean("prerelease"),
            htmlUrl = obj.optString("html_url").takeIf { it.isNotBlank() } ?: LATEST_PAGE
        )
    }

    fun parseGitLabRelease(obj: JSONObject): GithubRelease {
        val links = obj.optJSONObject("assets")?.optJSONArray("links")
        fun downloadUrl(name: String): String? {
            if (links == null) return null
            for (i in 0 until links.length()) {
                val link = links.optJSONObject(i) ?: continue
                if (link.optString("name") == name) {
                    return link.optString("url").takeIf { it.isNotBlank() }
                }
            }
            return null
        }
        val selfUrl = obj.optJSONObject("_links")?.optString("self")?.takeIf { it.isNotBlank() }
        return GithubRelease(
            versionName = obj.getString("tag_name"),
            body = if (obj.isNull("description")) "" else obj.optString("description"),
            downloadUrl = downloadUrl(ARM64_APK),
            downloadUrl32Bit = downloadUrl(ARM32_APK),
            publishedAt = obj.optString("released_at"),
            isPrerelease = obj.optBoolean("upcoming_release"),
            htmlUrl = selfUrl ?: GITLAB_RELEASES_PAGE
        )
    }
}
