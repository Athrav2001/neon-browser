package com.neo.downloader.android.pages.browser.bookmark

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neo.downloader.android.storage.BrowserBookmark
import com.neo.downloader.shared.ui.widget.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

@Composable
fun BookmarkFavicon(
    bookmark: BrowserBookmark,
    size: Dp = 24.dp,
) {
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, bookmark.url) {
        value = loadBookmarkIcon(bookmark.url)
    }
    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap!!,
            contentDescription = null,
            modifier = Modifier.size(size),
        )
    } else {
        val label = bookmark.title.ifBlank { bookmark.url }
            .trim()
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: "•"
        Text(label, fontWeight = FontWeight.Bold)
    }
}

private suspend fun loadBookmarkIcon(rawUrl: String): ImageBitmap? = withContext(Dispatchers.IO) {
    val normalized = normalizeUrl(rawUrl)
    val host = runCatching { Uri.parse(normalized).host }
        .getOrNull()
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }
        ?: return@withContext null

    BookmarkFaviconCache[host]?.let { return@withContext it }

    val resolved = resolveBestIcon(normalized, host)
    BookmarkFaviconCache[host] = resolved
    resolved
}

private fun normalizeUrl(rawUrl: String): String {
    return when {
        rawUrl.startsWith("http://", ignoreCase = true) || rawUrl.startsWith("https://", ignoreCase = true) -> rawUrl
        else -> "https://$rawUrl"
    }
}

private fun resolveBestIcon(baseUrl: String, host: String): ImageBitmap? {
    val candidates = LinkedHashMap<String, Int>()

    fun register(url: String?, score: Int) {
        if (url.isNullOrBlank()) return
        val current = candidates[url]
        if (current == null || score > current) {
            candidates[url] = score
        }
    }

    val htmlResult = fetchText(baseUrl)
    if (htmlResult != null) {
        val finalPageUrl = htmlResult.first
        val html = htmlResult.second

        val parsed = parseHtmlIconCandidates(finalPageUrl, html)
        parsed.iconCandidates.forEach { (url, score) -> register(url, score) }

        val manifestUrl = parsed.manifestUrl
        if (manifestUrl != null) {
            val manifestText = fetchText(manifestUrl)?.second
            if (!manifestText.isNullOrBlank()) {
                parseManifestCandidates(manifestUrl, manifestText).forEach { (url, score) ->
                    register(url, score)
                }
            }
        }
    }

    register("https://$host/favicon.ico", 150)
    register("http://$host/favicon.ico", 140)
    register("https://www.google.com/s2/favicons?domain=$host&sz=128", 50)

    val ordered = candidates.entries.sortedByDescending { it.value }
    for ((url, _) in ordered) {
        fetchImage(url)?.let { return it }
    }
    return null
}

private data class HtmlParseResult(
    val manifestUrl: String?,
    val iconCandidates: List<Pair<String, Int>>,
)

private fun parseHtmlIconCandidates(baseUrl: String, html: String): HtmlParseResult {
    val linkTagRegex = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
    val attrRegex = Regex("""([a-zA-Z_:][-a-zA-Z0-9_:.]*)\s*=\s*("([^"]*)"|'([^']*)'|([^\s"'=<>`]+))""")

    var manifestUrl: String? = null
    val icons = mutableListOf<Pair<String, Int>>()

    for (tag in linkTagRegex.findAll(html)) {
        val attrs = mutableMapOf<String, String>()
        for (attrMatch in attrRegex.findAll(tag.value)) {
            val key = attrMatch.groupValues[1].lowercase()
            val value = attrMatch.groupValues[3].ifBlank {
                attrMatch.groupValues[4].ifBlank { attrMatch.groupValues[5] }
            }
            attrs[key] = value
        }

        val rel = attrs["rel"]?.lowercase() ?: continue
        val href = attrs["href"] ?: continue
        val resolvedHref = resolveUrl(baseUrl, href) ?: continue

        if (rel.contains("manifest")) {
            manifestUrl = resolvedHref
            continue
        }

        if (!rel.contains("icon")) continue

        val sizes = parseSizes(attrs["sizes"])
        val type = attrs["type"]?.lowercase().orEmpty()
        val purpose = attrs["purpose"]?.lowercase().orEmpty()
        val isApple = rel.contains("apple-touch-icon")
        val score = (if (isApple) 450 else 300) +
            sizes +
            (if (type.contains("png")) 30 else 0) +
            (if (purpose.contains("maskable")) 40 else 0)

        icons += resolvedHref to score
    }

    return HtmlParseResult(
        manifestUrl = manifestUrl,
        iconCandidates = icons,
    )
}

private fun parseManifestCandidates(manifestUrl: String, manifestJson: String): List<Pair<String, Int>> {
    return runCatching {
        val root = JSONObject(manifestJson)
        val icons = root.optJSONArray("icons") ?: return@runCatching emptyList<Pair<String, Int>>()
        buildList {
            for (i in 0 until icons.length()) {
                val icon = icons.optJSONObject(i) ?: continue
                val src = icon.optString("src").takeIf { it.isNotBlank() } ?: continue
                val resolved = resolveUrl(manifestUrl, src) ?: continue
                val sizes = parseSizes(icon.optString("sizes"))
                val purpose = icon.optString("purpose").lowercase()
                val type = icon.optString("type").lowercase()

                val score = 500 +
                    sizes +
                    (if (purpose.contains("maskable")) 70 else 0) +
                    (if (type.contains("png")) 30 else 0)

                add(resolved to score)
            }
        }
    }.getOrElse { emptyList() }
}

private fun parseSizes(sizes: String?): Int {
    if (sizes.isNullOrBlank()) return 0
    val sizeRegex = Regex("""(\d+)x(\d+)""")
    var best = 0
    for (match in sizeRegex.findAll(sizes.lowercase())) {
        val w = match.groupValues[1].toIntOrNull() ?: continue
        val h = match.groupValues[2].toIntOrNull() ?: continue
        best = maxOf(best, minOf(w, h))
    }
    return best
}

private fun resolveUrl(base: String, href: String): String? {
    return runCatching {
        URL(URL(base), href).toString()
    }.getOrNull()
}

private fun fetchText(url: String): Pair<String, String>? {
    return runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 6_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0")
            setRequestProperty("Accept", "text/html,application/json,*/*")
        }
        val finalUrl = connection.url.toString()
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        finalUrl to body
    }.getOrNull()
}

private fun fetchImage(url: String): ImageBitmap? {
    return runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 6_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0")
            setRequestProperty("Accept", "image/*,*/*")
        }
        val bitmap = connection.inputStream.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
        connection.disconnect()
        bitmap?.asImageBitmap()
    }.getOrNull()
}

private object BookmarkFaviconCache {
    private val cache = mutableMapOf<String, ImageBitmap?>()
    operator fun get(key: String): ImageBitmap? = synchronized(cache) { cache[key] }
    operator fun set(key: String, value: ImageBitmap?) = synchronized(cache) { cache[key] = value }
}
