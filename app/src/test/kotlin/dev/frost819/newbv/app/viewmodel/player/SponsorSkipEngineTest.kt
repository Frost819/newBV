package dev.frost819.newbv.app.viewmodel.player

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.app.entity.player.SponsorActionType
import dev.frost819.newbv.app.entity.player.SponsorSegment
import org.junit.jupiter.api.Test

/**
 * [SponsorSkipEngine] 的单元测试。
 *
 * 验证空降跳过状态机：向前跨入触发、fresh load 后首检在段内也触发（prev=0 视为跨入）、
 * 宽限期内不误判回拉且跨入降级为手动、回拉重置消费记录但不回退游标、
 * 用户 seek 重定位游标、UUID 一次消费、reset 清空。
 */
class SponsorSkipEngineTest {
    private val engine = SponsorSkipEngine()

    /** 固定时钟起点，宽限期/回拉判定均基于显式注入的 nowMs。 */
    private var nowMs = 1_000_000L

    private fun skipSegment(
        uuid: String = "u1",
        startMs: Long = 10_000L,
        endMs: Long = 20_000L,
        category: String = "sponsor",
    ) = SponsorSegment(
        uuid = uuid,
        category = category,
        actionType = SponsorActionType.SKIP,
        startMs = startMs,
        endMs = endMs,
    )

    private fun muteSegment(
        uuid: String = "m1",
        startMs: Long = 30_000L,
        endMs: Long = 35_000L,
    ) = SponsorSegment(
        uuid = uuid,
        category = "sponsor",
        actionType = SponsorActionType.MUTE,
        startMs = startMs,
        endMs = endMs,
    )

    @Test
    fun `returns null when no segments loaded`() {
        assertThat(engine.checkPosition(15_000L, nowMs)).isNull()
    }

    @Test
    fun `auto skips when position crosses into segment start while playing forward`() {
        engine.loadSegments(listOf(skipSegment()))

        engine.checkPosition(5_000L, nowMs)
        val result = engine.checkPosition(11_000L, nowMs + 1)

        assertThat(result).isNotNull()
        assertThat(result!!.action).isEqualTo(SponsorSkipEngine.SkipAction.AUTO_SKIP)
        assertThat(result.segment.uuid).isEqualTo("u1")
    }

    @Test
    fun `auto skips on first check when position starts inside segment`() {
        engine.loadSegments(listOf(skipSegment()))

        // fresh load 后首检 lastPosition=0：断点续播落在恰饭段内时同样自动跳过
        val result = engine.checkPosition(15_000L, nowMs)

        assertThat(result!!.action).isEqualTo(SponsorSkipEngine.SkipAction.AUTO_SKIP)
    }

    @Test
    fun `backward seek into a passed segment does not re-trigger via detection`() {
        engine.loadSegments(listOf(skipSegment("u1", startMs = 10_000L, endMs = 60_000L)))

        engine.checkPosition(5_000L, nowMs)
        assertThat(engine.checkPosition(70_000L, nowMs + 1)).isNull() // 越过段尾，游标推进
        val backIn = engine.checkPosition(20_000L, nowMs + 2) // 回拉进段内

        // 游标只前进不回退：自动检测不再触发，用户 seek（onUserSeek）才重定位
        assertThat(backIn).isNull()
    }

    @Test
    fun `each segment is consumed only once`() {
        engine.loadSegments(listOf(skipSegment(startMs = 10_000L, endMs = 60_000L)))

        engine.checkPosition(9_000L, nowMs)
        val first = engine.checkPosition(11_000L, nowMs + 1)
        val second = engine.checkPosition(15_000L, nowMs + 2)

        assertThat(first!!.action).isEqualTo(SponsorSkipEngine.SkipAction.AUTO_SKIP)
        assertThat(second).isNull()
    }

    @Test
    fun `grace period after auto skip suppresses seek-back reset`() {
        engine.loadSegments(
            listOf(skipSegment(uuid = "u1", startMs = 10_000L, endMs = 20_000L)),
        )

        engine.checkPosition(9_000L, nowMs)
        engine.checkPosition(11_000L, nowMs + 1) // AUTO_SKIP → 宽限期开启
        // 宽限期内 seek 落点回传不触发回拉重置，也不重复触发
        val duringGrace = engine.checkPosition(10_500L, nowMs + 2)

        assertThat(duringGrace).isNull()
    }

