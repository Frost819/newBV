package dev.frost819.newbv.biliapi.entity.video.season

data class Section(
    val id: Long,
    val title: String,
    val episodes: List<Episode>,
) {
    companion object {
        fun fromSection(section: dev.frost819.newbv.biliapi.http.entity.video.UgcSeason.Section) =
            Section(
                id = section.id,
                title = section.title,
                episodes = section.episodes.map { Episode.fromEpisode(it) },
            )

        fun fromSection(section: bilibili.app.view.v1.Section) =
            Section(
                id = section.id,
                title = section.title,
                episodes = section.episodesList.map { Episode.fromEpisode(it) },
            )

        fun fromModule(module: dev.frost819.newbv.biliapi.http.entity.season.AppSeasonData.Module) =
            Section(
                id = module.id,
                title = module.title,
                episodes = module.data.episodes.map { Episode.fromEpisode(it) },
            )

        fun fromSection(section: dev.frost819.newbv.biliapi.http.entity.season.SeasonSection) =
            Section(
                id = section.id,
                title = section.title,
                episodes =
                    section.episodes.map { Episode.fromEpisode(it) }
                        // aid 为 0 的视频是跳转到其它 PGC 页面的链接，暂不适配
                        .filter { it.aid != 0L },
            )
    }
}
