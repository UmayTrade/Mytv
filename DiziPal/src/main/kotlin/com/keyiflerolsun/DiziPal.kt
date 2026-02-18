package com.keyiflerolsun

import android.util.Base64
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class DiziPal : MainAPI() {

    override var mainUrl = "https://dizipal1539.com"
    override var name = "DiziPal"
    override var lang = "tr"

    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override val mainPage = mainPageOf(
        "$mainUrl/yabanci-dizi-izle" to "Diziler",
        "$mainUrl/hd-film-izle" to "Filmler"
    )

    // ================= MAIN PAGE =================

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {

        val url = if (page == 1) request.data
        else "${request.data}/page/$page"

        val document = app.get(url).document

        val home = document.select("article, li").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, home)
    }

    // ================= SEARCH =================

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("article, li").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun quickSearch(query: String) = search(query)

    // ================= POSTER FIX =================

    private fun Element.getPoster(): String? {

        var poster = selectFirst("img")?.attr("data-src")
            ?: selectFirst("img")?.attr("data-original")
            ?: selectFirst("img")?.attr("data-lazy-src")
            ?: selectFirst("img")?.attr("src")

        if (poster.isNullOrBlank()) return null

        if (poster.startsWith("//")) {
            poster = "https:$poster"
        }

        return fixUrl(poster)
    }

    private fun Element.toSearchResult(): SearchResponse? {

        val link = selectFirst("a") ?: return null
        val title = link.attr("title").ifBlank { link.text() }
        val href = fixUrlNull(link.attr("href")) ?: return null
        val poster = getPoster()

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

    // ================= LOAD =================

    override suspend fun load(url: String): LoadResponse? {

        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text() ?: return null
        val poster = document.getPoster()
        val plot = document.selectFirst("p")?.text()

        return if (url.contains("/dizi/")) {

            val episodes = document.select("a[href*='/bolum/']").mapNotNull {

                val epUrl = fixUrl(it.attr("href"))

                newEpisode(epUrl) {
                    this.name = it.text()
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

        val iframe = document.selectFirst("iframe")?.attr("src")
            ?: return false

        val iframeUrl = fixUrl(iframe)

        val body = app.get(iframeUrl).text

        // m3u8 regex
        Regex("""https?:\/\/[^"' ]+\.m3u8[^"' ]*""")
            .findAll(body)
            .map { it.value }
            .distinct()
            .forEach {

                M3u8Helper.generateM3u8(
                    source = name,
                    name = name,
                    streamUrl = it,
                    referer = mainUrl
                ).forEach(callback)

                return true
            }

        // Base64 fallback
        Regex("""["']([A-Za-z0-9+/=]{100,})["']""")
            .find(body)
            ?.groupValues?.getOrNull(1)
            ?.let {
                try {
                    val decoded = String(Base64.decode(it, Base64.DEFAULT))
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

        // Extractor fallback
        loadExtractor(
            url = iframeUrl,
            referer = mainUrl,
            subtitleCallback = subtitleCallback,
            callback = callback
        )

        return true
    }
}
