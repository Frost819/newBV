package dev.frost819.newbv.player

import android.content.Context
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * OkHttp 工具类，用于生成带自定义 SSL 配置的 OkHttpClient。
 *
 * 主要解决 B 站视频域名（bilivideo.com / bilivideo.cn）证书互通问题：
 * - 加载系统 CA 证书库
 * - 追加自定义 CA 证书（如 GlobalSign ECC Root CA R5）
 * - 允许 bilivideo.com 和 bilivideo.cn 域名证书互通
 */
object OkHttpUtil {

    /**
     * 创建带自定义 SSL 配置的 OkHttpClient。
     *
     * @param context Android Context，用于读取 assets 中的 CA 证书文件
     * @return 配置好 SSL 和域名验证的 OkHttpClient
     */
    fun generateCustomSslOkHttpClient(context: Context): OkHttpClient {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val customCaMap = mapOf(
            "custom:r5" to "GlobalSign ECC Root CA R5.crt"
        )

        val keyStoreType = KeyStore.getDefaultType()
        val systemKeyStore = KeyStore.getInstance("AndroidCAStore").apply {
            load(null, null)
        }
        val customKeyStore = KeyStore.getInstance(keyStoreType).apply {
            load(null, null)

            // 复制系统 CA 证书
            systemKeyStore.aliases().toList().forEach {
                setCertificateEntry(it, systemKeyStore.getCertificate(it))
            }
            // 追加自定义 CA 证书
            customCaMap.forEach { (alias, caFilename) ->
                val certificateInputStream = context.assets.open(caFilename)
                val certificate = certificateFactory.generateCertificate(certificateInputStream)
                setCertificateEntry(alias, certificate)
            }
        }

        val tmfAlgorithm: String = TrustManagerFactory.getDefaultAlgorithm()
        val trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm).apply {
            init(customKeyStore)
        }

        val sslContext: SSLContext = SSLContext.getInstance("TLS").apply {
            init(null, trustManagerFactory.trustManagers, null)
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(
                sslContext.socketFactory,
                trustManagerFactory.trustManagers[0] as X509TrustManager
            )
            .hostnameVerifier { hostname, session ->
                // 允许 bilivideo.com 和 bilivideo.cn 域名证书互通
                val biliDomains = listOf("bilivideo.com", "bilivideo.cn")
                val isBiliDomain = biliDomains.any { domain ->
                    hostname == domain || hostname.endsWith(".$domain")
                }
                if (isBiliDomain) {
                    true
                } else {
                    HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
                }
            }
            .build()
    }
}
