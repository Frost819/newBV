package dev.frost819.newbv.app.ui.screen.settings

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import dev.frost819.newbv.core.focus.touchClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.frost819.newbv.app.util.CodecInfoData
import dev.frost819.newbv.app.util.CodecMedia
import dev.frost819.newbv.app.util.CodecMode
import dev.frost819.newbv.app.util.CodecType
import dev.frost819.newbv.app.util.CodecUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 编解码器信息页。
 *
 * 左右分栏：左侧为解码器列表，右侧为选中编解码器的详细信息。
 * D-Pad Left 从详情返回列表。
 */
@Composable
fun MediaCodecScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    var currentCodecInfoData by remember { mutableStateOf<CodecInfoData?>(null) }
    var focusInNav by remember { mutableStateOf(false) }
    val decoderList = remember { mutableStateListOf<CodecInfoData>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val list = CodecUtil.parseCodecs().filter { it.type == CodecType.Decoder }
            decoderList.clear()
            decoderList.addAll(list)
            currentCodecInfoData = list.firstOrNull()
        }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            Box(
                modifier = Modifier.padding(start = 48.dp, top = 24.dp, bottom = 8.dp, end = 48.dp),
            ) {
                Text(text = "编解码信息", fontSize = 24.sp)
            }
        },
    ) { innerPadding ->
        Row(modifier = Modifier.padding(innerPadding)) {
            MediaCodecListItems(
                modifier = Modifier
                    .onFocusChanged { focusInNav = it.hasFocus }
                    .weight(3f)
                    .fillMaxHeight(),
                codecInfoDataList = decoderList,
                currentCodecInfoData = currentCodecInfoData,
                onCodecInfoDataChanged = { currentCodecInfoData = it },
                isFocusing = focusInNav,
            )
            MediaCodecDetails(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxSize(),
                onBackNav = { focusInNav = true },
                currentCodecInfoData = currentCodecInfoData,
            )
        }
    }
}

@Composable
private fun MediaCodecListItems(
    modifier: Modifier = Modifier,
    codecInfoDataList: List<CodecInfoData>,
    currentCodecInfoData: CodecInfoData?,
    onCodecInfoDataChanged: (CodecInfoData) -> Unit,
    isFocusing: Boolean,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocusing) {
        if (isFocusing) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    LaunchedEffect(codecInfoDataList) {
        if (codecInfoDataList.isNotEmpty()) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = codecInfoDataList) { codecInfoData ->
            val buttonModifier = if (currentCodecInfoData == codecInfoData) {
                Modifier.focusRequester(focusRequester).fillMaxWidth()
            } else {
                Modifier.fillMaxWidth()
            }
            ListItem(
                modifier = buttonModifier
                    .onFocusChanged {
                        if (it.hasFocus) onCodecInfoDataChanged(codecInfoData)
                    }
                    .touchClickable(onClick = { onCodecInfoDataChanged(codecInfoData) }),
                selected = currentCodecInfoData == codecInfoData,
                onClick = { onCodecInfoDataChanged(codecInfoData) },
                headlineContent = {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = codecInfoData.name,
                    )
                },
                overlineContent = {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp),
                            text = codecInfoData.mimeType,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = when (codecInfoData.media) {
                                CodecMedia.Audio -> Icons.Default.Audiotrack
                                CodecMedia.Video -> Icons.Default.Videocam
                            },
                            contentDescription = null,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun MediaCodecDetails(
    modifier: Modifier = Modifier,
    onBackNav: () -> Unit,
    currentCodecInfoData: CodecInfoData?,
) {
    if (currentCodecInfoData != null) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .onPreviewKeyEvent {
                    if (it.key == Key.DirectionLeft && it.type == KeyEventType.KeyDown) {
                        onBackNav()
                        true
                    } else {
                        false
                    }
                },
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
        ) {
            item {
                MediaCodecDetailItem(
                    title = "硬/软解",
                    text = when (currentCodecInfoData.mode) {
                        CodecMode.Hardware -> "硬件解码"
                        CodecMode.Software -> "软件解码"
                    },
                )
            }
            item {
                MediaCodecDetailItem(
                    title = "最大并发实例",
                    text = currentCodecInfoData.maxSupportedInstances?.toString() ?: "Unknown",
                )
            }
            if (currentCodecInfoData.media == CodecMedia.Video) {
                item {
                    MediaCodecDetailItem(
                        title = "颜色格式",
                        text = currentCodecInfoData.colorFormats.joinToString(),
                    )
                }
                item {
                    MediaCodecDetailItem(
                        title = "视频最大码率",
                        text = currentCodecInfoData.videoBitrateRange?.last?.toBps() ?: "Unknown",
                    )
                }
                item {
                    MediaCodecDetailItem(
                        title = "帧率范围",
                        text = "${currentCodecInfoData.videoFrame?.first}fps - ${currentCodecInfoData.videoFrame?.last}fps",
                    )
                }
                item {
                    MediaCodecDetailItem(
                        title = "支持帧率",
                        text = currentCodecInfoData.supportedFrameRates.joinToString("\n") { sfr ->
                            resolutionName(sfr.resolution.second) + ": " +
                                if (sfr.unsupported) "不支持" else {
                                    String.format(
                                        Locale.getDefault(),
                                        "%.1f",
                                        sfr.frameRate.upper,
                                    ) + "fps"
                                }
                        },
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    item {
                        MediaCodecDetailItem(
                            title = "可达帧率",
                            text = currentCodecInfoData.achievableFrameRates.joinToString("\n") { sfr ->
                                resolutionName(sfr.resolution.second) + ": " +
                                    if (sfr.unsupported) "不支持" else {
                                        String.format(
                                            Locale.getDefault(),
                                            "%.1f",
                                            sfr.frameRate.upper,
                                        ) + "fps"
                                    }
                            },
                        )
                    }
                }
            }
            if (currentCodecInfoData.media == CodecMedia.Audio) {
                item {
                    MediaCodecDetailItem(
                        title = "音频码率范围",
                        text = "${currentCodecInfoData.audioBitrateRange?.first?.toBps()} - ${currentCodecInfoData.audioBitrateRange?.last?.toBps()}",
                    )
                }
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Empty")
        }
    }
}

@Composable
private fun MediaCodecDetailItem(title: String, text: String) {
    ListItem(
        headlineContent = { Text(text = title) },
        supportingContent = { Text(text = text) },
        selected = false,
        onClick = {},
    )
}

private fun resolutionName(height: Int): String = when (height) {
    360 -> "360P"
    480 -> "480P"
    720 -> "720P"
    1080 -> "1080P"
    1440 -> "1440P"
    2160 -> "4K"
    4320 -> "8K"
    else -> "${height}P"
}

private fun Int.toBps(): String = when {
    this >= 1_000_000 -> "${this / 1_000_000} Mbps"
    this >= 1_000 -> "${this / 1_000} Kbps"
    else -> "$this bps"
}
