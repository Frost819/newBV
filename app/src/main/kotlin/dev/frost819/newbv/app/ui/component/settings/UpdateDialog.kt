package dev.frost819.newbv.app.ui.component.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.tv.material3.Button
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.frost819.newbv.BuildConfig
import dev.frost819.newbv.app.network.GithubApi
import dev.frost819.newbv.app.network.entity.GithubRelease
import dev.frost819.newbv.app.util.CacheManager
import dev.frost819.newbv.core.focus.touchClickable
import dev.frost819.newbv.core.log.Loggers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

/**
 * 更新检查弹窗。
 *
 * 打开后自动检查最新版本，支持下载 APK 并调起安装。
 * 下载进度通过 [LinearProgressIndicator] 实时显示。
 *
 * Android 8.0 及以上系统会按来源应用管控「安装未知应用」权限。若未授权，
 * 安装器会被系统静默拦截（[android.content.Context.startActivity] 不抛异常），
 * 弹窗会停在「安装中」造成假死。因此调起安装前先检查该权限，未授权时明确
 * 提示用户自行到系统设置中授权后重试，不再展示无限等待状态。
 *
 * @param show 是否显示弹窗。
 * @param onHideDialog 关闭弹窗回调。
 */
@Composable
fun UpdateDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = Loggers.get("UpdateDialog")

    var updateStatus by remember { mutableStateOf(UpdateStatus.UpdatingInfo) }
    var bytesSentTotal by remember { mutableLongStateOf(0L) }
    var contentLength by remember { mutableLongStateOf(0L) }
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val progress by animateFloatAsState(targetValue = targetProgress, label = "update progress")
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var latestRelease by remember { mutableStateOf<GithubRelease?>(null) }
    // 暂存已下载的 APK，用户授权后重试安装时无需重新下载
    var pendingInstallFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(show) {
        if (!show) {
            downloadJob?.cancel()
        }
        onDispose {
            downloadJob?.cancel()
        }
    }

    val checkUpdate: () -> Unit = {
        updateStatus = UpdateStatus.UpdatingInfo
        scope.launch(Dispatchers.IO) {
            runCatching {
                latestRelease = GithubApi.getLatestBuild()
                val revision =
                    latestRelease!!
                        .assets
                        .first { it.name.startsWith("newBV") && it.name.contains("release") }
                        .name
                        .split("_")[1]
                        .toInt()
                if (revision <= BuildConfig.VERSION_CODE) {
                    updateStatus = UpdateStatus.NoAvailableUpdate
                    return@launch
                }
            }.onFailure {
                logger.error(it) { "Failed to get latest version" }
                updateStatus = UpdateStatus.CheckError
            }.onSuccess {
                logger.info { "Find latest version ${latestRelease!!.name}" }
                updateStatus = UpdateStatus.Ready
            }
        }
    }

    /** 调起系统安装器安装已下载的 APK。 */
    val launchInstaller: (File) -> Unit = { file ->
        updateStatus = UpdateStatus.Installing
        runCatching {
            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${BuildConfig.APPLICATION_ID}.provider",
                    file,
                )
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(intent)
        }.onFailure {
            logger.error(it) { "Failed to launch installer" }
            updateStatus = UpdateStatus.InstallError
        }
    }

    val installUpdate: (File) -> Unit = { file ->
        pendingInstallFile = file
        if (context.canRequestPackageInstallPermission()) {
            launchInstaller(file)
        } else {
            // 未授权时系统会静默拦截安装器，直接进入「安装中」会假死
            updateStatus = UpdateStatus.InstallPermissionRequired
        }
    }

    /** 用户自行在系统设置中授权后，点击重试再次安装。 */
    val retryInstall: () -> Unit = {
        val file = pendingInstallFile
        if (file != null && context.canRequestPackageInstallPermission()) {
            launchInstaller(file)
        }
    }

    val startUpdate: () -> Unit = {
        updateStatus = UpdateStatus.Downloading
        downloadJob =
            scope.launch(Dispatchers.IO) {
                val tempFilename =
                    latestRelease!!
                        .assets
                        .first { it.name.startsWith("newBV") && it.name.contains("release") }
                        .name
                val tempDir = File(context.cacheDir, "update_downloader")
                if (!tempDir.exists()) tempDir.mkdirs()
                val tempFile = File(tempDir, tempFilename)
                tempFile.createNewFile()
                runCatching {
                    GithubApi.downloadUpdate(
                        release = latestRelease!!,
                        file = tempFile,
                        onProgress = { downloaded, total ->
                            bytesSentTotal = downloaded
                            contentLength = total
                            targetProgress =
                                if (total > 0) {
                                    downloaded.toFloat() / total
                                } else {
                                    0f
                                }
                        },
                    )
                    // 缓存写入后检查阈值，保留刚下载的 APK 待安装
                    CacheManager(context).checkCache(preserve = tempFile)
                    if (show) installUpdate(tempFile)
                }.onFailure {
                    logger.error(it) { "Failed to download update" }
                    updateStatus = UpdateStatus.DownloadError
                }
            }
    }

    LaunchedEffect(show) {
        if (show) {
            checkUpdate()
        } else {
            updateStatus = UpdateStatus.UpdatingInfo
        }
    }

    if (show) {
        AlertDialog(
            modifier = modifier.width(400.dp),
            onDismissRequest = onHideDialog,
            title = {
                Text(
                    text =
                        when (updateStatus) {
                            UpdateStatus.UpdatingInfo -> "获取更新信息中"
                            UpdateStatus.Ready -> latestRelease?.versionLabel ?: "Loading..."
                            UpdateStatus.Downloading -> "下载中"
                            UpdateStatus.Installing -> "安装中"
                            UpdateStatus.NoAvailableUpdate -> "无可用更新"
                            UpdateStatus.CheckError -> "检查更新失败"
                            UpdateStatus.DownloadError -> "下载失败"
                            UpdateStatus.InstallError -> "安装失败"
                            UpdateStatus.InstallPermissionRequired -> "需要安装权限"
                        },
                )
            },
            text = {
                when (updateStatus) {
                    UpdateStatus.UpdatingInfo -> {
                        Text(text = "检查更新中...")
                    }

                    UpdateStatus.Ready -> {
                        Text(text = latestRelease?.body ?: "Empty content")
                    }

                    UpdateStatus.Downloading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Text(
                                    text = "${bytesSentTotal.toMBString()}/${contentLength.toMBString()}",
                                )
                            }
                        }
                    }

                    UpdateStatus.Installing -> {
                        Text(text = "请坐和放宽")
                    }

                    UpdateStatus.DownloadError -> {
                        Text(text = "下载失败")
                    }

                    UpdateStatus.InstallError -> {
                        Text(text = "安装失败")
                    }

                    UpdateStatus.InstallPermissionRequired -> {
                        Text(
                            text =
                                "未授予「安装未知应用」权限，安装器无法启动。" +
                                    "请到系统设置中允许 newBV 安装应用，然后点击「重试」。",
                        )
                    }

                    UpdateStatus.CheckError -> {
                        Text(text = "获取更新信息失败")
                    }

                    UpdateStatus.NoAvailableUpdate -> {
                        Text(text = "真没更新，骗你是小狗！")
                    }
                }
            },
            confirmButton = {
                when (updateStatus) {
                    UpdateStatus.UpdatingInfo, UpdateStatus.NoAvailableUpdate,
                    UpdateStatus.Downloading, UpdateStatus.Installing,
                    -> {}

                    UpdateStatus.Ready -> {
                        Button(
                            onClick = startUpdate,
                            modifier = Modifier.touchClickable(onClick = startUpdate),
                        ) {
                            Text(text = "立即更新")
                        }
                    }

                    UpdateStatus.InstallPermissionRequired -> {
                        Button(
                            onClick = retryInstall,
                            modifier = Modifier.touchClickable(onClick = retryInstall),
                        ) {
                            Text(text = "重试")
                        }
                    }

                    UpdateStatus.InstallError, UpdateStatus.DownloadError, UpdateStatus.CheckError -> {
                        Button(
                            onClick = checkUpdate,
                            modifier = Modifier.touchClickable(onClick = checkUpdate),
                        ) {
                            Text(text = "再试一次")
                        }
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    enabled =
                        updateStatus !in
                            setOf(
                                UpdateStatus.Downloading,
                                UpdateStatus.Installing,
                            ),
                    onClick = onHideDialog,
                    modifier =
                        Modifier.touchClickable(onClick = {
                            if (updateStatus !in
                                setOf(
                                    UpdateStatus.Downloading,
                                    UpdateStatus.Installing,
                                )
                            ) {
                                onHideDialog()
                            }
                        }),
                ) {
                    Text(
                        text =
                            when (updateStatus) {
                                UpdateStatus.UpdatingInfo -> "我点错了"
                                UpdateStatus.Ready -> "打死不更"
                                UpdateStatus.NoAvailableUpdate -> "走了走了"
                                UpdateStatus.CheckError, UpdateStatus.DownloadError,
                                UpdateStatus.InstallError, UpdateStatus.InstallPermissionRequired,
                                -> "算了算了"
                                UpdateStatus.Downloading, UpdateStatus.Installing -> "你已经无路可逃！"
                            },
                    )
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        )
    }
}

/** 更新状态。 */
enum class UpdateStatus {
    UpdatingInfo,
    Ready,
    Downloading,
    Installing,
    NoAvailableUpdate,
    CheckError,
    DownloadError,
    InstallError,

    /** 缺少「安装未知应用」权限，需用户自行到系统设置授权后重试。 */
    InstallPermissionRequired,
}

/**
 * 判断当前应用是否已被允许安装 APK。
 *
 * Android 8.0（API 26）起系统按来源应用管控「安装未知应用」权限；
 * 更低版本只有全局开关，无法按应用查询，直接视为可安装。
 */
private fun Context.canRequestPackageInstallPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
    return packageManager.canRequestPackageInstalls()
}

private fun Long.toMBString(): String =
    when {
        this >= 1_000_000 -> "%.1f MB".format(this / 1_000_000.0)
        this >= 1_000 -> "%.1f KB".format(this / 1_000.0)
        else -> "$this B"
    }
