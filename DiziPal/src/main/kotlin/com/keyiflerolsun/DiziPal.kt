package com.keyiflerolsun

import android.util.Base64
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class DiziPal : MainAPI() {

    override var mainUrl = "https://dizipal1539.com" // AKTİF DOMAIN
    override var name = "DiziPal"
    override var lang = "tr"

    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override val mainPage = mainPageOf(
        "$mainUrl/yabanci-dizi-izle" to "Diziler",
        "$mainUrl/hd-film-izle" to "Filmler",
        "$mainUrl/anime" to "Anime",
        "$mainUrl/kanal/exxen" to "Exxen",
        "$mainUrl/kanal/disney" to "Disney+",
        "$mainUrl/kanal/netflix" to "Netflix",
        "$mainUrl/kanal/amazon" to "Amazon",
        "$mainUrl/kanal/apple-tv" to "Apple TV+",
        "$mainUrl/kanal/max" to "Max",
        "$mainUrl/kanal/hulu" to "Hulu",
        "$mainUrl/kanal/tod" to "TOD",
        "$mainUrl/kanal/tabii" to "tabii",
    )

    // ================= MAIN PAGE =================

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data).document

        val home = document.select("article, li").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = selectFirst("a")?.attr("title")
            ?: selectFirst("img")?.attr("alt")
            ?: return null

        val href = fixUrlNull(selectFirst("a")?.attr("href")) ?: return null
        val poster = fixUrlNull(selectFirst("img")?.attr("src"))

        return if (href.contains("/film/")) {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
            }
        } else {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = poster
            }
        }
    }

    // ================= SEARCH =================

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("article, li").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    // ================= LOAD =================

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1, h5")?.text() ?: return null
        val poster = fixUrlNull(document.selectFirst("img")?.attr("src"))
        val plot = document.selectFirst("p")?.text()

        return if (url.contains("/dizi/")) {

            val episodes = document.select("a[href*='/bolum/']").map {
                val epName = it.text()
                val epUrl = fixUrl(it.attr("href"))

                newEpisode(epUrl) {
                    this.name = epName
                }
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = plot
            }

        } else {

            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = plot
            }
        }
    }

    // ================= ADVANCED LOAD LINKS =================

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val document = app.get(data).document

        val iframeRaw = document.selectFirst("iframe")?.attr("src")
            ?: document.selectFirst("iframe")?.attr("data-src")
            ?: return false

        val iframeUrl = fixUrl(iframeRaw)

        val iframeResponse = app.get(
            iframeUrl,
            referer = mainUrl,
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to mainUrl
            )
        )

        var body = iframeResponse.text

        // 1️⃣ Direkt m3u8
        Regex("""https?:\/\/[^"' ]+\.m3u8[^"' ]*""")
            .findAll(body)
            .map { it.value }
            .distinct()
            .forEach { streamUrl ->

                M3u8Helper.generateM3u8(
                    source = name,
                    name = name,
                    streamUrl = streamUrl,
                    referer = mainUrl
                ).forEach(callback)

                return true
            }

        // 2️⃣ JWPlayer file
        Regex("""file\s*:\s*["']([^"']+)""")
            .find(body)
            ?.groupValues?.getOrNull(1)
            ?.let { streamUrl ->

                M3u8Helper.generateM3u8(
                    source = name,
                    name = name,
                    streamUrl = fixUrl(streamUrl),
                    referer = mainUrl
                ).forEach(callback)

                return true
            }

        // 3️⃣ Base64 decode denemesi
        Regex("""["']([A-Za-z0-9+/=]{100,})["']""")
            .findAll(body)
            .forEach { match ->

                try {
                    val decoded = String(Base64.decode(match.groupValues[1], Base64.DEFAULT))

                    Regex("""https?:\/\/[^"' ]+\.m3u8[^"' ]*""")
                        .find(decoded)
                        ?.groupValues?.getOrNull(0)
                        ?.let { streamUrl ->

                            M3u8Helper.generateM3u8(
                                source = name,
                                name = name,
                                streamUrl = streamUrl,
                                referer = mainUrl
                            ).forEach(callback)

                            return true
                        }

                } catch (_: Exception) {}
            }

        // 4️⃣ Extractor fallback
        loadExtractor(
            url = iframeUrl,
            referer = mainUrl,
            subtitleCallback = subtitleCallback,
            callback = callback
        )

        return true
    }
}
