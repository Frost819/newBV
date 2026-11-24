package dev.frost819.newbv.app.viewmodel.player

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CdnCandidateOrderTest {
    private val akamai = "https://upos-hz-mirrorakam.akamaized.net/upgcxcode/1-1-30280.m4s?e=x"
    private val cosov = "https://upos-sz-mirrorcosov.bilivideo.com/upgcxcode/1-1-30280.m4s?e=x"
    private val ali = "https://upos-sz-mirrorali.bilivideo.com/upgcxcode/1-1-30280.m4s?e=x"

    @Test
    fun `akamai host moves behind other candidates`() {
        assertThat(listOf(akamai, cosov).openRangeRejectingHostsLast())
            .containsExactly(cosov, akamai)
            .inOrder()
    }

    @Test
    fun `other candidates keep their order`() {
        assertThat(listOf(cosov, akamai, ali).openRangeRejectingHostsLast())
            .containsExactly(cosov, ali, akamai)
            .inOrder()
    }

    @Test
    fun `single akamai candidate is kept`() {
        assertThat(listOf(akamai).openRangeRejectingHostsLast()).containsExactly(akamai)
    }
}
