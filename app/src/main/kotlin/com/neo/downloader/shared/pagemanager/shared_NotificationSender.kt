package com.neo.downloader.shared.pagemanager

import com.neo.downloader.shared.ui.widget.MessageDialogType
import com.neo.downloader.shared.ui.widget.NotificationType
import com.neo.downloader.util.compose.StringSource

interface NotificationSender {
    fun sendDialogNotification(title: StringSource, description: StringSource, type: MessageDialogType)
    fun sendNotification(tag: Any, title: StringSource, description: StringSource, type: NotificationType)
}
