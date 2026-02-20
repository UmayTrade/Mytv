package com.nikyokki

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class FullHDFilmIzlede : MainAPI() {

    override var mainUrl = "https://www.hdfilmizle.to"
    override var name = "HdFilmIzle"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "$mainUrl/tur/aile-1" to "Aile",
        "$mainUrl/tur/aksiyon-2" to "Aksiyon",
        "$mainUrl/tur/komedi-1" to "Komedi",
        "$mainUrl/tur/dram-1" to "Dram",
        "$mainUrl/tur/korku-1" to "Korku",
        "$mainUrl/tur/bilim-kurgu-1" to "Bilim Kurgu"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) {
            request.data
        } else {
            "${request.data}/sayfa/$page"
        }

        val document = app.get(url).document
        val home = document.select("li.movie").mapNotNull { it.toSearchResponse() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        val title = selectFirst("div.movieName a")?.text() ?: return null
        val href = fixUrlNull(selectFirst("div.movieName a")?.attr("href")) ?: return null
        val poster = fixUrlNull(selectFirst("img")?.attr("src"))
        val scoreText = selectFirst("div.Imdb")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = poster
            this.score = Score.from10(scoreText?.filter { it.isDigit() || it == '.' })
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.post(
            "$mainUrl/ara",
            headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            data = mapOf("kelime" to query)
        ).document

        return document.select("li.movie").mapNotNull { it.toSearchResponse() }
    }

    override suspend fun quickSearch(query: String) = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("div.movieBar h2")
            ?.text()?.replace(" izle", "")?.trim() ?: return null

        val poster = fixUrlNull(document.selectFirst("div.moviePoster img")?.attr("src"))
        val description = document.selectFirst("div.movieDescription h2")?.text()?.trim()

        val year = document.select("span:contains(Yapım Yılı)")
            .next()?.text()?.filter { it.isDigit() }?.toIntOrNull()

        val rating = document.selectFirst("span.imdb")
            ?.text()?.filter { it.isDigit() || it == '.' }

        val actors = document.select("span:contains(Oyuncular)")
            .next()?.text()?.split(",")?.map { it.trim() }

        val trailerId = document.selectFirst("a.js-modal-btn")
            ?.attr("data-video-id")

        val trailer = trailerId?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.score = Score.from10(rating)
            addActors(actors)
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val document = app.get(data).document
        val iframeSrc = document.selectFirst("iframe")?.attr("src") ?: return false
        val iframeUrl = fixUrl(iframeSrc)

        val iframeDoc = app.get(iframeUrl, referer = "$mainUrl/").document

        val script = iframeDoc.select("script")
            .firstOrNull { it.data().contains("sources") }
            ?.data() ?: return false

        val file = script.substringAfter("file:\"").substringBefore("\"")

        if (file.isBlank()) return false

        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = file,
                type = ExtractorLinkType.M3U8
            ) {
                referer = "$mainUrl/"
                quality = Qualities.Unknown.value
            }
        )

        return true
    }

    data class FHISource(
        @JsonProperty("file") val file: String? = null,
        @JsonProperty("label") val label: String? = null,
        @JsonProperty("kind") val kind: String? = null
    )
}
