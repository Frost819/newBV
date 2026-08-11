package dev.frost819.newbv.app.ui.screen.settings

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import dev.frost819.newbv.core.focus.touchClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.frost819.newbv.app.viewmodel.settings.LogViewerViewModel
import io.github.g0dkar.qrcode.QRCode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 日志查看页。
 *
 * 左右分栏布局：左侧为日志文件列表和手动创建按钮，右侧为二维码。
 *
 * 二维码内容根据焦点动态切换：
 * - 焦点在「手动保存日志」按钮上：二维码编码服务器首页 URL
 * - 焦点在某个日志文件上：二维码编码该文件的下载 URL
 *
 * @param onBack 返回上一页。
 * @param viewModel 日志查看 ViewModel。
 */
@Composable
fun LogViewerScreen(
    onBack: () -> Unit,
    viewModel: LogViewerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedFile by remember { mutableStateOf<File?>(null) }
    var isCreateFocused by remember { mutableStateOf(true) }
    var qrImage by remember { mutableStateOf<ImageBitmap?>(null) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    LaunchedEffect(selectedFile, isCreateFocused, uiState.serverAddress) {
        if (uiState.serverAddress.isEmpty()) {
            qrImage = null
            return@LaunchedEffect
        }
        val url = if (isCreateFocused) {
            viewModel.getServerUrl()
        } else {
            selectedFile?.let { viewModel.getFileUrl(it) } ?: viewModel.getServerUrl()
        }
        qrImage = generateQrCode(url)
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier.padding(
                    start = 48.dp,
                    top = 24.dp,
                    bottom = 8.dp,
                    end = 48.dp,
                ),
            ) {
                Text(
                    text = "日志管理",
                    fontSize = 24.sp,
                )
            }
        },
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Box(
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 36.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item {
                        ListItem(
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    if (it.hasFocus) {
                                        isCreateFocused = true
                                        selectedFile = null
                                    }
                                }
                                .touchClickable(onClick = { viewModel.createManualLog() }),
                            selected = false,
                            onClick = { viewModel.createManualLog() },
                            headlineContent = {
                                Text(text = "手动保存日志")
                            },
                        )
                    }

                    items(items = uiState.logFiles, key = { it.name }) { file ->
                        ListItem(
                            modifier = Modifier.onFocusChanged {
                                if (it.hasFocus) {
                                    isCreateFocused = false
                                    selectedFile = file
                                }
                            },
                            selected = false,
                            onClick = {},
                            headlineContent = {
                                Text(text = file.name)
                            },
                            supportingContent = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(text = viewModel.getLogTypeDisplayName(file))
                                    Text(text = "${file.length() / 1024} KB")
                                }
                            },
                        )
                    }

                    if (uiState.logFiles.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = "无日志")
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "浏览器打开 ${uiState.serverAddress}\n或扫码进入日志管理",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (qrImage != null) {
                            Image(
                                modifier = Modifier.size(200.dp),
                                bitmap = qrImage!!,
                                contentDescription = null,
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (isCreateFocused) Text(text = "正在获取端口……")
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 生成二维码图片。
 *
 * @param content 二维码内容 URL。
 * @return 生成的 [ImageBitmap]，失败返回 null。
 */
private fun generateQrCode(content: String): ImageBitmap? {
    return runCatching {
        val output = ByteArrayOutputStream()
        QRCode(content).render().writeImage(output)
        val input = ByteArrayInputStream(output.toByteArray())
        BitmapFactory.decodeStream(input).asImageBitmap()
    }.getOrNull()
}
