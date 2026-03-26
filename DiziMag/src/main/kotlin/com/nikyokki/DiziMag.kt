package com.nikyokki

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
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 100L

    private val commonHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Referer" to "$mainUrl/"
    )

    override val mainPage = mainPageOf(
        "${mainUrl}/kesfet/eyJ0eXBlIjoic2VyaWVzIn0=" to "Yeni Eklenen Diziler",
        "${mainUrl}/dizi/tur/aksiyon-macera" to "Aksiyon & Macera",
        "${mainUrl}/dizi/tur/bilim-kurgu-fantazi" to "Bilim Kurgu & Fantastik",
        "${mainUrl}/dizi/tur/komedi" to "Komedi",
        "${mainUrl}/dizi/tur/suc" to "Suç",
        "${mainUrl}/film/tur/aksiyon" to "Aksiyon Filmleri",
        "${mainUrl}/film/tur/bilim-kurgu" to "Bilim-Kurgu Filmleri",
        "${mainUrl}/film/tur/korku" to "Korku Filmleri",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}/${page}"
        val response = app.get(url, headers = commonHeaders)
        val document = Jsoup.parse(response.body.string())
        
        val home = document.select("div.filter-result-box, li.w-1\\/2, div.grid-items > li, div.series-list li").mapNotNull { 
            it.diziler() 
        }

        return newHomePageResponse(request.name, home, hasNext = home.isNotEmpty())
    }

    private fun Element.diziler(): SearchResponse? {
        val title = this.selectFirst("h2, h3, span.truncate")?.text() ?: return null
        val anchor = this.selectFirst("a") ?: return null
        val href = fixUrlNull(anchor.attr("href")) ?: return null
        
        val img = this.selectFirst("img")
        val posterUrl = fixUrlNull(img?.attr("data-src")?.ifEmpty { img.attr("src") } ?: img?.attr("src"))

        return if (href.contains("/dizi/")) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchReq = app.post(
            "${mainUrl}/search",
            data = mapOf("query" to query),
            headers = commonHeaders.plus("X-Requested-With" to "XMLHttpRequest"),
            referer = "${mainUrl}/"
        ).parsedSafe<SearchResult>()

        if (searchReq?.success != true) return emptyList()

        val document = Jsoup.parse(searchReq.theme.toString())
        return document.select("li").mapNotNull { it.diziler() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, headers = commonHeaders).document
        
        val title = document.selectFirst("div.page-title h1 a")?.text() 
            ?: document.selectFirst("div.page-title h1")?.text()?.split("(")?.first()?.trim() 
            ?: return null
            
        val poster = fixUrlNull(document.selectFirst("div.series-profile-image img")?.attr("src"))
        val year = document.selectFirst("h1 span")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val rating = document.selectFirst("span.color-imdb")?.text()?.trim()
        val plot = document.selectFirst("div.series-profile-summary p")?.text()?.trim()
        
        val actors = document.select("div.series-profile-cast li").mapNotNull {
            val name = it.selectFirst("h5")?.text() ?: return@mapNotNull null
            val img = fixUrlNull(it.selectFirst("img")?.attr("data-src"))
            Actor(name, img)
        }

        if (url.contains("/dizi/")) {
            val episodes = mutableListOf<Episode>()
            document.select("div.series-profile-episode-list").forEachIndexed { sIndex, season ->
                season.select("li").forEachIndexed { eIndex, bolum ->
                    val epAnchor = bolum.selectFirst("h6 a") ?: return@forEachIndexed
                    episodes.add(newEpisode(fixUrlNull(epAnchor.attr("href")) ?: "") {
                        this.name = epAnchor.text()
                        this.season = sIndex + 1
                        this.episode = eIndex + 1
                    })
                }
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = plot
                this.score = Score.from10(rating)
                addActors(actors)
            }
        } else {
            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = plot
                this.score = Score.from10(rating)
                addActors(actors)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val response = app.get(data, headers = commonHeaders)
        val document = response.document
        val iframe = fixUrlNull(document.selectFirst("div#tv-spoox2 iframe")?.attr("src")) ?: return false

        val iframeRes = app.get(iframe, headers = commonHeaders, referer = data).document
        
        iframeRes.select("script").forEach { sc ->
            val scriptContent = sc.html()
            if (scriptContent.contains("bePlayer")) {
                val pattern = Pattern.compile("bePlayer\\('(.*?)', '(.*?)'\\)")
                val matcher = pattern.matcher(scriptContent)
                
                if (matcher.find()) {
                    val key = matcher.group(1)
                    val jsonCipher = matcher.group(2)

                    try {
                        val cipherData = ObjectMapper().readValue(
                            jsonCipher?.replace("\\/", "/"),
                            Cipher::class.java
                        )
                        
                        val decrypt = key?.let { CryptoJS.decrypt(it, cipherData.ct, cipherData.iv, cipherData.s) }
                        val jsonData = ObjectMapper().readValue(decrypt, JsonData::class.java)

                        jsonData.strSubtitles?.forEach { sub ->
                            subtitleCallback.invoke(
                                SubtitleFile(sub.label.toString(), "https://epikplayer.xyz${sub.file}")
                            )
                        }

                        if (!jsonData.videoLocation.isNullOrEmpty()) {
                            callback.invoke(
                                newExtractorLink(
                                    source = this.name,
                                    name = this.name,
                                    url = jsonData.videoLocation!!,
                                    referer = iframe,
                                    type = ExtractorLinkType.M3U8,
                                    quality = Qualities.Unknown.value
                                )
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return true
    }

    data class SearchResult(val success: Boolean, val theme: Any?)
    data class Cipher(val ct: String, val iv: String, val s: String)
    data class JsonData(
        val videoLocation: String?,
        val strSubtitles: List<Subtitles>?
    )
    data class Subtitles(val file: String, val label: String)
}
