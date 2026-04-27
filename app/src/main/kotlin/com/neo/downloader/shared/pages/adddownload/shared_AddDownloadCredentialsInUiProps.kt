package com.neo.downloader.shared.pages.adddownload

import com.neo.downloader.shared.util.FilenameFixer
import com.neo.downloader.downloaditem.IDownloadCredentials
import kotlinx.serialization.Serializable

@Serializable
data class AddDownloadCredentialsInUiProps(
    val credentials: IDownloadCredentials,
    val extraConfig: Configs = Configs(),
) {
    @Serializable
    data class Configs(
        // don't consume it directly as it might not be a valid file name on user's current OS
        val suggestedName: String? = null,
        // optional metadata provided by source UI (e.g. browser grabber)
        val prefetchedSizeLabel: String? = null,
        val prefetchedPartsCount: Int? = null,
        val prefetchedDurationSeconds: Double? = null,
    ) {
        fun getAndFixSuggestedName(): String? {
            return suggestedName?.let(FilenameFixer::fix)
        }

        fun getPrefetchedMetaLabel(): String? {
            val normalizedSize = prefetchedSizeLabel
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) }
            val parts = prefetchedPartsCount
                ?.takeIf { it > 0 }
                ?.let { "$it parts" }
            val duration = prefetchedDurationSeconds
                ?.takeIf { it > 0.0 }
                ?.let(::formatDuration)
            val chunks = listOfNotNull(normalizedSize, parts, duration)
            return chunks.takeIf { it.isNotEmpty() }?.joinToString(" • ")
        }

        private fun formatDuration(seconds: Double): String {
            val total = seconds.toInt()
            val h = total / 3600
            val m = (total % 3600) / 60
            val s = total % 60
            return if (h > 0) {
                "${h}h${m.toString().padStart(2, '0')}m${s.toString().padStart(2, '0')}s"
            } else {
                "${m}m${s.toString().padStart(2, '0')}s"
            }
        }
    }
}
