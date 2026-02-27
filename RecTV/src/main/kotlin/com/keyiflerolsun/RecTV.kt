package com.keyiflerolsun

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class RecTV : MainAPI() {

    override var mainUrl = "https://rectv.example" // değiştir
    override var name = "RecTV"
    override var lang = "tr"
    override val hasMainPage = true

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    // ================= MAIN PAGE =================

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val document = app.get(mainUrl).document

        val items = document.select("div.item")
            .mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            listOf(
                HomePageList(
                    name = "Son Eklenenler",
                    list = items
                )
            ),
            hasNext = false
        )
    }

    // ================= SEARCH =================

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("div.item")
            .mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = selectFirst("h2")?.text() ?: return null
        val href = fixUrl(selectFirst("a")?.attr("href") ?: return null)
        val poster = fixUrlNull(selectFirst("img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = poster
        }
    }

    // ================= LOAD =================

    override suspend fun load(url: String): LoadResponse? {

        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text() ?: return null
        val poster = fixUrlNull(document.selectFirst("img")?.attr("src"))
        val plot = document.selectFirst(".description")?.text()

        // Eğer sezon sistemi varsa
        val seasons = document.select(".season")

        if (seasons.isNotEmpty()) {

            val episodes = mutableListOf<Episode>()

            val numberRegex = Regex("""\d+""")

            seasons.forEach { seasonElement ->

                val seasonTitle = seasonElement.selectFirst(".season-title")?.text() ?: ""
                val seasonNum = numberRegex.find(seasonTitle)?.value?.toIntOrNull()

                seasonElement.select(".episode").forEach { ep ->

                    val epTitle = ep.text()
                    val epUrl = fixUrl(ep.selectFirst("a")?.attr("href") ?: return@forEach)

                    val epNum = numberRegex.find(epTitle)?.value?.toIntOrNull()

                    episodes.add(
                        newEpisode(epUrl) {
                            this.name = epTitle
                            this.season = seasonNum
                            this.episode = epNum
                        }
                    )
                }
            }

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = plot
            }
        }

        // Film ise
        val links = document.select("iframe")
            .mapNotNull { it.attr("src") }

        return newMovieLoadResponse(title, url, TvType.Movie, links) {
            this.posterUrl = poster
            this.plot = plot
        }
    }

    // ================= LINKS =================

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val fixedUrl = fixUrl(data)

        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = fixedUrl,
                type = if (fixedUrl.contains(".m3u8"))
                    ExtractorLinkType.M3U8
                else
                    ExtractorLinkType.VIDEO
            ) {
                this.referer = mainUrl
                this.quality = Qualities.Unknown.value
            }
        )

        return true
    }
}
