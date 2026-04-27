package com.neo.downloader.downloaditem.hls

import io.lindstrom.m3u8.model.MediaPlaylist
import com.neo.downloader.downloaditem.DownloadJobExtraConfig

data class HLSDownloadJobExtraConfig(
    val hlsManifest: MediaPlaylist? = null,
    val remuxToMp4: Boolean = false,
) : DownloadJobExtraConfig
