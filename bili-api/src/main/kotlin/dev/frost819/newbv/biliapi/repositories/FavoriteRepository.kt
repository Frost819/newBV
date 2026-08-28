package dev.frost819.newbv.biliapi.repositories

import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.FavoriteFolderData
import dev.frost819.newbv.biliapi.entity.FavoriteFolderMetadata
import dev.frost819.newbv.biliapi.entity.FavoriteItemType
import dev.frost819.newbv.biliapi.http.BiliHttpApi

class FavoriteRepository(
    private val authRepository: AuthRepository,
) {
    suspend fun checkVideoFavoured(
        aid: Long,
        preferApiType: ApiType,
    ): Boolean =
        BiliHttpApi.checkVideoFavoured(
            avid = aid,
            accessKey = authRepository.accessToken.takeIf { preferApiType == ApiType.App },
        )

    suspend fun addVideoToFavoriteFolder(
        aid: Long,
        addMediaIds: List<Long>,
        preferApiType: ApiType,
    ) {
        BiliHttpApi.setVideoToFavorite(
            avid = aid,
            type = FavoriteItemType.Video.value,
            addMediaIds = addMediaIds,
            csrf = authRepository.biliJct.takeIf { preferApiType == ApiType.Web },
            accessKey = authRepository.accessToken.takeIf { preferApiType == ApiType.App },
        )
    }

    suspend fun delVideoFromFavoriteFolder(
        aid: Long,
        delMediaIds: List<Long>,
        preferApiType: ApiType,
    ) {
        BiliHttpApi.setVideoToFavorite(
            avid = aid,
            type = FavoriteItemType.Video.value,
            delMediaIds = delMediaIds,
            csrf = authRepository.biliJct.takeIf { preferApiType == ApiType.Web },
            accessKey = authRepository.accessToken.takeIf { preferApiType == ApiType.App },
        )
    }

    suspend fun updateVideoToFavoriteFolder(
        aid: Long,
        addMediaIds: List<Long>,
        delMediaIds: List<Long>,
        preferApiType: ApiType,
    ) {
        BiliHttpApi.setVideoToFavorite(
            avid = aid,
            type = FavoriteItemType.Video.value,
            addMediaIds = addMediaIds,
            delMediaIds = delMediaIds,
            csrf = authRepository.biliJct.takeIf { preferApiType == ApiType.Web },
            accessKey = authRepository.accessToken.takeIf { preferApiType == ApiType.App },
        )
    }

    suspend fun getAllFavoriteFolderMetadataList(
        mid: Long,
        type: FavoriteItemType = FavoriteItemType.Video,
        rid: Long? = null,
        preferApiType: ApiType,
    ): List<FavoriteFolderMetadata> {
        val userFavoriteFoldersData =
            BiliHttpApi
                .getAllFavoriteFoldersInfo(
                    mid = mid,
                    type = type.value,
                    rid = rid,
                    accessKey = authRepository.accessToken.takeIf { preferApiType == ApiType.App },
                ).getResponseData()
        return userFavoriteFoldersData.list.map {
            FavoriteFolderMetadata.fromHttpUserFavoriteFolder(it)
        }
    }

    suspend fun getFavoriteFolderData(
        mediaId: Long,
        pageSize: Int = 20,
        pageNumber: Int = 1,
        preferApiType: ApiType,
    ): FavoriteFolderData {
        val favoriteFolderListData =
            BiliHttpApi
                .getFavoriteList(
                    mediaId = mediaId,
                    pageSize = pageSize,
                    pageNumber = pageNumber,
                    accessKey = authRepository.accessToken.takeIf { preferApiType == ApiType.App },
                ).getResponseData()
        return FavoriteFolderData.fromHttpFavoriteFolderInfoListData(favoriteFolderListData)
    }
}
