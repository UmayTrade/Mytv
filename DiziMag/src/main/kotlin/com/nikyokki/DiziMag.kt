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
        val doc = app.get("${request.data}/$page").document
        val list = doc.select("div.poster-long").mapNotNull { it.toSearch() }
        return newHomePageResponse(request.name, list)
    }

    private fun Element.toSearch(): SearchResponse? {
        val title = selectFirst("div.poster-long-subject h2")?.text() ?: return null
        val href = fixUrlNull(selectFirst("a")?.attr("href")) ?: return null
        val poster = fixUrlNull(selectFirst("img")?.attr("data-src"))

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

        val title = doc.selectFirst("h1")?.text() ?: return null
        val poster = fixUrlNull(doc.selectFirst("img")?.attr("src"))

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            posterUrl = poster
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
