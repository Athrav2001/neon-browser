package com.neo.downloader.shared.downloaderinui.add

import com.neo.downloader.resources.Res
import com.neo.downloader.shared.downloaderinui.DownloadSize
import com.neo.downloader.shared.downloaderinui.DownloadUiChecker
import com.neo.downloader.shared.downloaderinui.LinkChecker
import com.neo.downloader.shared.ui.configurable.Configurable
import com.neo.downloader.shared.util.perhostsettings.PerHostSettingsItem
import com.neo.downloader.connection.IResponseInfo
import com.neo.downloader.downloaditem.DownloadJobExtraConfig
import com.neo.downloader.downloaditem.IDownloadCredentials
import com.neo.downloader.downloaditem.IDownloadItem
import com.neo.downloader.util.compose.StringSource
import com.neo.downloader.util.compose.asStringSource
import com.neo.downloader.util.flow.mapStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

abstract class NewDownloadInputs<
        TDownloadItem : IDownloadItem,
        TCredentials : IDownloadCredentials,
        TResponseInfoType : IResponseInfo,
        TDownloadSize : DownloadSize,
        TLinkChecker : LinkChecker<TCredentials, TResponseInfoType, TDownloadSize>,
        >(
    val downloadUiChecker: DownloadUiChecker<TCredentials, TResponseInfoType, TDownloadSize, TLinkChecker>
) {
    val openedTime = System.currentTimeMillis()

    val name = downloadUiChecker.name
    val folder = downloadUiChecker.folder
    val credentials = downloadUiChecker.credentials
    val downloadSize = downloadUiChecker.downloadSize
    abstract val downloadItem: StateFlow<TDownloadItem>
    abstract val downloadJobConfig: StateFlow<DownloadJobExtraConfig?>
    abstract val configurableList: List<Configurable<*>>

    abstract fun applyHostSettingsToExtraConfig(extraConfig: PerHostSettingsItem)

    fun setCredentials(credentials: TCredentials) {
        downloadUiChecker.credentials.update { credentials }
    }

    abstract fun downloadSizeToStringSource(downloadSize: TDownloadSize): StringSource?

    val lengthStringFlow: StateFlow<StringSource> = downloadSize.mapStateFlow {
        it
            ?.let(::downloadSizeToStringSource)
            ?: Res.string.unknown.asStringSource()
    }

    fun getLengthString(): StringSource {
        return lengthStringFlow.value
    }

    fun getUniqueId(): NewDownloadInputsUniqueIdType = hashCode()
}
typealias TANewDownloadInputs = NewDownloadInputs<IDownloadItem, IDownloadCredentials, IResponseInfo, DownloadSize, LinkChecker<IDownloadCredentials, IResponseInfo, DownloadSize>>
typealias NewDownloadInputsUniqueIdType = Int
