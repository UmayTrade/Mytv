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

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val html = RequestHelper.get(url, mainUrl) ?: return emptyList()

        val document = Jsoup.parse(html)

        return document.select("div.post-item").mapNotNull {
            val title = it.selectFirst("h3")?.text() ?: return@mapNotNull null
            val link = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = it.selectFirst("img")?.attr("src")

            newTvSeriesSearchResponse(title, link, TvType.TvSeries) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val html = RequestHelper.get(url, mainUrl) ?: return null
        val document = Jsoup.parse(html)

        val title = document.selectFirst("h1")?.text() ?: return null
        val poster = document.selectFirst("img")?.attr("src")

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries) {
            this.posterUrl = poster
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val html = RequestHelper.get(data, mainUrl) ?: return false
        val document = Jsoup.parse(html)

        val iframe = document.selectFirst("iframe")?.attr("src") ?: return false

        callback.invoke(
            ExtractorLink(
                name,
                name,
                iframe,
                data,
                Qualities.Unknown.value,
                iframe.contains(".m3u8")
            )
        )

        return true
    }
}
