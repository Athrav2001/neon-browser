package com.neo.downloader.android.pages.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.neo.downloader.resources.Res
import com.neo.downloader.shared.util.div
import com.neo.downloader.shared.util.ui.icon.MyIcons
import com.neo.downloader.shared.util.ui.myColors
import com.neo.downloader.shared.util.ui.theme.myShapes
import com.neo.downloader.shared.util.ui.widget.MyIcon
import ir.amirab.util.compose.IconSource
import ir.amirab.util.compose.StringSource
import ir.amirab.util.compose.asStringSource

object BottomNavigationConstants {
    const val DEFAULT_ICON_SIZE = 20
    const val DEFAULT_ICON_PADDING = 16
}

@Composable
fun BottomNavigation(
    modifier: Modifier,
    component: HomeComponent,
) {
    val isShowingAddMenu by component.isAddMenuShowing.collectAsState()
    val isMainMenuShowing by component.isMainMenuShowing.collectAsState()
    val isCategoryFilterMenuShowing by component.isCategoryFilterShowing.collectAsState()
    val filterMode = component.filterMode.value
    Row(
        modifier
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = myShapes.defaultRounded
        Box(
            Modifier
                .weight(1f)
                .height(IntrinsicSize.Max)
                .shadow(
                    elevation = if (myColors.isLight) 6.dp else 14.dp,
                    shape = shape,
                    ambientColor = if (myColors.isLight) myColors.onBackground / 0.06f else myColors.glowColor,
                    spotColor = if (myColors.isLight) myColors.onBackground / 0.06f else myColors.glowColor,
                )
                .clip(shape)
                .border(
                    1.dp,
                    if (myColors.isLight) myColors.onSurface / 0.10f else myColors.primary / 0.35f,
                    shape
                )
                .background(myColors.surface)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottonNavigationItem(
                    icon = MyIcons.earth,
                    contentDescription = Res.string.browser.asStringSource(),
                    onClick = component::openBrowser,
                    modifier = Modifier.weight(1f),
                    isSelected = false,
                    iconSize = 18.dp,
                )
                Spacer(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(myColors.onSurface / 0.1f)
                )
                when (filterMode) {
                    is HomeComponent.FilterMode.Queue -> {
                        QueueIndicator(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            filterMode,
                            isSelected = isCategoryFilterMenuShowing,
                        ) {
                            component.setIsCategoryFilterShowing(!isCategoryFilterMenuShowing)
                        }
                    }

                    is HomeComponent.FilterMode.Status -> {
                        FilterStatusIndicator(
                            component,
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            filterMode,
                            isSelected = isCategoryFilterMenuShowing,
                            onClick = {
                                component.setIsCategoryFilterShowing(!isCategoryFilterMenuShowing)
                            }
                        )
                    }
                }
                Spacer(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(myColors.onSurface / 0.1f)
                )
                Box(Modifier.weight(1f)) {
                    RenderAddMenu(component)
                    BottonNavigationItem(
                        icon = MyIcons.add,
                        contentDescription = Res.string.add.asStringSource(),
                        onClick = {
                            component.setIsAddMenuShowing(!isShowingAddMenu)
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxHeight(),
                        isSelected = isShowingAddMenu,
                    )
                }
                Spacer(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(myColors.onSurface / 0.1f)
                )
                Box(Modifier.weight(1f)) {
                    RenderMainMenu(component)
                    BottonNavigationItem(
                        icon = MyIcons.menu,
                        contentDescription = Res.string.menu.asStringSource(),
                        onClick = {
                            component.setIsMainMenuShowing(!isMainMenuShowing)
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxHeight(),
                        isSelected = isMainMenuShowing,
                    )
                }
            }
        }
    }
}

@Composable
private fun BottonNavigationItem(
    icon: IconSource,
    contentDescription: StringSource,
    onClick: () -> Unit,
    modifier: Modifier,
    isSelected: Boolean,
    iconSize: androidx.compose.ui.unit.Dp = BottomNavigationConstants.DEFAULT_ICON_SIZE.dp,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        MyIcon(
            icon = icon,
            contentDescription = contentDescription.rememberString(),
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(BottomNavigationConstants.DEFAULT_ICON_PADDING.dp)
                .size(iconSize)
        )
        BottomNavigationSelectedIndicator(isSelected)
    }
}

@Composable
fun BoxScope.BottomNavigationSelectedIndicator(
    isSelected: Boolean,
) {
    if (isSelected) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = myColors.primaryGradientColors.map { it / 0.15f }
                    )
                )
        )
        Box(
            Modifier
                .matchParentSize()
                .wrapContentHeight(Alignment.Bottom)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        myColors.primaryGradientColors
                    )
                )
        )
    }
}
