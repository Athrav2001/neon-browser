package com.neo.downloader.shared.util.ui.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.neo.downloader.resources.icons.NDMIcons
import com.neo.downloader.shared.util.ui.BaseMyColors
import ir.amirab.util.compose.IconSource

object MyIcons : BaseMyColors() {
    override val appIcon = NDMIcons.AppIcon.asIconSource("appIcon", false)

    override val settings = Icons.Rounded.Settings.asIconSource("settings")
    override val flag = Icons.Rounded.Flag.asIconSource("flag")
    override val fast = Icons.Rounded.FlashOn.asIconSource("fast")
    override val search = Icons.Rounded.Search.asIconSource("search")
    override val info = Icons.Rounded.Info.asIconSource("info")
    override val check = Icons.Rounded.Check.asIconSource("check")
    override val link = Icons.Rounded.Link.asIconSource("link")
    override val download = Icons.Rounded.Download.asIconSource("download")
    override val permission = Icons.Rounded.Security.asIconSource("permission")

    override val windowMinimize = Icons.Rounded.Remove.asIconSource("windowMinimize")
    override val windowFloating = Icons.Rounded.OpenInNew.asIconSource("windowFloating")
    override val windowMaximize = Icons.Rounded.CropSquare.asIconSource("windowMaximize")
    override val windowClose = Icons.Rounded.Close.asIconSource("windowClose")

    override val exit = Icons.Rounded.Logout.asIconSource("exit")
    override val edit = Icons.Rounded.Edit.asIconSource("edit")
    override val undo = Icons.Rounded.Undo.asIconSource("undo")

    override val openSource = Icons.Rounded.Code.asIconSource("openSource")
    override val telegram = Icons.Rounded.Send.asIconSource("telegram")
    override val speaker = Icons.Rounded.Campaign.asIconSource("speaker")
    override val group = Icons.Rounded.Group.asIconSource("group")

    override val browserMozillaFirefox = Icons.Rounded.TravelExplore.asIconSource("browserMozillaFirefox")
    override val browserGoogleChrome = Icons.Rounded.Web.asIconSource("browserGoogleChrome")
    override val browserMicrosoftEdge = Icons.Rounded.Public.asIconSource("browserMicrosoftEdge")
    override val browserOpera = Icons.Rounded.Language.asIconSource("browserOpera")

    override val next = Icons.Rounded.NavigateNext.asIconSource("next")
    override val back = Icons.Rounded.NavigateBefore.asIconSource("back")
    override val up = Icons.Rounded.KeyboardArrowUp.asIconSource("up")
    override val down = Icons.Rounded.KeyboardArrowDown.asIconSource("down")

    override val activeCount = Icons.Rounded.List.asIconSource("activeCount")
    override val speed = Icons.Rounded.Speed.asIconSource("speed")

    override val resume = Icons.Rounded.PlayArrow.asIconSource("resume")
    override val pause = Icons.Rounded.Pause.asIconSource("pause")
    override val stop = Icons.Rounded.Stop.asIconSource("stop")

    override val queue = Icons.Rounded.Queue.asIconSource("queue")
    override val queueStart = Icons.Rounded.PlayArrow.asIconSource("queueStart")
    override val queueStop = Icons.Rounded.Pause.asIconSource("queueStop")

    override val remove = Icons.Rounded.Delete.asIconSource("remove")
    override val clear = Icons.Rounded.Clear.asIconSource("clear")
    override val add = Icons.Rounded.Add.asIconSource("add")
    override val minus = Icons.Rounded.Remove.asIconSource("minus")
    override val paste = Icons.Rounded.ContentPaste.asIconSource("paste")

    override val copy = Icons.Rounded.ContentCopy.asIconSource("copy")
    override val refresh = Icons.Rounded.Refresh.asIconSource("refresh")
    override val editFolder = Icons.Rounded.Folder.asIconSource("editFolder")

    override val share = Icons.Rounded.Share.asIconSource("share")
    override val file = Icons.Rounded.Description.asIconSource("file")
    override val folder = Icons.Rounded.Folder.asIconSource("folder")

    override val fileOpen = file
    override val folderOpen = folder
    override val pictureFile = Icons.Rounded.Image.asIconSource("fileOpen")
    override val musicFile = Icons.Rounded.Audiotrack.asIconSource("folderOpen")
    override val zipFile = Icons.Rounded.FolderZip.asIconSource("pictureFile")
    override val videoFile = Icons.Rounded.Movie.asIconSource("musicFile")
    override val applicationFile = Icons.Rounded.Android.asIconSource("zipFile")
    override val documentFile = Icons.Rounded.Article.asIconSource("videoFile")
    override val otherFile = Icons.Rounded.Description.asIconSource("applicationFile")

    override val lock = Icons.Rounded.Lock.asIconSource("lock")
    override val question = Icons.Rounded.Help.asIconSource("question")

    override val grip = Icons.Rounded.DragIndicator.asIconSource("grip")
    override val sortUp = Icons.Rounded.ArrowUpward.asIconSource("sortUp")
    override val sortDown = Icons.Rounded.ArrowDownward.asIconSource("sortDown")
    override val verticalDirection = Icons.Rounded.SwapVert.asIconSource("verticalDirection")

    override val browserIntegration = Icons.Rounded.Public.asIconSource("browserIntegration")
    override val appearance = Icons.Rounded.Palette.asIconSource("appearance")
    override val downloadEngine = Icons.Rounded.Download.asIconSource("downloadEngine")
    override val network = Icons.Rounded.Wifi.asIconSource("network")
    override val language = Icons.Rounded.Language.asIconSource("language")

    override val externalLink = Icons.Rounded.OpenInNew.asIconSource("externalLink")
    override val earth = Icons.Rounded.Public.asIconSource("earth")
    override val hearth = Icons.Rounded.Favorite.asIconSource("hearth")
    override val dragAndDrop = Icons.Rounded.OpenWith.asIconSource("dragAndDrop")


    override val selectAll = Icons.Rounded.SelectAll.asIconSource("selectAll")
    override val selectInside = Icons.Rounded.CenterFocusStrong.asIconSource("selectInside")
    override val selectInvert = Icons.Rounded.CompareArrows.asIconSource("selectInvert")

    override val menu = Icons.Rounded.Menu.asIconSource("menu")

    override val close: IconSource = Icons.Rounded.Close.asIconSource("close")

    override val data: IconSource = Icons.Rounded.Storage.asIconSource("data")
    override val alphabet: IconSource = Icons.Rounded.SortByAlpha.asIconSource("alphabet")
    override val clock: IconSource = Icons.Rounded.Schedule.asIconSource("clock")
}
