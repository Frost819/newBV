package dev.frost819.newbv.app.ui.screen.login

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.frost819.newbv.R
import dev.frost819.newbv.app.viewmodel.login.LoginViewModel
import dev.frost819.newbv.biliapi.entity.login.QrLoginState
import io.github.g0dkar.qrcode.QRCode

/**
 * 扫码登录页面。
 *
 * 默认使用 TV QR 登录（App 接口），适用于 Android TV 设备。
 * 用户通过 B 站手机 App 扫码完成登录。
 *
 * @param viewModel 登录 ViewModel。
 * @param onLoginSuccess 登录成功回调（用于导航返回）。
 */
@Composable
fun QrLoginContent(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val statusColor = when (uiState.state) {
        QrLoginState.Error, QrLoginState.Expired -> MaterialTheme.colorScheme.error
        QrLoginState.Success -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }

    LaunchedEffect(Unit) {
        viewModel.requestAppQrCode()
    }

    LaunchedEffect(uiState.state) {
        if (uiState.state == QrLoginState.Success) {
            onLoginSuccess()
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.cancelPolling() }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = modifier
                .focusable()
                .fillMaxSize()
                .clickable {
                    if (uiState.state == QrLoginState.Expired || uiState.state == QrLoginState.Error) {
                        viewModel.requestAppQrCode()
                    }
                }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key == Key.DirectionCenter) {
                        if (uiState.state == QrLoginState.Expired || uiState.state == QrLoginState.Error) {
                            viewModel.requestAppQrCode()
                        }
                        true
                    } else {
                        false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(36.dp),
            ) {
                AnimatedVisibility(
                    visible = uiState.state == QrLoginState.WaitingForScan ||
                        uiState.state == QrLoginState.WaitingForConfirm,
                ) {
                    QrCodeImage(
                        url = uiState.qrUrl,
                        modifier = Modifier.size(240.dp),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = when (uiState.state) {
                            QrLoginState.Ready, QrLoginState.RequestingQRCode ->
                                stringResource(R.string.login_requesting)
                            QrLoginState.WaitingForScan ->
                                stringResource(R.string.login_wait_for_scan)
                            QrLoginState.WaitingForConfirm ->
                                stringResource(R.string.login_wait_for_confirm)
                            QrLoginState.Expired ->
                                stringResource(R.string.login_expired)
                            QrLoginState.Success ->
                                stringResource(R.string.login_success)
                            QrLoginState.Error, QrLoginState.Unknown ->
                                uiState.errorMessage.ifEmpty { stringResource(R.string.login_error) }
                        },
                        style = MaterialTheme.typography.displaySmall,
                        color = statusColor,
                    )
                    AnimatedVisibility(
                        visible = uiState.state == QrLoginState.Expired ||
                            uiState.state == QrLoginState.Error,
                    ) {
                        Text(
                            text = stringResource(R.string.login_retry),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 26.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 将 URL 渲染为二维码图片。
 *
 * 使用 [QRCode] 库生成 Bitmap，转为 [androidx.compose.ui.graphics.ImageBitmap] 显示。
 *
 * @param url 二维码内容 URL。
 * @param modifier 修饰符。
 */
@Composable
private fun QrCodeImage(
    url: String,
    modifier: Modifier = Modifier,
) {
    if (url.isEmpty()) return
    val qrImage = remember(url) {
        val graphics = QRCode(url).render()
        val bitmap = graphics.nativeImage() as android.graphics.Bitmap
        bitmap.asImageBitmap()
    }
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .border(2.dp, MaterialTheme.colorScheme.border, MaterialTheme.shapes.large)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            modifier = Modifier.size(200.dp),
            bitmap = qrImage,
            contentDescription = "QR Code",
        )
    }
}
