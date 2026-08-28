package dev.frost819.newbv.biliapi.entity.pgc.index

import dev.frost819.newbv.biliapi.entity.pgc.PgcItem

data class PgcIndexData(
    val list: List<PgcItem>,
    val nextPage: PgcIndexPage,
) {
    companion object {
        fun fromIndexResultData(data: dev.frost819.newbv.biliapi.http.entity.index.IndexResultData): PgcIndexData =
            PgcIndexData(
                list = data.list.map { PgcItem.fromIndexResultItem(it) },
                nextPage =
                    PgcIndexPage(
                        currentPage = data.num,
                        pageSize = data.size,
                        totalSize = data.total,
                        nextPage = data.num + 1,
                        hasNext = data.hasNext == 1,
                    ),
            )
    }

    data class PgcIndexPage(
        val currentPage: Int = 1,
        val pageSize: Int = 20,
        val totalSize: Int = 0,
        val nextPage: Int = 1,
        val hasNext: Boolean = true,
    )
}
