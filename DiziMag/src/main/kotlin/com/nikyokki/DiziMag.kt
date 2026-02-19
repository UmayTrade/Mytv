package com.nikyokki

import CryptoJS
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.lagradost.cloudstream3.*
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
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override val mainPage = mainPageOf(
        "$mainUrl/dizi/tur/aile" to "Aile",
        "$mainUrl/dizi/tur/aksiyon-macera" to "Aksiyon-Macera",
        "$mainUrl/dizi/tur/dram" to "Dram",
        "$mainUrl/dizi/tur/komedi" to "Komedi"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get("${request.data}/$page").document
        val list = doc.select("div.poster-long").mapNotNull { it.toSearch() }
        return newHomePageResponse(request.name, list)
    }

    private fun Element.toSearch(): SearchResponse? {
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
        val html = app.post(
            "$mainUrl/search",
            data = mapOf("query" to query),
            referer = mainUrl
        ).body.string()

        val doc = Jsoup.parse(html)
        return doc.select("ul li").mapNotNull { it.toSearch() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url, referer = mainUrl).document
        val title = doc.selectFirst("h1")?.text() ?: return null
        val poster = fixUrlNull(doc.selectFirst("img")?.attr("src"))
        val plot = doc.selectFirst("div.series-profile-summary")?.text()

        if (url.contains("/dizi/")) {

            val episodes = mutableListOf<Episode>()
            var season = 1

            doc.select("div.series-profile-episode-list").forEach { sezon ->
                var ep = 1
                sezon.select("li a").forEach { a ->
                    val epName = a.text()
                    val epHref = fixUrlNull(a.attr("href")) ?: return@forEach

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
        val iframe = fixUrlNull(doc.selectFirst("iframe")?.attr("src")) ?: return false

        val iframeDoc = app.get(iframe, referer = mainUrl).document

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

                    callback.invoke(
                        newExtractorLink(
                            name,
                            name,
                            jsonData.videoLocation,
                            ExtractorLinkType.M3U8
                        ) {
                            this.referer = iframe
                            this.quality = Qualities.Unknown.value
                        }
                    )
                }
            }
        }

        loadExtractor(iframe, mainUrl, subtitleCallback, callback)
        return true
    }
}

/* MODELS */

data class Cipher(
    val ct: String,
    val iv: String,
    val s: String
)

data class JsonData(
    @JsonProperty("videoLocation")
    val videoLocation: String
)
