package com.neo.downloader.shared.downloaderinui.http.edit

import com.neo.downloader.shared.downloaderinui.DownloadSize
import com.neo.downloader.shared.downloaderinui.LinkChecker
import com.neo.downloader.shared.downloaderinui.edit.CanEditDownloadResult
import com.neo.downloader.shared.downloaderinui.edit.CanEditWarnings
import com.neo.downloader.shared.downloaderinui.edit.DownloadConflictDetector
import com.neo.downloader.shared.downloaderinui.http.add.HttpLinkChecker
import com.neo.downloader.connection.IResponseInfo
import com.neo.downloader.connection.response.HttpResponseInfo
import com.neo.downloader.downloaditem.IDownloadCredentials
import com.neo.downloader.downloaditem.IDownloadItem
import com.neo.downloader.downloaditem.http.HttpDownloadCredentials
import com.neo.downloader.downloaditem.http.HttpDownloadItem
import com.neo.downloader.util.FileNameValidator
import com.neo.downloader.util.HttpUrlUtils
import com.neo.downloader.util.flow.mapStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class EditDownloadChecker<
        TDownloadItem : IDownloadItem,
        TCredentials : IDownloadCredentials,
        TResponseInfo : IResponseInfo,
        TDownloadSize : DownloadSize,
        TLinkChecker : LinkChecker<TCredentials, TResponseInfo, TDownloadSize>
        >(
    val currentDownloadItem: MutableStateFlow<TDownloadItem>,
    val editedDownloadItem: MutableStateFlow<TDownloadItem>,
    val linkChecker: TLinkChecker,
    val conflictDetector: DownloadConflictDetector,
    val scope: CoroutineScope,
) {
    abstract fun check()

    protected val _canEditResult = MutableStateFlow<CanEditDownloadResult>(CanEditDownloadResult.NothingChanged)
    val canEditResult = _canEditResult.asStateFlow()
    val canEdit = canEditResult.mapStateFlow {
        it is CanEditDownloadResult.CanEdit
    }
}

class HttpEditDownloadChecker(
    currentDownloadItem: MutableStateFlow<HttpDownloadItem>,
    editedDownloadItem: MutableStateFlow<HttpDownloadItem>,
    conflictDetector: DownloadConflictDetector,
    scope: CoroutineScope,
    linkChecker: HttpLinkChecker,
) : EditDownloadChecker<HttpDownloadItem, HttpDownloadCredentials, HttpResponseInfo, DownloadSize.Bytes, HttpLinkChecker>(
    currentDownloadItem = currentDownloadItem,
    editedDownloadItem = editedDownloadItem,
    conflictDetector = conflictDetector,
    scope = scope,
    linkChecker = linkChecker
) {
    init {
        editedDownloadItem
            .onEach {
                _canEditResult.value = CanEditDownloadResult.Waiting
            }.launchIn(scope)
    }

    override fun check() {
        _canEditResult.value = CanEditDownloadResult.Waiting
        _canEditResult.value = check(
            current = currentDownloadItem.value,
            edited = editedDownloadItem.value,
            newLength = linkChecker.downloadSize.value?.bytes,
        )
    }

    private fun check(
        current: HttpDownloadItem,
        edited: HttpDownloadItem,
        newLength: Long?,
    ): CanEditDownloadResult {
        if (current == edited) {
            return CanEditDownloadResult.NothingChanged
        }
        if (!HttpUrlUtils.isValidUrl(edited.link)) {
            return CanEditDownloadResult.InvalidURL
        }
        if (edited.name != current.name) {
            if (!FileNameValidator.isValidFileName(edited.name)) {
                return CanEditDownloadResult.InvalidFileName
            }
            if (conflictDetector.checkAlreadyExists(current, edited)) {
                return CanEditDownloadResult.FileNameAlreadyExists
            }
        }
        val warnings = mutableListOf<CanEditWarnings>()
        if (current.contentLength != newLength) {
            warnings.add(
                CanEditWarnings.FileSizeNotMatch(
                    currentSize = current.contentLength,
                    newSize = newLength ?: IDownloadItem.Companion.LENGTH_UNKNOWN,
                )
            )
        }
        return CanEditDownloadResult.CanEdit(warnings)
    }
}
