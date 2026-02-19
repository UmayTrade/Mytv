package com.nikyokki

import CryptoJS
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.regex.Pattern

class DiziMag : MainAPI() {

    override var mainUrl = "https://dizimag.eu"
    override var name = "DiziMag"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 200L
    override var sequentialMainPageScrollDelay = 200L

    override val mainPage = mainPageOf(
        "$mainUrl/dizi/tur/aile" to "Aile",
        "$mainUrl/dizi/tur/aksiyon-macera" to "Aksiyon-Macera",
        "$mainUrl/dizi/tur/animasyon" to "Animasyon",
        "$mainUrl/dizi/tur/belgesel" to "Belgesel",
        "$mainUrl/dizi/tur/bilim-kurgu-fantazi" to "Bilim Kurgu",
        "$mainUrl/dizi/tur/dram" to "Dram",
        "$mainUrl/dizi/tur/gizem" to "Gizem",
        "$mainUrl/dizi/tur/komedi" to "Komedi",
        "$mainUrl/dizi/tur/savas-politik" to "Savaş Politik",
        "$mainUrl/dizi/tur/suc" to "Suç"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get("${request.data}/$page").document
        val items = doc.select("div.poster-long").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = selectFirst("h2")?.text() ?: return null
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

    override suspend fun search(query: String): List<SearchResponse> {
        val res = app.post(
            "$mainUrl/search",
            data = mapOf("query" to query),
            headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
            referer = mainUrl
        ).body.string()

        val doc = Jsoup.parse(res)
        return doc.select("ul li").mapNotNull { it.toSearchResult() }
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
                    val epName = bolum.selectFirst("a")?.text() ?: return@forEach
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

        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Referer" to mainUrl
        )

        val doc = app.get(data, headers = headers).document
        val iframe = fixUrlNull(doc.selectFirst("iframe")?.attr("src")) ?: return false

        val iframeDoc = app.get(iframe, headers = headers, referer = mainUrl).document

        iframeDoc.select("script").forEach { script ->
            if (script.data().contains("bePlayer")) {

                val pattern = Pattern.compile("bePlayer\\('(.*?)', '(.*?)'\\)")
                val matcher = pattern.matcher(script.data())

                if (matcher.find()) {

                    val key = matcher.group(1)
                    val jsonCipher = matcher.group(2)

                    val cipher = ObjectMapper().readValue(
                        jsonCipher.replace("\\/", "/"),
                        Cipher::class.java
                    )

                    val decrypted = CryptoJS.decrypt(key, cipher.ct, cipher.iv, cipher.s)

                    val jsonData = ObjectMapper().readValue(
                        decrypted,
                        JsonData::class.java
                    )

                    jsonData.strSubtitles.forEach { sub ->
                        subtitleCallback.invoke(
                            SubtitleFile(
                                sub.label,
                                "https://epikplayer.xyz${sub.file}"
                            )
                        )
                    }

                    callback.invoke(
                        newExtractorLink(
                            source = name,
                            name = name,
                            url = jsonData.videoLocation,
                            ExtractorLinkType.M3U8
                        ) {
                            headers = mapOf("Referer" to iframe)
                            quality = Qualities.Unknown.value
                        }
                    )
                }
            }
        }

        loadExtractor(iframe, mainUrl, subtitleCallback, callback)
        return true
    }
}

/* -------------------- MODELS -------------------- */

data class Cipher(
    val ct: String,
    val iv: String,
    val s: String
)

data class JsonData(
    @JsonProperty("videoLocation")
    val videoLocation: String,

    @JsonProperty("strSubtitles")
    val strSubtitles: List<Subtitle>
)

data class Subtitle(
    val file: String,
    val label: String
)
