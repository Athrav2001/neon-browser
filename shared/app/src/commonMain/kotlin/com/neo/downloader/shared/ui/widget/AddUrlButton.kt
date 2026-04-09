package com.neo.downloader.shared.ui.widget

import com.neo.downloader.shared.util.ui.widget.MyIcon
import com.neo.downloader.shared.util.ui.icon.MyIcons
import com.neo.downloader.shared.util.div
import com.neo.downloader.shared.util.ui.myColors
import com.neo.downloader.shared.util.ui.theme.myTextSizes
import com.neo.downloader.shared.util.ui.WithContentAlpha
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.neo.downloader.resources.Res
import com.neo.downloader.shared.util.ui.theme.myShapes
import ir.amirab.util.compose.resources.myStringResource

@Composable
fun AddUrlButton(
    modifier: Modifier=Modifier,
    onClick:()->Unit
) {
    val shape = myShapes.defaultRounded
    val addUrlIcon = MyIcons.link
    val downloadIcon = MyIcons.download
    Row(
        modifier
            .shadow(
                elevation = if (myColors.isLight) 3.dp else 8.dp,
                shape = shape,
                ambientColor = if (myColors.isLight) myColors.onBackground / 0.06f else myColors.glowColor,
                spotColor = if (myColors.isLight) myColors.onBackground / 0.06f else myColors.glowColor,
            )
            .clip(shape)
            .background(myColors.surface)
            .clickable(onClick = onClick)
            .height(36.dp)
//            .width(120.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,

        ) {
        WithContentAlpha(1f) {
            MyIcon(addUrlIcon, null, Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                myStringResource(Res.string.new_download),
                Modifier,
                maxLines = 1,
                fontSize = myTextSizes.sm,
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .clip(myShapes.defaultRounded)
                .background(
                    myColors.primaryGradient
                )
                .padding(5.dp)
        ) {
            MyIcon(
                downloadIcon,
                null,
                Modifier.size(13.dp),
                tint = myColors.onPrimaryGradient,
            )
        }
    }

}
