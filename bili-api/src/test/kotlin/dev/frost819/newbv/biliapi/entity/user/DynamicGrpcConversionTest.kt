package dev.frost819.newbv.biliapi.entity.user

import bilibili.app.dynamic.v2.DynModuleType
import bilibili.app.dynamic.v2.cardVideoDynList
import bilibili.app.dynamic.v2.dynVideoReply
import bilibili.app.dynamic.v2.dynamicItem
import bilibili.app.dynamic.v2.mdlDynArchive
import bilibili.app.dynamic.v2.mdlDynPGC
import bilibili.app.dynamic.v2.module
import bilibili.app.dynamic.v2.moduleAuthor
import bilibili.app.dynamic.v2.moduleDesc
import bilibili.app.dynamic.v2.moduleDynamic
import bilibili.app.dynamic.v2.userInfo
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [DynamicVideo] / [DynamicVideoData] gRPC 转换方法的单元测试。
 *
 * 覆盖 `fromDynamicVideoItem(DynamicItem)` 的 DYN_ARCHIVE / DYN_PGC / 未知类型分支，
 * 以及 `fromDynamicData(DynVideoReply)` 的字段映射。
 */
class DynamicGrpcConversionTest {
    @Test
    fun `fromDynamicVideoItem grpc archive maps all fields`() {
        val item =
            dynamicItem {
                modules +=
                    module {
                        moduleType = DynModuleType.module_author
                        moduleAuthor =
                            moduleAuthor {
                                mid = 999L
                                ptimeLabelText = "2024-01-01 12:00"
                                author =
                                    userInfo {
                                        mid = 999L
                                        name = "gRPC UP"
                                    }
                            }
                    }
                modules +=
                    module {
                        moduleType = DynModuleType.module_dynamic
                        moduleDynamic =
                            moduleDynamic {
                                dynArchive =
                                    mdlDynArchive {
                                        title = "gRPC视频标题"
                                        cover = "http://cover.grpc"
                                        avid = 200L
                                        bvid = "BV200"
                                        cid = 300L
                                        coverLeftText1 = "10:30"
                                        coverLeftText2 = "5万"
                                        coverLeftText3 = "200"
                                    }
                            }
                    }
            }

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video).isNotNull()
        video!!
        assertThat(video.aid).isEqualTo(200L)
        assertThat(video.bvid).isEqualTo("BV200")
        assertThat(video.cid).isEqualTo(300L)
        assertThat(video.title).isEqualTo("gRPC视频标题")
        assertThat(video.cover).isEqualTo("http://cover.grpc")
        assertThat(video.author).isEqualTo("gRPC UP")
        assertThat(video.authorMid).isEqualTo(999L)
        assertThat(video.duration).isEqualTo(630)
        assertThat(video.play).isEqualTo(50000)
        assertThat(video.danmaku).isEqualTo(200)
        assertThat(video.pubTime).isEqualTo("2024-01-01")
    }

    @Test
    fun `fromDynamicVideoItem grpc archive with dynamic video prefix uses desc text`() {
        val item =
            dynamicItem {
                modules +=
                    module {
                        moduleType = DynModuleType.module_author
                        moduleAuthor =
                            moduleAuthor {
                                ptimeLabelText = "动态视频 2024-01-01"
                                author =
                                    userInfo {
                                        mid = 1L
                                        name = "UP"
                                    }
                            }
                    }
                modules +=
                    module {
                        moduleType = DynModuleType.module_dynamic
                        moduleDynamic =
                            moduleDynamic {
                                dynArchive =
                                    mdlDynArchive {
                                        title = "原标题"
                                        cover = ""
                                        avid = 1L
                                        bvid = "BV1"
                                        cid = 2L
                                    }
                            }
                    }
                modules +=
                    module {
                        moduleType = DynModuleType.module_desc
                        moduleDesc = moduleDesc { text = "动态视频｜实际标题" }
                    }
            }

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video!!.title).isEqualTo("实际标题")
    }

    @Test
    fun `fromDynamicVideoItem grpc pgc maps all fields`() {
        val item =
            dynamicItem {
                modules +=
                    module {
                        moduleType = DynModuleType.module_author
                        moduleAuthor =
                            moduleAuthor {
                                ptimeLabelText = "2024-03-15"
                                author =
                                    userInfo {
                                        mid = 888L
                                        name = "番剧UP"
                                    }
                            }
                    }
                modules +=
                    module {
                        moduleType = DynModuleType.module_dynamic
                        moduleDynamic =
                            moduleDynamic {
                                dynPgc =
                                    mdlDynPGC {
                                        title = "番剧标题"
                                        cover = "http://cover.pgc"
                                        cid = 400L
                                        seasonId = 500L
                                        epid = 600L
                                        aid = 700L
                                        coverLeftText1 = "24:00"
                                        coverLeftText2 = "100万"
                                        coverLeftText3 = "5000"
                                    }
                            }
                    }
            }

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video).isNotNull()
        video!!
        assertThat(video.aid).isEqualTo(700L)
        assertThat(video.bvid).isNull()
        assertThat(video.cid).isEqualTo(400L)
        assertThat(video.epid).isEqualTo(600)
        assertThat(video.seasonId).isEqualTo(500)
        assertThat(video.title).isEqualTo("番剧标题")
        assertThat(video.cover).isEqualTo("http://cover.pgc")
        assertThat(video.author).isEqualTo("番剧UP")
        assertThat(video.authorMid).isEqualTo(888L)
        assertThat(video.duration).isEqualTo(1440)
        assertThat(video.play).isEqualTo(1000000)
        assertThat(video.danmaku).isEqualTo(5000)
    }

    @Test
    fun `fromDynamicVideoItem grpc unknown module type returns null`() {
        val item =
            dynamicItem {
                modules +=
                    module {
                        moduleType = DynModuleType.module_author
                        moduleAuthor =
                            moduleAuthor {
                                ptimeLabelText = "2024-01-01"
                                author =
                                    userInfo {
                                        mid = 1L
                                        name = "UP"
                                    }
                            }
                    }
                modules +=
                    module {
                        moduleType = DynModuleType.module_dynamic
                        moduleDynamic =
                            moduleDynamic {
                                // moduleItemCase is not set → default/unknown
                            }
                    }
            }

        val video = DynamicVideo.fromDynamicVideoItem(item)

        assertThat(video).isNull()
    }

    @Test
    fun `fromDynamicData grpc maps list and pagination`() {
        val reply =
            dynVideoReply {
                dynamicList =
                    cardVideoDynList {
                        list += fakeArchiveDynamicItem(aid = 100L, title = "视频1")
                        list += fakeArchiveDynamicItem(aid = 200L, title = "视频2")
                        hasMore = true
                        historyOffset = "offset-789"
                        updateBaseline = "baseline-012"
                    }
            }

        val result = DynamicVideoData.fromDynamicData(reply)

        assertThat(result.videos).hasSize(2)
        assertThat(result.videos[0].aid).isEqualTo(100L)
        assertThat(result.videos[1].aid).isEqualTo(200L)
        assertThat(result.hasMore).isTrue()
        assertThat(result.historyOffset).isEqualTo("offset-789")
        assertThat(result.updateBaseline).isEqualTo("baseline-012")
    }

    @Test
    fun `fromDynamicData grpc with empty list returns empty videos`() {
        val reply =
            dynVideoReply {
                dynamicList =
                    cardVideoDynList {
                        hasMore = false
                    }
            }

        val result = DynamicVideoData.fromDynamicData(reply)

        assertThat(result.videos).isEmpty()
        assertThat(result.hasMore).isFalse()
    }

    @Test
    fun `fromDynamicData grpc filters out null items`() {
        val reply =
            dynVideoReply {
                dynamicList =
                    cardVideoDynList {
                        list += fakeArchiveDynamicItem(aid = 100L, title = "有效视频")
                        list += fakeUnknownDynamicItem()
                        hasMore = true
                    }
            }

        val result = DynamicVideoData.fromDynamicData(reply)

        assertThat(result.videos).hasSize(1)
        assertThat(result.videos[0].title).isEqualTo("有效视频")
    }

    private fun fakeArchiveDynamicItem(
        aid: Long,
        title: String,
    ) = dynamicItem {
        modules +=
            module {
                moduleType = DynModuleType.module_author
                moduleAuthor =
                    moduleAuthor {
                        ptimeLabelText = "2024-01-01 12:00"
                        author =
                            userInfo {
                                mid = 1L
                                name = "UP"
                            }
                    }
            }
        modules +=
            module {
                moduleType = DynModuleType.module_dynamic
                moduleDynamic =
                    moduleDynamic {
                        dynArchive =
                            mdlDynArchive {
                                this.title = title
                                cover = "http://cover.test"
                                avid = aid
                                bvid = "BV$aid"
                                cid = aid * 10
                            }
                    }
            }
    }

    private fun fakeUnknownDynamicItem() =
        dynamicItem {
            modules +=
                module {
                    moduleType = DynModuleType.module_author
                    moduleAuthor =
                        moduleAuthor {
                            ptimeLabelText = "2024-01-01"
                            author =
                                userInfo {
                                    mid = 1L
                                    name = "UP"
                                }
                        }
                }
            modules +=
                module {
                    moduleType = DynModuleType.module_dynamic
                    moduleDynamic = moduleDynamic {}
                }
        }
}
