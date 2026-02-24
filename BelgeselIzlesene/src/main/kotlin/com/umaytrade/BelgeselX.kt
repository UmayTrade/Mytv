package com.cloudstream3

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import org.jsoup.nodes.Element

class BelgeselIzlesene : MainAPI() {
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
        val href = fixUrl(this.selectFirst("h2.entry-title a")?.attr("href") ?: return null)
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
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
        val poster = fixUrlNull(document.selectFirst(".post-thumbnail img")?.attr("src"))
        val description = document.selectFirst(".entry-content p")?.text()?.trim()
        val year = document.selectFirst(".published")?.text()?.split(" ")?.last()?.toIntOrNull()
        
        // Kategorileri al
        val tags = document.select(".cat-links a").map { it.text() }
        
        // Oyuncular/Yapımcılar
        val actors = document.select(".starring a").map { it.text() }

        // Trailer linki
        val trailer = document.selectFirst("a[href*=youtube]")?.attr("href")

        val episodes = mutableListOf<Episode>()

        // Eğer bölümlü bir yapı varsa (bazı belgeseller seri halinde olabilir)
        document.select(".episode-item").forEachIndexed { index, element ->
            val epName = element.selectFirst(".episode-title")?.text() ?: "Bölüm ${index + 1}"
            val epHref = fixUrl(element.selectFirst("a")?.attr("href") ?: return@forEachIndexed)
            val epPoster = fixUrlNull(element.selectFirst("img")?.attr("src"))
            
            episodes.add(
                Episode(
                    data = epHref,
                    name = epName,
                    season = 1,
                    episode = index + 1,
                    posterUrl = epPoster
                )
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
        
        // Video kaynaklarını bul
        document.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.isNotBlank()) {
                // OEmbed veya direkt video linkleri
                loadExtractor(src, data, subtitleCallback, callback)
            }
        }

        // Alternatif kaynaklar (VK, Mail.ru, vb.)
        document.select(".video-source a").forEach { source ->
            val videoUrl = source.attr("data-src") ?: source.attr("href")
            if (videoUrl.isNotBlank()) {
                loadExtractor(videoUrl, data, subtitleCallback, callback)
            }
        }

        // Embed kodları içindeki linkler
        document.select(".entry-content").first()?.let { content ->
            val html = content.html()
            
            // VK.com linkleri
            Regex("https?://vk\\.com/video[\\w\\-_]+").findAll(html).forEach { match ->
                val vkUrl = match.value
                loadExtractor(vkUrl, data, subtitleCallback, callback)
            }
            
            // Mail.ru linkleri
            Regex("https?://my\\.mail\\.ru/video/embed/[\\w\\-/]+").findAll(html).forEach { match ->
                val mailruUrl = match.value
                loadExtractor(mailruUrl, data, subtitleCallback, callback)
            }
            
            // Ok.ru linkleri
            Regex("https?://ok\\.ru/video/[\\d]+").findAll(html).forEach { match ->
                val okUrl = match.value
                loadExtractor(okUrl, data, subtitleCallback, callback)
            }
            
            // YouTube embedleri
            Regex("youtube\\.com/embed/([\\w\\-]+)").find(html)?.groupValues?.get(1)?.let { videoId ->
                callback.invoke(
                    ExtractorLink(
                        source = "YouTube",
                        name = "YouTube",
                        url = "https://www.youtube.com/watch?v=$videoId",
                        referer = "https://www.youtube.com/",
                        quality = Qualities.Unknown.value,
                        isM3u8 = false
                    )
                )
            }
        }

        return true
    }
}
