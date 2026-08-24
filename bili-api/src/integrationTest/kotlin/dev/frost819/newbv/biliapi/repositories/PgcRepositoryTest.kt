package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.pgc.PgcType
import dev.frost819.newbv.biliapi.entity.pgc.index.Area
import dev.frost819.newbv.biliapi.entity.pgc.index.Copyright
import dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrder
import dev.frost819.newbv.biliapi.entity.pgc.index.IndexOrderType
import dev.frost819.newbv.biliapi.entity.pgc.index.IsFinish
import dev.frost819.newbv.biliapi.entity.pgc.index.PgcIndexData
import dev.frost819.newbv.biliapi.entity.pgc.index.Producer
import dev.frost819.newbv.biliapi.entity.pgc.index.ReleaseDate
import dev.frost819.newbv.biliapi.entity.pgc.index.SeasonMonth
import dev.frost819.newbv.biliapi.entity.pgc.index.SeasonStatus
import dev.frost819.newbv.biliapi.entity.pgc.index.SeasonVersion
import dev.frost819.newbv.biliapi.entity.pgc.index.SpokenLanguage
import dev.frost819.newbv.biliapi.entity.pgc.index.Style
import dev.frost819.newbv.biliapi.entity.pgc.index.Year
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths
import java.util.Properties
import kotlin.test.Test

class PgcRepositoryTest {
    companion object {
        private val localProperties =
            Properties().apply {
                val path = Paths.get("../local.properties").toAbsolutePath().toString()
                load(File(path).bufferedReader())
            }
        val BUVID: String =
            runCatching { localProperties.getProperty("test.buvid") }.getOrNull() ?: ""
        val SESSDATA: String =
            runCatching { localProperties.getProperty("test.sessdata") }.getOrNull() ?: ""
        val BILI_JCT: String =
            runCatching { localProperties.getProperty("test.bili_jct") }.getOrNull() ?: ""
        val UID: Long =
            runCatching { localProperties.getProperty("test.uid") }.getOrNull()?.toLongOrNull() ?: 2
        val ACCESS_TOKEN: String =
            runCatching { localProperties.getProperty("test.access_token") }.getOrNull() ?: ""
    }

    private val pgcRepository: PgcRepository = PgcRepository()

    init {
        BiliHttpApi.init(buvid3 = BUVID, sessData = SESSDATA, biliJct = BILI_JCT, mid = UID, accessToken = ACCESS_TOKEN)
    }

    @Test
    fun `get pgc carousel data`() {
        runBlocking {
            // 查询类：断言每个分类返回轮播数据
            PgcType.entries.forEach { pgcType ->
                println("pgcType: $pgcType")
                val data = pgcRepository.getCarousel(pgcType)
                println("carousel items: ${data.items.size}")
                assertThat(data.items).isNotEmpty()
            }
        }
    }

    @Test
    fun `get pgc feed data`() {
        runBlocking {
            // 查询类：断言每个分类返回 feed 数据（部分分类 feed 可能为空，断言结构正常）
            PgcType.entries.forEach { pgcType ->
                println("pgcType: $pgcType")
                val data =
                    pgcRepository.getFeed(
                        pgcType = pgcType,
                        cursor = 0,
                    )
                println("feed items: ${data.items.size}")
                assertThat(data.items).isNotNull()
            }
        }
    }

    @Test
    fun `get pgc index`() {
        runBlocking {
            // 查询类：断言每个分类返回索引数据
            PgcType.entries.forEach { pgcType ->
                println("pgcType: $pgcType")
                val data =
                    pgcRepository.getPgcIndex(
                        pgcType = pgcType,
                        indexOrder = IndexOrder.PlayCount,
                        indexOrderType = IndexOrderType.Desc,
                        seasonVersion = SeasonVersion.All,
                        spokenLanguage = SpokenLanguage.All,
                        area = Area.All,
                        isFinish = IsFinish.All,
                        copyright = Copyright.All,
                        seasonStatus = SeasonStatus.All,
                        seasonMonth = SeasonMonth.All,
                        producer = Producer.All,
                        year = Year.All,
                        releaseDate = ReleaseDate.All,
                        style = Style.All,
                        page = PgcIndexData.PgcIndexPage(),
                    )
                println("index items: ${data.list.size}")
                assertThat(data.list).isNotEmpty()
            }
        }
    }
}
