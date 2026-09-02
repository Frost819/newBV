package dev.frost819.newbv.app.ui.screen.settings.content

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.frost819.newbv.app.ui.component.FocusSaver
import dev.frost819.newbv.app.ui.component.focusSaverItem
import dev.frost819.newbv.app.ui.screen.settings.SettingsMenuNavItem
import dev.frost819.newbv.core.focus.touchClickable
import java.text.DecimalFormat
import kotlin.math.pow

/**
 * 设备信息页。
 *
 * 显示设备型号、系统版本、屏幕分辨率、SOC、内存、存储等信息，
 * 底部按钮跳转编解码信息页。
 *
 * @param onOpenMediaCodec 点击"编解码信息"按钮的回调。
 */
@Composable
fun InfoSetting(
    modifier: Modifier = Modifier,
    onOpenMediaCodec: () -> Unit = {},
    screenFocusSaver: FocusSaver? = null,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val memoryInfo =
        remember {
            runCatching {
                val memoryInfo = ActivityManager.MemoryInfo()
                (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .getMemoryInfo(memoryInfo)
                val df = DecimalFormat("###.##")
                Pair(
                    "${df.format(memoryInfo.availMem / 1024.0.pow(3))} GB",
                    "${df.format(memoryInfo.totalMem / 1024.0.pow(3))} GB",
                )
            }.getOrDefault(Pair("Unknown", "Unknown"))
        }

    val storageInfo =
        remember {
            runCatching {
                val statFs = StatFs(Environment.getExternalStorageDirectory().absolutePath)
                val df = DecimalFormat("###.##")
                Pair(
                    "${df.format(statFs.availableBytes / 1024.0.pow(3))} GB",
                    "${df.format(statFs.totalBytes / 1024.0.pow(3))} GB",
                )
            }.getOrDefault(Pair("Unknown", "Unknown"))
        }

    val screenInfo =
        remember {
            runCatching {
                val display = context.display ?: return@runCatching Triple(0, 0, 0f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val mode = display.mode
                    Triple(mode.physicalWidth, mode.physicalHeight, mode.refreshRate)
                } else {
                    Triple(0, 0, 0f)
                }
            }.getOrDefault(Triple(0, 0, 0f))
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = SettingsMenuNavItem.Info.displayName,
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "厂商：${Build.MANUFACTURER}")
            Text(text = "型号：${Build.MODEL} (${Build.PRODUCT})")
            Text(text = "系统：Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            Text(text = "屏幕：${screenInfo.first}x${screenInfo.second} @ ${String.format("%.1f", screenInfo.third)}fps")
            if (Build.VERSION.SDK_INT >= 31) {
                Text(text = "SOC：${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}")
            }
            Text(text = "内存：可用 ${memoryInfo.first} / 总共 ${memoryInfo.second}")
            Text(text = "存储：可用 ${storageInfo.first} / 总共 ${storageInfo.second}")
        }
        val buttonModifier = screenFocusSaver?.let { Modifier.focusSaverItem(it, "content_media_codec") } ?: Modifier
        Button(
            onClick = onOpenMediaCodec,
            modifier = buttonModifier.touchClickable(onClick = onOpenMediaCodec),
        ) {
            Text(text = "编解码信息")
        }
    }
}
