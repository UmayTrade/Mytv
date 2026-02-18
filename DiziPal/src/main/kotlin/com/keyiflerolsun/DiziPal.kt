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

    // ================= SEARCH =================

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=$query").document
        return doc.select("article").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String) = search(query)

    // ================= PARSER =================

    private fun Element.toSearchResult(): SearchResponse? {

        val link = selectFirst("a") ?: return null
        val title = link.attr("title").ifBlank { link.text() }
        val href = fixUrlNull(link.attr("href")) ?: return null
        val poster = selectFirst("img")?.attr("src")?.let { fixUrl(it) }

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

        val doc = app.get(url).document

        val title = doc.selectFirst("h1")?.text() ?: return null
        val poster = doc.selectFirst("img")?.attr("src")?.let { fixUrl(it) }
        val plot = doc.selectFirst("p")?.text()

        return if (url.contains("/dizi/")) {

            val episodes = doc.select("a[href*='/bolum/']").map {
                newEpisode(fixUrl(it.attr("href"))) {
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

    // ================= STREAM EXTRACTION =================

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val doc = app.get(data).document

        val iframe = doc.selectFirst("iframe")?.attr("src") ?: return false
        val iframeUrl = fixUrl(iframe)

        val body = app.get(
            iframeUrl,
            referer = mainUrl
        ).text

        // m3u8 yakala
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

        // Base64 decode
        Regex("""["']([A-Za-z0-9+/=]{120,})["']""")
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

        loadExtractor(
            url = iframeUrl,
            referer = mainUrl,
            subtitleCallback = subtitleCallback,
            callback = callback
        )

        return true
    }
}
