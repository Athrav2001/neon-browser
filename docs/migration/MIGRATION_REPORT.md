# Migration Dry-Run Report

- Total Kotlin files scanned: **530**
- Modules scanned: **8**

## Breakdown

### By module
- `downloader:core`: 97
- `downloader:monitor`: 8
- `shared:app`: 267
- `shared:auto-start`: 4
- `shared:compose-utils`: 20
- `shared:resources`: 83
- `shared:updater`: 12
- `shared:utils`: 39

### By source set
- `androidMain`: 17
- `commonMain`: 513

## High-Risk Signals

- `expect` keywords: **25**
- `actual` keywords: **34**
- Arrow/optics markers: **2**
- StringSource usage: **645**
- rememberString usage: **21**
- Koin markers/imports: **21**

## Conflict Preview

- Duplicate FQCN candidates: **0**
- Target-path collisions (`app/src/main/kotlin/...`): **0**

### Duplicate FQCN sample
- none

### Target-path collision sample
- none

## Risky import families (top 40)
- `ir.neo.util.compose.StringSource`: 52
- `com.neo.downloader.shared.util.ui.myColors`: 28
- `ir.neo.util.compose.asStringSource`: 27
- `com.neo.downloader.shared.util.div`: 21
- `ir.neo.util.ifThen`: 21
- `ir.neo.util.compose.IconSource`: 20
- `ir.neo.util.flow.mapStateFlow`: 20
- `com.neo.downloader.shared.downloaderinui.DownloadSize`: 19
- `com.neo.downloader.shared.util.DownloadSystem`: 19
- `com.neo.downloader.shared.ui.configurable.Configurable`: 17
- `com.neo.downloader.shared.util.ui.icon.MyIcons`: 16
- `ir.neo.util.HttpUrlUtils`: 16
- `com.neo.downloader.shared.util.ui.widget.MyIcon`: 13
- `ir.neo.util.flow.combineStateFlows`: 13
- `com.neo.downloader.shared.util.ui.theme.myShapes`: 12
- `com.neo.downloader.shared.util.BaseComponent`: 11
- `com.neo.downloader.shared.util.ui.theme.myTextSizes`: 11
- `com.neo.downloader.shared.util.ui.LocalContentColor`: 10
- `ir.neo.util.compose.action.MenuItem`: 10
- `com.neo.downloader.shared.downloaderinui.LinkChecker`: 9
- `com.neo.downloader.shared.util.category.Category`: 9
- `com.neo.downloader.shared.util.category.CategoryManager`: 9
- `com.neo.downloader.shared.util.mvi.ContainsEffects`: 9
- `com.neo.downloader.shared.util.mvi.supportEffects`: 9
- `com.neo.downloader.shared.util.ui.WithContentAlpha`: 9
- `ir.neo.util.compose.asStringSourceWithARgs`: 9
- `com.neo.downloader.shared.ui.configurable.item.IntConfigurable`: 8
- `com.neo.downloader.shared.ui.configurable.item.SpeedLimitConfigurable`: 8
- `com.neo.downloader.shared.util.ui.theme.mySpacings`: 8
- `ir.neo.util.platform.Platform`: 8
- `com.neo.downloader.shared.downloaderinui.edit.DownloadConflictDetector`: 7
- `com.neo.downloader.shared.repository.BaseAppRepository`: 7
- `com.neo.downloader.shared.ui.configurable.item.StringConfigurable`: 7
- `com.neo.downloader.shared.util.SizeAndSpeedUnitProvider`: 7
- `com.neo.downloader.shared.util.ThreadCountLimitation`: 7
- `com.neo.downloader.shared.util.convertPositiveSpeedToHumanReadable`: 7
- `ir.neo.util.flow.mapTwoWayStateFlow`: 7
- `com.neo.downloader.shared.storage.BaseAppSettingsStorage`: 6
- `com.neo.downloader.shared.util.FileChecksum`: 6
- `com.neo.downloader.shared.util.perhostsettings.PerHostSettingsItem`: 6

## Ordered Migration Batches

1. `shared:utils`, `shared:config`, `shared:resources:contracts`
2. `shared:resources`, `shared:compose-utils`
3. `downloader:monitor`, `downloader:core`
4. `shared:updater`, `shared:auto-start`
5. `shared:app` (last, largest UI integration surface)

## Notes

- This report is dry-run only; no source mutation.
- Package remap is intentionally deferred until compile parity is green.
