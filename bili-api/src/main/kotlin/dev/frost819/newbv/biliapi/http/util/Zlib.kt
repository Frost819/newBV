package dev.frost819.newbv.biliapi.http.util

import io.ktor.utils.io.core.use
import org.brotli.dec.BrotliInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

fun ByteArray.zlibCompress(): ByteArray {
    val output = ByteArray(this.size * 4)
    val compressor =
        Deflater().apply {
            setInput(this@zlibCompress)
            finish()
        }
    val compressedDataLength: Int = compressor.deflate(output)
    return output.copyOfRange(0, compressedDataLength)
}

fun ByteArray.zlibDecompress(): ByteArray {
    val inflater = Inflater()
    val outputStream = ByteArrayOutputStream()
    return outputStream.use {
        val buffer = ByteArray(1024)
        inflater.setInput(this)
        var count = -1
        while (count != 0) {
            count = inflater.inflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        inflater.end()
        outputStream.toByteArray()
    }
}

/**
 * Brotli 解压。
 *
 * B 站 WebSocket 直播弹幕协议 version=3 使用 brotli 压缩，
 * 解压后与 zlib 解压结果相同：一个或多个带头部的弹幕帧。
 */
fun ByteArray.brotliDecompress(): ByteArray {
    val outputStream = ByteArrayOutputStream()
    outputStream.use { out ->
        ByteArrayInputStream(this).use { input ->
            BrotliInputStream(input).use { brotliInput ->
                val buffer = ByteArray(1024)
                while (true) {
                    val read = brotliInput.read(buffer)
                    if (read == -1) break
                    out.write(buffer, 0, read)
                }
            }
        }
    }
    return outputStream.toByteArray()
}
