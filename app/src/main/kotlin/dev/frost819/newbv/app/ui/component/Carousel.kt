package dev.frost819.newbv.app.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CarouselDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import coil3.compose.AsyncImage
import dev.frost819.newbv.biliapi.entity.CarouselData
import dev.frost819.newbv.core.focus.focusedBorder
import kotlinx.coroutines.delay

/**
 * PGC 轮播图组件。
 *
 * 自动滚动展示番剧/影视封面，获焦点时暂停自动滚动。
 * D-Pad 左右切换，点击跳转详情页。
 *
 * @param data 轮播图数据列表。
 * @param onClick 点击回调，传入当前轮播项。
 * @param modifier Modifier。
 */
@Composable
fun PgcCarousel(
    data: List<CarouselData.CarouselItem>,
    onClick: (CarouselData.CarouselItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    CarouselContent(
        modifier = modifier,
        data = data,
        onClick = onClick,
    )
}

@Composable
private fun CarouselContent(
    data: List<CarouselData.CarouselItem>,
    onClick: (CarouselData.CarouselItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Carousel(
        itemCount = data.size,
        modifier = modifier
            .height(240.dp)
            .clip(MaterialTheme.shapes.large)
            .focusedBorder(),
        onClick = { itemIndex ->
            onClick(data[itemIndex])
        },
    ) { itemIndex ->
        CarouselCard(data = data[itemIndex])
    }
}

@Composable
private fun CarouselCard(
    data: CarouselData.CarouselItem,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        modifier = modifier.fillMaxWidth(),
        model = data.cover,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter,
    )
}

/**
 * 通用轮播图引擎。
 *
 * 基于 [AnimatedContent] 实现自动滚动 + D-Pad 导航。
 * 自动滚动在获得焦点时暂停。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Carousel(
    itemCount: Int,
    onClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    autoScrollInterval: Long = CarouselDefaults.TimeToDisplayItemMillis,
    contentTransformStartToEnd: ContentTransform = fadeIn(tween(1000))
        .togetherWith(fadeOut(tween(1000))),
    contentTransformEndToStart: ContentTransform = fadeIn(tween(1000))
        .togetherWith(fadeOut(tween(1000))),
    content: @Composable AnimatedContentScope.(index: Int) -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }
    var isMovingBackward by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentIndex, itemCount) {
        while (true) {
            delay(autoScrollInterval)
            if (itemCount == 0 || hasFocus) continue
            isMovingBackward = false
            currentIndex = (currentIndex + 1) % itemCount
        }
    }

    Box(
        modifier = modifier
            .onFocusChanged { focusState ->
                hasFocus = focusState.isFocused
            }
            .clickable { onClick(currentIndex) }
            .pointerInput(itemCount) {
                if (itemCount == 0) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { },
                    onDragEnd = { },
                ) { _, dragAmount ->
                    if (dragAmount > 40f) {
                        isMovingBackward = true
                        currentIndex = (currentIndex - 1 + itemCount) % itemCount
                    } else if (dragAmount < -40f) {
                        isMovingBackward = false
                        currentIndex = (currentIndex + 1) % itemCount
                    }
                }
            }
            .onKeyEvent {
                when {
                    itemCount == 0 -> false
                    it.type == KeyEventType.KeyUp -> false
                    it.key == Key.DirectionLeft -> {
                        isMovingBackward = true
                        currentIndex = (currentIndex - 1 + itemCount) % itemCount
                        true
                    }

                    it.key == Key.DirectionRight -> {
                        isMovingBackward = false
                        currentIndex = (currentIndex + 1) % itemCount
                        true
                    }

                    else -> false
                }
            },
    ) {
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                if (isMovingBackward) {
                    contentTransformEndToStart
                } else {
                    contentTransformStartToEnd
                }
            },
            label = "CarouselAnimation",
        ) { activeItemIndex ->
            if (itemCount > 0) content(activeItemIndex)
        }
        CarouselDefaults.IndicatorRow(
            itemCount = itemCount,
            activeItemIndex = currentIndex,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }
}
