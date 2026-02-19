package com.nikyokki

import CryptoJS
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.util.regex.Pattern

class DiziMag : MainAPI() {

    override var mainUrl = "https://dizimag.eu"
    override var name = "DiziMag"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 250L
    override var sequentialMainPageScrollDelay = 250L

    override val mainPage = mainPageOf(

        // DİZİ
        "$mainUrl/kategori/aile" to "Aile",
        "$mainUrl/kategori/aksiyon-macera" to "Aksiyon-Macera",
        "$mainUrl/kategori/animasyon" to "Animasyon",
        "$mainUrl/kategori/belgesel" to "Belgesel",
        "$mainUrl/kategori/bilim-kurgu-fantazi" to "Bilim Kurgu",
        "$mainUrl/kategori/dram" to "Dram",
        "$mainUrl/kategori/gizem" to "Gizem",
        "$mainUrl/kategori/komedi" to "Komedi",
        "$mainUrl/kategori/savas-politik" to "Savaş Politik",
        "$mainUrl/kategori/suc" to "Suç",

        // FİLM
        "$mainUrl/kategori/aile" to "Aile Film",
        "$mainUrl/kategori/animasyon" to "Animasyon Film",
        "$mainUrl/kategori/bilim-kurgu" to "Bilim-Kurgu Film",
        "$mainUrl/kategori/dram" to "Dram Film",
        "$mainUrl/kategori/fantastik" to "Fantastik Film",
        "$mainUrl/kategori/gerilim" to "Gerilim Film",
        "$mainUrl/kategori/gizem" to "Gizem Film",
        "$mainUrl/kategori/komedi" to "Komedi Film",
        "$mainUrl/kategori/korku" to "Korku Film",
        "$mainUrl/kategori/macera" to "Macera Film",
        "$mainUrl/kategori/romantik" to "Romantik Film",
        "$mainUrl/kategori/savas" to "Savaş Film",
        "$mainUrl/kategori/suc" to "Suç Film",
        "$mainUrl/kategori/tarih" to "Tarih Film",
        "$mainUrl/kategori/vahsi-bati" to "Vahşi Batı Film",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get("${request.data}/$page").document
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

        val title = doc.selectFirst("div.page-title h1 a")?.text() ?: return null
        val poster = fixUrlNull(doc.selectFirst("div.series-profile-image img")?.attr("src"))
        val plot = doc.selectFirst("div.series-profile-summary p")?.text()

        if (url.contains("/dizi/")) {

            val episodes = mutableListOf<Episode>()
            var season = 1

            doc.select("div.series-profile-episode-list").forEach { sezon ->
                var ep = 1
                sezon.select("li").forEach { bolum ->
                    val epName = bolum.selectFirst("h6.truncate a")?.text() ?: return@forEach
                    val epHref = fixUrlNull(bolum.selectFirst("a")?.attr("href")) ?: return@forEach

                    episodes.add(
                        newEpisode(epHref) {
                            name = epName
                            this.season = season
                            episode = ep++
                        }
                    )
                }
                season++
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
        val iframe = fixUrlNull(doc.selectFirst("div#tv-spoox2 iframe")?.attr("src")) ?: return false

        loadExtractor(iframe, mainUrl, subtitleCallback, callback)
        return true
    }
}
