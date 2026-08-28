package com.kuaishou.akdanmaku.ui

import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.render.SimpleRenderer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [DanmakuPlayer.clearData] 的 Robolectric 测试。
 *
 * 验证切视频场景下旧弹幕数据的完整清理：实体移除、id 去重集合复位。
 * 注意 [DanmakuPlayer.updateData] 是增量追加语义，这是 clearData 存在的原因。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DanmakuPlayerClearDataTest {
    /** 构造一条滚动弹幕数据。 */
    private fun item(
        id: Long,
        positionMs: Long,
    ) = DanmakuItemData(
        danmakuId = id,
        position = positionMs,
        content = "danmaku-$id",
        mode = DanmakuItemData.DANMAKU_MODE_ROLLING,
        textSize = 25,
        textColor = Color.WHITE,
    )

    @Test
    fun `clearData removes entities and allows re-adding same danmakuId`() {
        val player = DanmakuPlayer(SimpleRenderer())
        player.updateData(listOf(item(1, 1_000)))
        // preAct 驱动 DataSystem：处理待加入数据 → 切片 → 创建实体
        player.engine.preAct()
        assertThat(player.engine.entities.size()).isEqualTo(1)

        // 清空后实体全部移除
        player.clearData()
        player.engine.preAct()
        assertThat(player.engine.entities.size()).isEqualTo(0)

        // 同 id 弹幕可重新加入并创建实体（idSet 已随 clearData 复位）
        player.updateData(listOf(item(1, 2_000)))
        player.engine.preAct()
        assertThat(player.engine.entities.size()).isEqualTo(1)

        player.release()
    }

    @Test
    fun `clearData on fresh player keeps engine usable`() {
        val player = DanmakuPlayer(SimpleRenderer())
        player.clearData()
        player.engine.preAct()

        player.updateData(listOf(item(2, 500)))
        player.engine.preAct()
        assertThat(player.engine.entities.size()).isEqualTo(1)

        player.release()
    }
}