    @Test
    fun `entering a segment during grace period degrades to manual action`() {
        engine.loadSegments(
            listOf(
                skipSegment("u1", startMs = 10_000L, endMs = 20_000L),
                skipSegment("u2", startMs = 25_000L, endMs = 35_000L),
            ),
        )

        engine.checkPosition(9_000L, nowMs)
        val first = engine.checkPosition(11_000L, nowMs + 1) // AUTO_SKIP u1 → 宽限期开启
        val blocked = engine.checkPosition(26_000L, nowMs + 2) // 宽限期内跨入 u2

        assertThat(first!!.action).isEqualTo(SponsorSkipEngine.SkipAction.AUTO_SKIP)
        assertThat(blocked!!.action).isEqualTo(SponsorSkipEngine.SkipAction.SHOW_BUTTON)

        // u2 未被消费；宽限期结束后 prev 已在段内，仍不自动触发（该片段放过）
        val after = engine.checkPosition(27_000L, nowMs + 4_000)

        assertThat(after!!.action).isEqualTo(SponsorSkipEngine.SkipAction.SHOW_BUTTON)
    }

    @Test
    fun `seek back beyond threshold re-arms consumption but keeps cursor`() {
        engine.loadSegments(
            listOf(
                skipSegment("u1", startMs = 10_000L, endMs = 20_000L),
                skipSegment("u2", startMs = 50_000L, endMs = 60_000L),
            ),
        )

        engine.checkPosition(9_000L, nowMs)
        engine.checkPosition(11_000L, nowMs + 1) // AUTO_SKIP u1
        engine.checkPosition(30_000L, nowMs + 2) // 越过 u1 尾
        nowMs += 10_000L // 出宽限期
        engine.checkPosition(5_000L, nowMs + 1) // 回拉：u1 消费记录被清，游标仍指向 u2

        // 游标不回退：u1 不会再次自动触发
        assertThat(engine.checkPosition(11_000L, nowMs + 2)).isNull()

        // u2 正常自动触发
        engine.checkPosition(49_000L, nowMs + 3)
        val u2 = engine.checkPosition(51_000L, nowMs + 4)

        assertThat(u2!!.action).isEqualTo(SponsorSkipEngine.SkipAction.AUTO_SKIP)
        assertThat(u2.segment.uuid).isEqualTo("u2")
    }

    @Test
    fun `mute action returned for mute segments without grace period`() {
        engine.loadSegments(listOf(muteSegment()))

        engine.checkPosition(29_000L, nowMs)
        val result = engine.checkPosition(31_000L, nowMs + 1)

        assertThat(result!!.action).isEqualTo(SponsorSkipEngine.SkipAction.MUTE)
        // 静音不进入宽限期；已消费的片段不重复触发
        assertThat(engine.checkPosition(31_000L, nowMs + 2)).isNull()
    }

    @Test
    fun `onUserSeek repositions cursor and re-arms passed segments`() {
        engine.loadSegments(listOf(skipSegment()))

        engine.checkPosition(9_000L, nowMs)
        engine.checkPosition(11_000L, nowMs + 1) // 消费 u1
        engine.onUserSeek(5_000L)

        val reEnter = engine.checkPosition(11_000L, nowMs + 2)

        assertThat(reEnter!!.action).isEqualTo(SponsorSkipEngine.SkipAction.AUTO_SKIP)
    }

    @Test
    fun `onUserSeek past all segments disables triggering`() {
        engine.loadSegments(listOf(skipSegment()))

        engine.onUserSeek(99_000L)

        assertThat(engine.checkPosition(15_000L, nowMs)).isNull()
    }

    @Test
    fun `reset clears segments and consumption records`() {
        engine.loadSegments(listOf(skipSegment()))
        engine.checkPosition(11_000L, nowMs)
        engine.reset()

        assertThat(engine.checkPosition(11_000L, nowMs + 1)).isNull()
    }
}
