
package com.umaytrade

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class BelgeselX : MainAPI() {

    override var mainUrl = "https://www.belgeselizlesene.com"
    override var name = "BelgeselIZlesene"
    override val hasMainPage = true
    override var lang = "tr"
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override val mainPage = mainPageOf(
        "$mainUrl/uzay-evren-belgeselleri" to "Uzay & Evren Belgeselleri",
        "$mainUrl/otomobil-belgeselleri" to "Otomobil Belgeselleri",
        "$mainUrl/vahsi-yasam-belgeselleri" to "Vahşi Yaşam Belgeselleri"
    )

    // ================= UTIL =================

    private fun fixUrlNull(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> mainUrl + url
            else -> url
        }
    }

    private fun Element.getPoster(): String? {
        return fixUrlNull(
            selectFirst("img")?.attr("data-src")
                ?: selectFirst("img")?.attr("data-lazy-src")
                ?: selectFirst("img")?.attr("src")
        )
    }

    // ================= MAIN PAGE =================

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val url = if (page == 1) request.data else "${request.data}/page/$page"
        val doc = app.get(url).document

        val items = doc.select(
            "article, .post, .post-item, .video-item, .col-lg-3, .col-md-4"
        ).mapNotNull { element ->

            val link = fixUrlNull(element.selectFirst("a")?.attr("href"))
                ?: return@mapNotNull null

            val title = element.selectFirst("h2, h3, .title, img")
                ?.let { if (it.tagName() == "img") it.attr("alt") else it.text() }
                ?.trim()
                ?: return@mapNotNull null

            newTvSeriesSearchResponse(
                title,
                link,
                TvType.TvSeries
            ) {
                posterUrl = element.getPoster()
            }
        }

        return newHomePageResponse(request.name, items)
    }

    // ================= SEARCH =================

    override suspend fun search(query: String): List<SearchResponse> {

        val url = "$mainUrl/?s=${query.replace(" ", "+")}"
        val doc = app.get(url).document

        return doc.select("article, .post, .post-item")
            .mapNotNull { element ->

                val link = fixUrlNull(element.selectFirst("a")?.attr("href"))
                    ?: return@mapNotNull null

                val title = element.selectFirst("h2, h3, img")
                    ?.let { if (it.tagName() == "img") it.attr("alt") else it.text() }
                    ?.trim()
                    ?: return@mapNotNull null

                newTvSeriesSearchResponse(
                    title,
                    link,
                    TvType.TvSeries
                ) {
                    posterUrl = element.getPoster()
                }
            }
    }

    // ================= LOAD =================

    override suspend fun load(url: String): LoadResponse {

        val doc = app.get(url).document

        val title = doc.selectFirst("h1")?.text()?.trim() ?: "BelgeselX"
        val description = doc.selectFirst("p")?.text()
        val poster = fixUrlNull(doc.selectFirst("img")?.attr("src"))

        val episodeLinks = doc.select("a[href*='bolum'], a[href*='episode']")

        val episodes = episodeLinks.mapIndexed { index, ep ->
            newEpisode(fixUrlNull(ep.attr("href")) ?: "") {
                name = ep.text().trim()
                episode = index + 1
            }
        }

        return if (episodes.isNotEmpty()) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
            }
        }
    }

    // ================= LOAD LINKS =================

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val visited = mutableSetOf<String>()
        var found = false

        suspend fun extract(url: String) {

            if (visited.contains(url)) return
            visited.add(url)

            val res = app.get(url, referer = mainUrl)
            val html = res.text
            val doc = res.document

            // m3u8 yakala
            Regex("""https?://[^"' ]+\.m3u8[^"' ]*""")
                .findAll(html)
                .forEach {
                    found = true
                    callback.invoke(
                        newExtractorLink(
                            source = name,
                            name = "$name M3U8",
                            url = it.value,
                            type = ExtractorLinkType.M3U8
                        ) {
                            referer = url
                            quality = Qualities.Unknown.value
                        }
                    )
                }

            // mp4 yakala
            Regex("""https?://[^"' ]+\.mp4[^"' ]*""")
                .findAll(html)
                .forEach {
                    found = true
                    callback.invoke(
                        newExtractorLink(
                            source = name,
                            name = "$name MP4",
                            url = it.value,
                            type = ExtractorLinkType.VIDEO
                        ) {
                            referer = url
                            quality = Qualities.Unknown.value
                        }
                    )
                }

            // iframe recursive
            doc.select("iframe").forEach {
                val src = fixUrlNull(it.attr("src"))
                if (!src.isNullOrBlank()) {
                    extract(src)
                }
            }
        }

        extract(data)

        return found
    }
}
