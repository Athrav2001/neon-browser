package com.neo.downloader.shared.action

import com.neo.downloader.shared.pagemanager.NotificationSender
import com.neo.downloader.shared.ui.widget.MessageDialogType
import com.neo.downloader.shared.util.ui.icon.MyIcons
import com.neo.downloader.util.compose.action.AnAction
import com.neo.downloader.util.compose.action.MenuItem
import com.neo.downloader.util.compose.action.simpleAction
import com.neo.downloader.util.compose.asStringSource


fun createDummyExceptionAction(): AnAction {
    return simpleAction(
        "Dummy Exception".asStringSource(),
        MyIcons.info
    ) {
        error("This is a dummy exception that is thrown by developer")
    }
}

fun createDummyMessageAction(
    notificationSender: NotificationSender,
): MenuItem.SubMenu {
    return MenuItem.SubMenu(
        title = "Show Dialog Message".asStringSource(),
        icon = MyIcons.info,
        items = listOf(
            MessageDialogType.Info,
            MessageDialogType.Error,
            MessageDialogType.Warning,
            MessageDialogType.Success,
        ).map {
            createDummyMessage(it, notificationSender)
        }
    )
}

private fun createDummyMessage(
    type: MessageDialogType,
    notificationSender: NotificationSender,
): AnAction {
    return simpleAction(
        "$type Message".asStringSource(),
        MyIcons.info,
    ) {
        notificationSender.sendDialogNotification(
            type = type,
            title = "Dummy Message".asStringSource(),
            description = "This is a test message".asStringSource()
        )
    }
}
