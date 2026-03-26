package com.kraptor

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import org.jsoup.nodes.Element
import org.json.JSONObject

class DiziYoUltimate : MainAPI() {

    override var name = "DiziYo"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Anime)

    override var mainUrl = "https://diziyo.so"
    private val cfKiller = CloudflareKiller()

    // ================= MAIN PAGE =================

    override val mainPage = mainPageOf(
        "/filmler/" to "Filmler",
        "/diziler/" to "Diziler",
        "/anime/" to "Anime"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        
        val url = "$mainUrl${request.data}$page/"
        Log.d("DIZIYO", "📄 URL: $url")

        val doc = app.get(url, timeout = 30, interceptor = cfKiller).document

        // 🔥 TÜM OLASI SEÇİCİLER (En yaygından nadir olana)
        val selectors = listOf(
            "div.item",           // En yaygın
            "article.item",       // WordPress
            ".movie-item",        // Film temaları
            ".video-item",        // Video temaları
            ".post-item",         // Blog temaları
            ".card",              // Bootstrap
            ".content-item",      // Dooplay
            ".poster",            // Basit temalar
            "article",            // Genel
            ".result-item"        // Arama sonuçları
        )

        var items = emptyList<SearchResponse>()
        
        // Çalışan seçiciyi bul
        for (selector in selectors) {
            val elements = doc.select(selector)
            Log.d("DIZIYO", "🔍 $selector: ${elements.size} adet")
            
            if (elements.isNotEmpty()) {
                items = elements.mapNotNull { it.toSearchResult() }
                if (items.isNotEmpty()) {
                    Log.d("DIZIYO", "✅ Çalışan seçici: $selector")
                    break
                }
            }
        }

        // Hiçbiri çalışmadıysa tüm HTML'i logla
        if (items.isEmpty()) {
            Log.e("DIZIYO", "❌ Hiçbir seçici çalışmadı!")
            Log.d("DIZIYO", "HTML: ${doc.html().take(500)}")
        }

        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        // Başlık (tüm olasılıklar)
        val title = selectFirst("h3 a, h2 a, h3, h2, .title a, .title, a[title], .name")?.let {
            it.text().ifEmpty { it.attr("title") }
        }?.trim() ?: return null

        // Link
        val href = selectFirst("a")?.attr("href") ?: return null
        val link = when {
            href.startsWith("http") -> href
            href.startsWith("/") -> "$mainUrl$href"
            else -> "$mainUrl/$href"
        }

        // Poster
        val poster = selectFirst("img")?.let { img ->
            img.attr("data-src").ifEmpty { 
                img.attr("data-original").ifEmpty {
                    img.attr("src")
                }
            }
        }?.let { fixUrl(it) }

        Log.d("DIZIYO", "🎬 $title")

        // Tür belirle
        val type = when {
            link.contains("/dizi/") -> TvType.TvSeries
            link.contains("/anime/") -> TvType.Anime
            else -> TvType.Movie
        }

        return if (type == TvType.TvSeries || type == TvType.Anime) {
            newTvSeriesSearchResponse(title, link, type) {
                this.posterUrl = poster
            }
        } else {
            newMovieSearchResponse(title, link, type) {
                this.posterUrl = poster
            }
        }
    }

    // ================= SEARCH =================

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=${query.replace(" ", "+")}"
        val doc = app.get(url, timeout = 30, interceptor = cfKiller).document
        
        return doc.select("div.item, article.item, .result-item, .video-item").mapNotNull { 
            it.toSearchResult() 
        }
    }

    // ================= LOAD =================

    override suspend fun load(url: String): LoadResponse? {
        Log.d("DIZIYO", "📥 Detay: $url")
        
        val doc = app.get(url, timeout = 30, interceptor = cfKiller).document

        val title = doc.selectFirst("h1[data-name], h1.title, h1")?.let {
            it.attr("data-name").ifEmpty { it.text() }
        }?.trim() ?: return null

        val poster = doc.selectFirst(".poster img, .thumb img, img")?.attr("src")?.let { fixUrl(it) }
        val desc = doc.selectFirst(".description, .summary, .plot, .content")?.text()

        // Bölümler
        val episodes = doc.select("#episodes a, .episode-list a, .episodes a").mapIndexed { index, element ->
            val epName = element.text().trim().ifEmpty { "Bölüm ${index + 1}" }
            val epUrl = fixUrl(element.attr("href"))
            newEpisode(epUrl) {
                name = epName
                episode = index + 1
            }
        }

        return if (episodes.isNotEmpty()) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = desc
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = desc
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
        Log.d("DIZIYO", "▶️ Link: $data")

        return try {
            val doc = app.get(data, timeout = 30, interceptor = cfKiller).document

            // Post ID bul
            val postId = Regex("""postid-(\d+)""").find(doc.html())?.groupValues?.get(1)
                ?: Regex("""data-id=["']?(\d+)["']?""").find(doc.html())?.groupValues?.get(1)
                ?: return false

            val type = if (data.contains("/dizi/") || data.contains("/anime/")) "tv" else "movie"

            val response = app.post(
                "$mainUrl/wp-admin/admin-ajax.php",
                data = mapOf(
                    "action" to "doo_player_ajax",
                    "post" to postId,
                    "nume" to "1",
                    "type" to type
                ),
                headers = mapOf(
                    "X-Requested-With" to "XMLHttpRequest",
                    "Referer" to data
                ),
                timeout = 30,
                interceptor = cfKiller
            ).text

            // iframe bul
            val iframe = Regex("""src=["']([^"']+)["']""").find(response)?.groupValues?.get(1)
                ?.replace("\\/", "/") ?: return false

            Log.d("DIZIYO", "🌐 iframe: $iframe")

            // m3u8 mi?
            if (iframe.contains(".m3u8")) {
                callback.invoke(
                    newExtractorLink(name, "Direct", iframe, ExtractorLinkType.M3U8) {
                        referer = mainUrl
                    }
                )
                return true
            }

            // Hash sistemi
            val hash = Regex("""video/([a-zA-Z0-9]+)""").find(iframe)?.groupValues?.get(1)
            if (hash != null) {
                val json = app.post(
                    "$iframe?do=getVideo",
                    data = mapOf("hash" to hash, "r" to mainUrl),
                    headers = mapOf("Referer" to iframe),
                    timeout = 30,
                    interceptor = cfKiller
                ).text

                val sources = JSONObject(json).getJSONArray("videoSources")
                for (i in 0 until sources.length()) {
                    val file = sources.getJSONObject(i).getString("file")
                    M3u8Helper.generateM3u8(name, file, iframe).forEach(callback)
                }
                return true
            }

            loadExtractor(iframe, data, subtitleCallback, callback)

        } catch (e: Exception) {
            Log.e("DIZIYO", "❌ Hata: ${e.message}")
            false
        }
    }
}
