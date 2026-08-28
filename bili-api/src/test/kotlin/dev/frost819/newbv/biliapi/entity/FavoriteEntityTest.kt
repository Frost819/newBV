package dev.frost819.newbv.biliapi.entity

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.entity.user.favorite.FavoriteFolderInfo
import dev.frost819.newbv.biliapi.http.entity.user.favorite.FavoriteFolderInfoListData
import dev.frost819.newbv.biliapi.http.entity.user.favorite.FavoriteItemId
import dev.frost819.newbv.biliapi.http.entity.user.favorite.UserFavoriteFoldersData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [FavoriteFolderItemId]、[FavoriteItemType]、[FavoriteFolderMetadata]、[FavoriteFolderData]、
 * [FavoriteItem]、[Upper] 实体的单元测试。
 *
 * 覆盖各 `fromHttp*` 转换方法的字段映射、类型枚举查找与边界情况。不依赖网络。
 */
class FavoriteEntityTest {
    // ------------------------------------------------------------------
    // FavoriteItemType.fromValue
    // ------------------------------------------------------------------

    @Test
    fun `FavoriteItemType fromValue returns correct type for known ids`() {
        assertThat(FavoriteItemType.fromValue(0)).isEqualTo(FavoriteItemType.All)
        assertThat(FavoriteItemType.fromValue(2)).isEqualTo(FavoriteItemType.Video)
        assertThat(FavoriteItemType.fromValue(12)).isEqualTo(FavoriteItemType.Audio)
        assertThat(FavoriteItemType.fromValue(21)).isEqualTo(FavoriteItemType.VideoCollection)
    }

    @Test
    fun `FavoriteItemType fromValue throws for unknown id`() {
        assertThrows<NoSuchElementException> { FavoriteItemType.fromValue(99) }
    }

    @Test
    fun `FavoriteItemType value property returns correct ids`() {
        assertThat(FavoriteItemType.All.value).isEqualTo(0)
        assertThat(FavoriteItemType.Video.value).isEqualTo(2)
        assertThat(FavoriteItemType.Audio.value).isEqualTo(12)
        assertThat(FavoriteItemType.VideoCollection.value).isEqualTo(21)
    }

    // ------------------------------------------------------------------
    // FavoriteFolderItemId.fromFavoriteItemId
    // ------------------------------------------------------------------

    @Test
    fun `FavoriteFolderItemId fromFavoriteItemId maps all fields`() {
        val httpItemId = FavoriteItemId(id = 12345L, type = 2, bvid = "BV1xx411c7mD")

        val result = FavoriteFolderItemId.fromFavoriteItemId(httpItemId)

        assertThat(result.id).isEqualTo(12345L)
        assertThat(result.type).isEqualTo(FavoriteItemType.Video)
        assertThat(result.bvid).isEqualTo("BV1xx411c7mD")
    }

    @Test
    fun `FavoriteFolderItemId fromFavoriteItemId maps audio type`() {
        val httpItemId = FavoriteItemId(id = 99L, type = 12, bvid = "")

        val result = FavoriteFolderItemId.fromFavoriteItemId(httpItemId)

        assertThat(result.type).isEqualTo(FavoriteItemType.Audio)
    }

    // ------------------------------------------------------------------
    // FavoriteFolderMetadata.fromHttpFavoriteFolderInfo
    // ------------------------------------------------------------------

    @Test
    fun `fromHttpFavoriteFolderInfo maps all fields correctly`() {
        val info = favoriteFolderInfo(favState = 1, mediaCount = 42)

        val result = FavoriteFolderMetadata.fromHttpFavoriteFolderInfo(info)

        assertThat(result.id).isEqualTo(1001L)
        assertThat(result.fid).isEqualTo(10L)
        assertThat(result.mid).isEqualTo(12345L)
        assertThat(result.title).isEqualTo("my-folder")
        assertThat(result.cover).isEqualTo("http://cover.test")
        assertThat(result.mediaCount).isEqualTo(42)
        assertThat(result.videoInThisFav).isTrue()
    }

    @Test
    fun `fromHttpFavoriteFolderInfo sets videoInThisFav false when favState is 0`() {
        val info = favoriteFolderInfo(favState = 0, mediaCount = 0)

        val result = FavoriteFolderMetadata.fromHttpFavoriteFolderInfo(info)

        assertThat(result.videoInThisFav).isFalse()
    }

    // ------------------------------------------------------------------
    // FavoriteFolderMetadata.fromHttpUserFavoriteFolder
    // ------------------------------------------------------------------

    @Test
    fun `fromHttpUserFavoriteFolder maps fields and sets cover null`() {
        val folder =
            UserFavoriteFoldersData.UserFavoriteFolder(
                id = 2001L,
                fid = 20L,
                mid = 12345L,
                attr = 0,
                title = "user-folder",
                favState = 1,
                mediaCount = 10,
            )

        val result = FavoriteFolderMetadata.fromHttpUserFavoriteFolder(folder)

        assertThat(result.id).isEqualTo(2001L)
        assertThat(result.fid).isEqualTo(20L)
        assertThat(result.mid).isEqualTo(12345L)
        assertThat(result.title).isEqualTo("user-folder")
        assertThat(result.cover).isNull()
        assertThat(result.mediaCount).isEqualTo(10)
        assertThat(result.videoInThisFav).isTrue()
    }

