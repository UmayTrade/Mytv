package com.nikyokki

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class DiziMag : MainAPI() {

    override var mainUrl = "https://dizimag.eu"
    override var name = "DiziMag"
    override val hasMainPage = true
    override var lang = "tr"
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 250L
    override var sequentialMainPageScrollDelay = 250L

    override val mainPage = mainPageOf(
        "$mainUrl/kategori/aile" to "Aile",
        "$mainUrl/kategori/aksiyon-macera" to "Aksiyon-Macera",
        "$mainUrl/kategori/animasyon" to "Animasyon",
        "$mainUrl/kategori/belgesel" to "Belgesel",
        "$mainUrl/kategori/bilim-kurgu-fantazi" to "Bilim Kurgu",
        "$mainUrl/kategori/dram" to "Dram",
        "$mainUrl/kategori/gizem" to "Gizem",
        "$mainUrl/kategori/komedi" to "Komedi",
        "$mainUrl/kategori/savas-politik" to "Savaş Politik",
        "$mainUrl/kategori/suc" to "Suç"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get("${request.data}/page/$page", referer = mainUrl).document
        val list = doc.select("div.poster-long").mapNotNull { it.toSearch() }
        return newHomePageResponse(request.name, list)
    }

    private fun Element.toSearch(): SearchResponse? {
        val title = selectFirst("div.poster-long-subject h2")?.text() ?: return null
        val href = fixUrlNull(selectFirst("div.poster-long-subject a")?.attr("href")) ?: return null
        val poster = fixUrlNull(selectFirst("div.poster-long-image img")?.attr("data-src"))

        return if (href.contains("/dizi/")) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                posterUrl = poster
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url, referer = mainUrl).document

        val title = doc.selectFirst("div.page-title h1 a")?.text()
            ?: doc.selectFirst("h1")?.text()
            ?: return null

        val poster = fixUrlNull(
            doc.selectFirst("div.series-profile-image img")?.attr("src")
        )

        val plot = doc.selectFirst("div.series-profile-summary p")?.text()

        if (url.contains("/dizi/")) {

            val episodes = mutableListOf<Episode>()
            var seasonNumber = 1

            doc.select("div.series-profile-episode-list").forEach { seasonBlock ->
                var episodeNumber = 1

                seasonBlock.select("li").forEach { ep ->
                    val epName = ep.selectFirst("h6.truncate a")?.text() ?: return@forEach
                    val epHref = fixUrlNull(ep.selectFirst("a")?.attr("href")) ?: return@forEach

                    episodes.add(
                        newEpisode(epHref) {
                            name = epName
                            season = seasonNumber
                            episode = episodeNumber++
                        }
                    )
                }
                seasonNumber++
            }

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                posterUrl = poster
                this.plot = plot
            }

        } else {

            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                posterUrl = poster
                this.plot = plot
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val doc = app.get(data, referer = mainUrl).document
        val iframe = fixUrlNull(doc.selectFirst("iframe")?.attr("src")) ?: return false

        loadExtractor(iframe, mainUrl, subtitleCallback, callback)
        return true
    }
}
