package com.umaytrade

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class BelgeselX : MainAPI() {
    override var mainUrl = "https://www.belgeselizlesene.com"
    override var name = "Belgesel İzlesene"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Documentary)

    override val mainPage = mainPageOf(
        "/" to "Son Eklenenler",
        "/tur/bbc" to "BBC Belgeselleri",
        "/tur/national-geographic" to "National Geographic",
        "/tur/discovery-channel" to "Discovery Channel",
        "/tur/history-channel" to "History Channel",
        "/tur/turkce-belgeseller" to "Türkçe Belgeseller",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl${request.data}page/$page/").document
        val home = document.select("article.post").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2.entry-title a")?.text()?.trim() ?: return null
        val href = this.selectFirst("h2.entry-title a")?.attr("href")?.let { fixUrl(it) } ?: return null
        val posterUrl = this.selectFirst("img")?.attr("src")?.let { fixUrl(it) }
        val quality = this.selectFirst(".quality")?.text()
        return newMovieSearchResponse(title, href, TvType.Documentary) {
            this.posterUrl = posterUrl
            addQuality(quality)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("article.post").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        val poster = document.selectFirst(".post-thumbnail img")?.attr("src")?.let { fixUrl(it) }
        val description = document.selectFirst(".entry-content p")?.text()?.trim()
        val year = document.selectFirst(".published")?.text()?.split(" ")?.last()?.toIntOrNull()
        
        val tags = document.select(".cat-links a").map { it.text() }
        val actors = document.select(".starring a").map { it.text() }
        val trailer = document.selectFirst("a[href*=youtube]")?.attr("href")

        val episodes = mutableListOf<Episode>()

        document.select(".episode-item").forEachIndexed { index, element ->
            val epName = element.selectFirst(".episode-title")?.text() ?: "Bölüm ${index + 1}"
            val epHref = element.selectFirst("a")?.attr("href")?.let { fixUrl(it) } ?: return@forEachIndexed
            val epPoster = element.selectFirst("img")?.attr("src")?.let { fixUrl(it) }
            
            episodes.add(
                newEpisode(epHref) {
                    name = epName
                    season = 1
                    episode = index + 1
                    posterUrl = epPoster
                }
            )
        }

        return if (episodes.isEmpty()) {
            newMovieLoadResponse(title, url, TvType.Documentary, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                addActors(actors)
                addTrailer(trailer)
            }
        } else {
            newTvSeriesLoadResponse(title, url, TvType.Documentary, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                addActors(actors)
                addTrailer(trailer)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        document.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.isNotBlank()) {
                loadExtractor(src, data, subtitleCallback, callback)
            }
        }

        document.select(".video-source a").forEach { source ->
            val dataSrc = source.attr("data-src")
            val href = source.attr("href")
            val videoUrl = dataSrc.ifBlank { href }
            if (videoUrl.isNotBlank()) {
                loadExtractor(videoUrl, data, subtitleCallback, callback)
            }
        }

        document.select(".entry-content").first()?.let { content ->
            val html = content.html()
            
            Regex("https?://vk\\.com/video[\\w\\-_]+").findAll(html).forEach { match ->
                loadExtractor(match.value, data, subtitleCallback, callback)
            }
            
            Regex("https?://my\\.mail\\.ru/video/embed/[\\w\\-/]+").findAll(html).forEach { match ->
                loadExtractor(match.value, data, subtitleCallback, callback)
            }
            
            Regex("https?://ok\\.ru/video/[\\d]+").findAll(html).forEach { match ->
                loadExtractor(match.value, data, subtitleCallback, callback)
            }
            
            Regex("youtube\\.com/embed/([\\w\\-]+)").find(html)?.groupValues?.get(1)?.let { videoId ->
                callback.invoke(
                    newExtractorLink(
                        source = "YouTube",
                        name = "YouTube",
                        url = "https://www.youtube.com/watch?v=$videoId"
                    ) {
                        referer = "https://www.youtube.com/"
                        quality = Qualities.Unknown.value
                    }
                )
            }
        }

        return true
    }
}