    @Test
    fun `fromHttpUserFavoriteFolder sets videoInThisFav false when favState is 0`() {
        val folder =
            UserFavoriteFoldersData.UserFavoriteFolder(
                id = 1L,
                fid = 1L,
                mid = 1L,
                attr = 0,
                title = "f",
                favState = 0,
                mediaCount = 0,
            )

        val result = FavoriteFolderMetadata.fromHttpUserFavoriteFolder(folder)

        assertThat(result.videoInThisFav).isFalse()
    }

    // ------------------------------------------------------------------
    // FavoriteItem.fromHttpFavoriteItem
    // ------------------------------------------------------------------

    @Test
    fun `fromHttpFavoriteItem maps all fields correctly`() {
        val httpItem = fakeFavoriteItem(id = 555L, type = 2, title = "test-video")

        val result = FavoriteItem.fromHttpFavoriteItem(httpItem)

        assertThat(result.id).isEqualTo(555L)
        assertThat(result.type).isEqualTo(FavoriteItemType.Video)
        assertThat(result.title).isEqualTo("test-video")
        assertThat(result.cover).isEqualTo("http://cover.test/555")
        assertThat(result.intro).isEqualTo("intro")
        assertThat(result.page).isEqualTo(3)
        assertThat(result.duration).isEqualTo(600)
        assertThat(result.upper.mid).isEqualTo(12345L)
        assertThat(result.upper.name).isEqualTo("up-name")
        assertThat(result.link).isEqualTo("http://link.test/555")
        assertThat(result.pubtime).isEqualTo(1000L)
        assertThat(result.bvid).isEqualTo("BV555")
    }

    @Test
    fun `fromHttpFavoriteItem maps audio type`() {
        val httpItem = fakeFavoriteItem(id = 1L, type = 12, title = "audio-item")

        val result = FavoriteItem.fromHttpFavoriteItem(httpItem)

        assertThat(result.type).isEqualTo(FavoriteItemType.Audio)
    }

    // ------------------------------------------------------------------
    // Upper.fromHttpUpper
    // ------------------------------------------------------------------

    @Test
    fun `fromHttpUpper maps all fields correctly`() {
        val httpUpper =
            dev.frost819.newbv.biliapi.http.entity.user.favorite.Upper(
                mid = 999L,
                name = "test-up",
                face = "http://face.test",
            )

        val result = Upper.fromHttpUpper(httpUpper)

        assertThat(result.mid).isEqualTo(999L)
        assertThat(result.name).isEqualTo("test-up")
        assertThat(result.face).isEqualTo("http://face.test")
    }

    // ------------------------------------------------------------------
    // FavoriteFolderData.fromHttpFavoriteFolderInfoListData
    // ------------------------------------------------------------------

    @Test
    fun `fromHttpFavoriteFolderInfoListData maps info medias and hasMore`() {
        val listData =
            FavoriteFolderInfoListData(
                info = favoriteFolderInfo(favState = 1, mediaCount = 2),
                medias =
                    listOf(
                        fakeFavoriteItem(id = 1L, type = 2, title = "v1"),
                        fakeFavoriteItem(id = 2L, type = 2, title = "v2"),
                    ),
                hasMore = true,
            )

        val result = FavoriteFolderData.fromHttpFavoriteFolderInfoListData(listData)

        assertThat(result.info.title).isEqualTo("my-folder")
        assertThat(result.info.videoInThisFav).isTrue()
        assertThat(result.medias).hasSize(2)
        assertThat(result.medias[0].title).isEqualTo("v1")
        assertThat(result.medias[1].title).isEqualTo("v2")
        assertThat(result.hasMore).isTrue()
    }

    @Test
    fun `fromHttpFavoriteFolderInfoListData with empty medias`() {
        val listData =
            FavoriteFolderInfoListData(
                info = favoriteFolderInfo(favState = 0, mediaCount = 0),
                medias = emptyList(),
                hasMore = false,
            )

        val result = FavoriteFolderData.fromHttpFavoriteFolderInfoListData(listData)

        assertThat(result.medias).isEmpty()
        assertThat(result.hasMore).isFalse()
    }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private fun favoriteFolderInfo(
        favState: Int,
        mediaCount: Int,
    ) = FavoriteFolderInfo(
        id = 1001L,
        fid = 10L,
        mid = 12345L,
        attr = 0,
        title = "my-folder",
        cover = "http://cover.test",
        upper =
            dev.frost819.newbv.biliapi.http.entity.user.favorite.Upper(
                mid = 12345L,
                name = "up-name",
                face = "http://face.test",
            ),
        coverType = 0,
        cntInfo =
            dev.frost819.newbv.biliapi.http.entity.user.favorite.CntInfo(
                collect = 0,
                play = 0,
            ),
        type = 11,
        intro = "intro",
        ctime = 0,
        mtime = 0,
        state = 0,
        favState = favState,
        likeState = 0,
        mediaCount = mediaCount,
    )

    private fun fakeFavoriteItem(
        id: Long,
        type: Int,
        title: String,
    ) = dev.frost819.newbv.biliapi.http.entity.user.favorite.FavoriteItem(
        id = id,
        type = type,
        title = title,
        cover = "http://cover.test/$id",
        intro = "intro",
        page = 3,
        duration = 600,
        upper =
            dev.frost819.newbv.biliapi.http.entity.user.favorite.Upper(
                mid = 12345L,
                name = "up-name",
                face = "http://face.test",
            ),
        attr = 0,
        cntInfo =
            dev.frost819.newbv.biliapi.http.entity.user.favorite
                .CntInfo(collect = 0, play = 0),
        link = "http://link.test/$id",
        ctime = 0,
        pubtime = 1000L,
        favTime = 0,
        bvid = "BV$id",
    )
}
