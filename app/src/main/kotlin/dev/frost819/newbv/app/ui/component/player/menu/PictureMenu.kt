package dev.frost819.newbv.app.ui.component.player.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.frost819.newbv.app.entity.player.VideoAspectRatio
import dev.frost819.newbv.app.ui.component.player.menu.component.PlayerThreeLevelMenu
import dev.frost819.newbv.app.ui.component.player.menu.component.RadioMenuList
import dev.frost819.newbv.app.viewmodel.player.MenuFocusState
import dev.frost819.newbv.app.viewmodel.player.VideoPlayerPictureMenuItem
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.Resolution
import dev.frost819.newbv.data.datastore.VideoCodec

/**
 * 画质设置面板。
 *
 * 双列布局：左侧为选项值列表（[RadioMenuList]），右侧为子项列表。
 * 子项包括：分辨率、编码、宽高比、音轨。
 *
 * @param modifier 修饰符
 * @param availableQualityIds 可用画质 ID 列表
 * @param availableAudio 可用音轨列表
 * @param availableVideoCodec 可用编码列表
 * @param currentResolution 当前画质 ID
 * @param currentVideoCodec 当前编码
 * @param currentVideoAspectRatio 当前宽高比
 * @param currentAudio 当前音轨
 * @param onResolutionChange 画质变化回调
 * @param onCodecChange 编码变化回调
 * @param onAspectRatioChange 宽高比变化回调
 * @param onAudioChange 音轨变化回调
 * @param onFocusStateChange 焦点状态变化回调
 */
@Composable
fun PictureMenuList(
    modifier: Modifier = Modifier,
    availableQualityIds: List<Int>,
    availableAudio: List<Audio>,
    availableVideoCodec: List<VideoCodec>,
    currentResolution: Int?,
    currentVideoCodec: VideoCodec,
    currentVideoAspectRatio: VideoAspectRatio,
    currentAudio: Audio,
    onResolutionChange: (Int) -> Unit,
    onCodecChange: (VideoCodec) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onAudioChange: (Audio) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit,
) {
    val qualityIdList =
        remember(availableQualityIds) {
            availableQualityIds.sortedByDescending { it }
        }
    val audioList = remember(availableAudio) { availableAudio.sortedBy { it.ordinal } }

    // 菜单项文字只在源列表变化时重算。传新 List 实例会让 RadioMenuList 整列失去跳过性，
    // 播放器里每次状态变化都重建这些字符串是白花的开销。
    val qualityLabels =
        remember(qualityIdList) { qualityIdList.map { Resolution.fromCode(it).displayName } }
    val codecLabels =
        remember(availableVideoCodec) { availableVideoCodec.map { it.displayName } }
    val aspectRatioLabels = remember { VideoAspectRatio.entries.map { it.displayName } }
    val audioLabels = remember(audioList) { audioList.map { it.displayName } }
    val qualityIndex = remember(qualityIdList, currentResolution) { qualityIdList.indexOf(currentResolution) }
    val codecIndex = remember(availableVideoCodec, currentVideoCodec) { availableVideoCodec.indexOf(currentVideoCodec) }
    val aspectIndex = remember(currentVideoAspectRatio) { currentVideoAspectRatio.ordinal }
    val audioIndex = remember(audioList, currentAudio) { audioList.indexOf(currentAudio) }

    PlayerThreeLevelMenu(
        modifier = modifier,
        categories = VideoPlayerPictureMenuItem.entries,
        categoryLabel = { it.displayName },
        onFocusStateChange = onFocusStateChange,
    ) { selectedItem, itemModifier, backToMenu ->
        when (selectedItem) {
            VideoPlayerPictureMenuItem.Resolution ->
                RadioMenuList(
                    modifier = itemModifier,
                    items = qualityLabels,
                    selected = qualityIndex,
                    onSelectedChanged = { onResolutionChange(qualityIdList[it]) },
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerPictureMenuItem.Codec ->
                RadioMenuList(
                    modifier = itemModifier,
                    items = codecLabels,
                    selected = codecIndex,
                    onSelectedChanged = { onCodecChange(availableVideoCodec[it]) },
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerPictureMenuItem.AspectRatio ->
                RadioMenuList(
                    modifier = itemModifier,
                    items = aspectRatioLabels,
                    selected = aspectIndex,
                    onSelectedChanged = { onAspectRatioChange(VideoAspectRatio.entries[it]) },
                    onFocusBackToParent = backToMenu,
                )

            VideoPlayerPictureMenuItem.Audio ->
                RadioMenuList(
                    modifier = itemModifier,
                    items = audioLabels,
                    selected = audioIndex,
                    onSelectedChanged = { onAudioChange(audioList[it]) },
                    onFocusBackToParent = backToMenu,
                )
        }
    }
}

/** 画质显示名称。 */
private val Resolution.displayName: String
    get() =
        when (this) {
            Resolution.R240P -> "240P"
            Resolution.R360P -> "360P"
            Resolution.R480P -> "480P"
            Resolution.R720P -> "720P"
            Resolution.R720P60 -> "720P60"
            Resolution.R1080P -> "1080P"
            Resolution.R1080PPlus -> "1080P+"
            Resolution.R1080P60 -> "1080P60"
            Resolution.R4K -> "4K"
            Resolution.RHdr -> "HDR"
            Resolution.RDolby -> "Dolby"
            Resolution.R8K -> "8K"
        }

/** 编码显示名称。 */
private val VideoCodec.displayName: String
    get() =
        when (this) {
            VideoCodec.AVC -> "AVC/H.264"
            VideoCodec.HEVC -> "HEVC/H.265"
            VideoCodec.AV1 -> "AV1"
            VideoCodec.DVH1 -> "DV H.1"
        }

/** 音轨显示名称。 */
private val Audio.displayName: String
    get() =
        when (this) {
            Audio.A64K -> "64K"
            Audio.A132K -> "132K"
            Audio.A192K -> "192K"
            Audio.ADolbyAtoms -> "Dolby Atmos"
            Audio.AHiRes -> "Hi-Res"
        }

/** 宽高比显示名称。 */
private val VideoAspectRatio.displayName: String
    get() =
        when (this) {
            VideoAspectRatio.Default -> "默认"
            VideoAspectRatio.FourToThree -> "4:3"
            VideoAspectRatio.SixteenToNine -> "16:9"
        }
