package com.neo.downloader.android.pages.browser.adblock

import com.neo.downloader.android.storage.AppSettingsStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class AdBlockFiltersManager(
    private val appScope: CoroutineScope,
    private val appSettingsStorage: AppSettingsStorage,
    private val sourceStorage: IAdBlockSourceStorage,
) {
    private val _hostsFlow = MutableStateFlow<Set<String>>(emptySet())
    val hostsFlow: StateFlow<Set<String>> = _hostsFlow.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()
    val sourcesFlow: StateFlow<List<AdBlockFilterSource>> = sourceStorage.sourcesFlow
    val adBlockEnabledFlow = appSettingsStorage.browserAdBlockEnabled
    val autoUpdateEnabledFlow = appSettingsStorage.adBlockAutoUpdateEnabled
    val lastUpdatedAtFlow = appSettingsStorage.adBlockLastUpdatedAt

    fun initialize() {
        ensureSourcesBootstrapped()
        loadHostsFromDisk()
        if (appSettingsStorage.adBlockAutoUpdateEnabled.value && shouldUpdate()) {
            forceUpdateInBackground()
        }
    }

    fun getSources(): List<AdBlockFilterSource> = sourceStorage.sourcesFlow.value

    fun setSourceEnabled(id: String, enabled: Boolean) {
        sourceStorage.sourcesFlow.value = sourceStorage.sourcesFlow.value.map {
            if (it.id == id) it.copy(enabled = enabled) else it
        }
    }

    fun addSource(
        name: String,
        url: String,
        enabled: Boolean = true,
    ) {
        val source = AdBlockFilterSource(
            id = "src_${UUID.randomUUID().toString().replace("-", "")}",
            name = name.trim(),
            url = url.trim(),
            enabled = enabled,
        )
        sourceStorage.sourcesFlow.value = sourceStorage.sourcesFlow.value + source
    }

    fun updateSource(
        id: String,
        name: String,
        url: String,
    ) {
        sourceStorage.sourcesFlow.value = sourceStorage.sourcesFlow.value.map { source ->
            if (source.id == id) {
                source.copy(
                    name = name.trim(),
                    url = url.trim(),
                )
            } else {
                source
            }
        }
    }

    fun deleteSource(id: String) {
        sourceStorage.sourcesFlow.value = sourceStorage.sourcesFlow.value.filterNot { it.id == id }
        sourceCacheFile(id).delete()
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        appSettingsStorage.adBlockAutoUpdateEnabled.value = enabled
    }

    fun forceUpdateInBackground(onCompleted: ((Result<Int>) -> Unit)? = null) {
        if (_isUpdating.value) return
        appScope.launch(Dispatchers.IO) {
            _isUpdating.value = true
            val result = runCatching {
                val hosts = buildHostsFromEnabledSources()
                persistHosts(hosts)
                _hostsFlow.value = hosts
                appSettingsStorage.adBlockLastUpdatedAt.value = System.currentTimeMillis()
                hosts.size
            }
            _isUpdating.value = false
            onCompleted?.let { callback ->
                launch(Dispatchers.Main) {
                    callback(result)
                }
            }
        }
    }

    fun forceUpdateSourceInBackground(
        sourceId: String,
        onCompleted: ((Result<Int>) -> Unit)? = null,
    ) {
        sourceStorage.sourcesFlow.value = sourceStorage.sourcesFlow.value.map { source ->
            if (source.id == sourceId) {
                source.copy(etag = null, lastModified = null)
            } else {
                source
            }
        }
        forceUpdateInBackground(onCompleted)
    }

    private fun shouldUpdate(): Boolean {
        val sevenDaysMillis = 7L * 24L * 60L * 60L * 1000L
        val last = appSettingsStorage.adBlockLastUpdatedAt.value
        return (System.currentTimeMillis() - last) >= sevenDaysMillis
    }

    private fun ensureSourcesBootstrapped() {
        val current = sourceStorage.sourcesFlow.value
        if (current.isEmpty()) {
            sourceStorage.sourcesFlow.value = defaultAdBlockSources()
            return
        }
        val oldIds = setOf("1dm_pack", "easylist", "adguard_mobile")
        val needsMigration = current.any { source ->
            source.url.contains("files.catbox.moe", ignoreCase = true)
        } || (current.size <= 3 && current.all { it.id in oldIds })
        if (needsMigration) {
            sourceStorage.sourcesFlow.value = defaultAdBlockSources()
        }
    }

    private fun loadHostsFromDisk() {
        val file = AdBlockStoragePaths.hostsFile()
        if (!file.exists() || !file.canRead()) {
            _hostsFlow.value = emptySet()
            return
        }
        val hosts = readLinesSafely(file)
            .orEmpty()
            .map { it.trim().lowercase(Locale.US) }
            .filter { it.isNotEmpty() }
            .toSet()
        _hostsFlow.value = hosts
    }

    private fun persistHosts(hosts: Set<String>) {
        val file = AdBlockStoragePaths.hostsFile()
        writeTextSafely(file, hosts.joinToString("\n"))
    }

    private fun buildHostsFromEnabledSources(): Set<String> {
        val hosts = linkedSetOf<String>()
        val currentSources = sourceStorage.sourcesFlow.value
        val updatedSources = mutableListOf<AdBlockFilterSource>()
        val removedSourceIds = mutableSetOf<String>()
        currentSources
            .filter { it.enabled }
            .forEach { source ->
                val (sourceHosts, updatedSource, shouldRemove) = loadOrUpdateHosts(source)
                if (shouldRemove) {
                    removedSourceIds.add(source.id)
                    sourceCacheFile(source.id).delete()
                    return@forEach
                }
                updatedSources.add(updatedSource)
                sourceHosts.forEach { host ->
                    hosts.add(host)
                }
            }
        if (updatedSources.isNotEmpty() || removedSourceIds.isNotEmpty()) {
            val updatesById = updatedSources.associateBy { it.id }
            sourceStorage.sourcesFlow.value = currentSources.map { original ->
                updatesById[original.id] ?: original
            }.filterNot { it.id in removedSourceIds }
        }
        return hosts
    }

    private fun loadOrUpdateHosts(source: AdBlockFilterSource): Triple<Set<String>, AdBlockFilterSource, Boolean> {
        val cacheFile = sourceCacheFile(source.id)
        val currentMeta = fetchRemoteMetadata(source.url)
        val remoteEtag = currentMeta?.first?.takeIf { it.isNotBlank() }
        val remoteLastModified = currentMeta?.second?.takeIf { it.isNotBlank() }
        val hasCache = cacheFile.exists() && cacheFile.canRead()
        val etagMatches = remoteEtag != null && source.etag != null && source.etag == remoteEtag
        val lastModifiedMatches = remoteLastModified != null &&
            source.lastModified != null &&
            source.lastModified == remoteLastModified
        val hasComparableMetadata = (remoteEtag != null && source.etag != null) ||
            (remoteLastModified != null && source.lastModified != null)
        val unchanged = hasCache && hasComparableMetadata && (etagMatches || lastModifiedMatches)

        if (unchanged) {
            val fromCache = readLinesSafely(cacheFile)
                .orEmpty()
                .asSequence()
                .map { it.trim().lowercase(Locale.US) }
                .filter { it.isNotEmpty() }
                .toSet()
            return Triple(fromCache, source, false)
        }

        val bytes = runCatching { URL(source.url).openStream().use { it.readBytes() } }
            .getOrNull()
        if (bytes == null) {
            return Triple(emptySet(), source, true)
        }
        val parsed = parseBytesToHosts(bytes)
        if (parsed.isEmpty()) {
            return Triple(emptySet(), source, true)
        }
        writeTextSafely(cacheFile, parsed.joinToString("\n"))
        val updated = source.copy(
            etag = remoteEtag ?: source.etag,
            lastModified = remoteLastModified ?: source.lastModified,
            downloadedBytes = bytes.size.toLong(),
            lastUpdatedAt = System.currentTimeMillis(),
        )
        return Triple(parsed, updated, false)
    }

    private fun sourceCacheFile(sourceId: String): File {
        val safe = sourceId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return AdBlockStoragePaths.sourceHostsDir().resolve("$safe.txt")
    }

    private fun fetchRemoteMetadata(url: String): Pair<String?, String?>? {
        return runCatching {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "HEAD"
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.connect()
            val etag = conn.getHeaderField("ETag")
            val lastModified = conn.getHeaderField("Last-Modified")
            conn.disconnect()
            etag to lastModified
        }.getOrNull()
    }

    private fun parseBytesToHosts(bytes: ByteArray): Set<String> {
        return if (isZip(bytes)) {
            parseZipBytes(bytes)
        } else {
            parseTextToHosts(bytes.toString(StandardCharsets.UTF_8))
        }
    }

    private fun isZip(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
            bytes[0] == 'P'.code.toByte() &&
            bytes[1] == 'K'.code.toByte()
    }

    private fun parseZipBytes(bytes: ByteArray): Set<String> {
        val hosts = linkedSetOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val entryBytes = zis.readBytes()
                    val name = entry.name.lowercase(Locale.US)
                    if (name.endsWith(".zip")) {
                        hosts.addAll(parseZipBytes(entryBytes))
                    } else {
                        hosts.addAll(parseTextToHosts(entryBytes.toString(StandardCharsets.UTF_8)))
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return hosts
    }

    private fun parseTextToHosts(text: String): Set<String> {
        val hosts = linkedSetOf<String>()
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            if (line.startsWith("!") || line.startsWith("#")) return@forEach
            if (line.startsWith("@@")) return@forEach
            // Avoid converting ABP/uBO filter syntax into host blocks.
            // This parser intentionally supports only hosts-format and plain domains.
            if (
                line.startsWith("||") ||
                line.contains("##") ||
                line.contains("#@#") ||
                line.contains('$') ||
                line.contains('*') ||
                line.contains('^')
            ) return@forEach

            // hosts file style: 0.0.0.0 example.com
            if (line.contains(' ')) {
                val parts = line.split(Regex("\\s+"))
                val maybeHost = parts.lastOrNull().orEmpty()
                normalizeHost(maybeHost)?.let { hosts.add(it) }
                return@forEach
            }

            normalizeHost(line)?.let { hosts.add(it) }
        }
        return hosts
    }

    private fun normalizeHost(value: String): String? {
        val candidate = value
            .removePrefix("www.")
            .trim()
            .lowercase(Locale.US)
            .removeSuffix(".")
        if (candidate.isBlank()) return null
        if (!candidate.contains('.')) return null
        if (candidate.any { !(it.isLetterOrDigit() || it == '.' || it == '-') }) return null
        return runCatching { URI("https://$candidate").host }.getOrNull()
            ?.removePrefix("www.")
            ?.lowercase(Locale.US)
    }

    private fun readLinesSafely(file: File): List<String>? {
        return runCatching { file.readLines() }.getOrNull()
    }

    private fun writeTextSafely(file: File, value: String): Boolean {
        return runCatching {
            file.parentFile?.mkdirs()
            file.writeText(value)
            true
        }.getOrElse { false }
    }
}
