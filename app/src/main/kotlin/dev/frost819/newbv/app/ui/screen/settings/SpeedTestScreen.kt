package dev.frost819.newbv.app.ui.screen.settings

import android.util.Base64
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebView.setWebContentsDebuggingEnabled
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.frost819.newbv.data.datastore.Prefs

/**
 * CDN 测速页。
 *
 * 嵌入 B 站测速页面（`video-diagnostics.html`），注入登录 Cookie，
 * 调整 CSS 缩放以适配 TV 大屏。
 *
 * @param onBack 返回回调。
 */
@Composable
fun SpeedTestScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    var loading by remember { mutableStateOf(true) }

    BackHandler(onBack = onBack)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = with(LocalDensity.current) {
            this@BoxWithConstraints.maxWidth.toPx().toInt()
        }

        val webViewClient = object : androidx.webkit.WebViewClientCompat() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val css = """
                .container {
                    width: 1920px !important;
                    height: 1080px !important;
                }
                """.trimIndent()
                val encoded = Base64.encodeToString(css.toByteArray(), Base64.NO_WRAP)
                view?.loadUrl(
                    "javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.innerHTML = window.atob('$encoded');" +
                        "parent.appendChild(style)" +
                        "})()",
                )
                view?.setInitialScale(((width / 1920f) * 100).toInt())
                loading = false
            }
        }

        CookieManager.getInstance().apply {
            val cookies = mapOf(
                "DedeUserID" to Prefs.uid.toString(),
                "DedeUserID__ckMd5" to Prefs.uidCkMd5,
                "SESSDATA" to Prefs.sessData,
                "bili_jct" to Prefs.biliJct,
                "sid" to Prefs.sid,
            )
            cookies.forEach { (name, value) ->
                setCookie(".bilibili.com", "$name=$value")
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    this.webViewClient = webViewClient
                    setWebContentsDebuggingEnabled(true)
                    settings.apply {
                        userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36"
                        javaScriptEnabled = true
                    }
                    loadUrl("https://www.bilibili.com/blackboard/video-diagnostics.html")
                }
            },
        )

        if (loading) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                colors = SurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.9f),
                ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "加载中...", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}
