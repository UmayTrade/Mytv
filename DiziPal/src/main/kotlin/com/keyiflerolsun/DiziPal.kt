package com.keyiflerolsun

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

class DiziPal : MainAPI() {

    override var mainUrl = "https://dizipal1539.com"
    override var name = "DiziPal"
    override val hasMainPage = true
    override var lang = "tr"

    override val supportedTypes = setOf(
        TvType.TvSeries,
        TvType.Movie
    )

    // ---------------- SEARCH ----------------

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document

        return document.select("div.post-item").mapNotNull {
            val title = it.selectFirst("h3")?.text() ?: return@mapNotNull null
            val link = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = it.selectFirst("img")?.attr("src")

            newTvSeriesSearchResponse(title, link, TvType.TvSeries) {
                this.posterUrl = poster
            }
        }
    }

    // ---------------- LOAD ----------------

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text() ?: return null
        val poster = document.selectFirst("img")?.attr("src")

        val episodes = mutableListOf<Episode>()

        document.select("div.episode-item a").forEach {
            val epName = it.text()
            val epLink = it.attr("href")

            episodes.add(
                newEpisode(epLink) {
                    this.name = epName
                }
            )
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
        }
    }

    // ---------------- LINKS ----------------

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val document = app.get(data).document
        val iframe = document.selectFirst("iframe")?.attr("src") ?: return false

        callback.invoke(
            newExtractorLink(
                name,
                name,
                iframe,
                data,
                Qualities.Unknown.value
            )
        )

        return true
    }
}
