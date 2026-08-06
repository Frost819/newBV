package dev.frost819.newbv.app.viewmodel.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.biliapi.entity.ApiType as BiliApiType
import dev.frost819.newbv.biliapi.repositories.ToViewRepository
import dev.frost819.newbv.data.datastore.ApiType as DataApiType
import dev.frost819.newbv.data.datastore.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WatchLaterEffect {
    data class ShowToast(val message: String) : WatchLaterEffect
}

/**
 * 稍后再看通用 ViewModel。
 *
 * 可在任意页面通过 `hiltViewModel<WatchLaterViewModel>()` 获取，
 * 提供添加/移除稍后再看的能力，无需修改各页面已有的 ViewModel。
 *
 * @param toViewRepository 稍后再看仓库。
 */
@HiltViewModel
class WatchLaterViewModel @Inject constructor(
    private val toViewRepository: ToViewRepository,
) : ViewModel() {

    private val logger = KotlinLogging.logger("WatchLaterViewModel")

    private val _effect = MutableSharedFlow<WatchLaterEffect>()
    val effect = _effect.asSharedFlow()

    private fun prefApiType(): BiliApiType = when (Prefs.apiType) {
        DataApiType.Web -> BiliApiType.Web
        DataApiType.App -> BiliApiType.App
    }

    /**
     * 添加视频到稍后再看。
     */
    fun addToView(aid: Long, bvid: String? = null) {
        viewModelScope.launch {
            runCatching {
                toViewRepository.addToView(
                    aid = aid,
                    bvid = bvid,
                    preferApiType = prefApiType(),
                )
            }.onSuccess {
                _effect.emit(WatchLaterEffect.ShowToast("已添加到稍后再看"))
            }.onFailure { error ->
                if (error is CancellationException) throw error
                logger.error(error) { "Failed to add to view" }
                _effect.emit(WatchLaterEffect.ShowToast("添加失败: ${error.message ?: "未知错误"}"))
            }
        }
    }
}
