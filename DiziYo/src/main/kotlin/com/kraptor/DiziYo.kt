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
    override val supportedTypes = setOf(TvType.TvSeries) // Sadece dizi

    override var mainUrl = "https://www.diziyo.so"
    private val cfKiller = CloudflareKiller()

    // ================= MAIN PAGE - DOĞRU URL'LER =================

    override val mainPage = mainPageOf(
        "/diziler/page/" to "Yeni Diziler",           // Ana dizi listesi
        "/dil/turkce-dublaj/page/" to "Dublaj Diziler", // Türkçe dublaj
        "/dil/turkce-altyazi/page/" to "Altyazılı Diziler" // Türkçe altyazı
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        
        val url = "$mainUrl${request.data}$page/"
        Log.d("DIZIYO", "📄 URL: $url")

        return try {
            val doc = app.get(url, timeout = 30, interceptor = cfKiller).document

            // 🔥 DOĞRU SEÇİCİ: article.item.item-overlay
            val items = doc.select("article.item.item-overlay").mapNotNull { 
                it.toSearchResult() 
            }

            Log.d("DIZIYO", "🎬 ${request.name}: ${items.size} adet")

            // Boşsa logla
            if (items.isEmpty()) {
                Log.e("DIZIYO", "❌ Boş liste! HTML: ${doc.html().take(500)}")
            }

            newHomePageResponse(request.name, items)

        } catch (e: Exception) {
            Log.e("DIZIYO", "❌ Hata: ${e.message}")
            newHomePageResponse(request.name, emptyList())
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val linkElement = selectFirst("a") ?: return null
        val href = linkElement.attr("href")
        val link = fixUrl(href)

        // Başlık
        val title = selectFirst(".overlay-title")?.text()?.trim() 
            ?: linkElement.attr("aria-label").trim()
            ?: return null

        // 🔥 RESİM - data-wpfc-original-src önemli!
        val img = selectFirst("img")
        val poster = when {
            img?.hasAttr("data-wpfc-original-src") == true -> img.attr("data-wpfc-original-src")
            img?.hasAttr("data-src") == true -> img.attr("data-src")
            img?.hasAttr("src") == true -> img.attr("src")
            else -> null
        }?.let { fixUrl(it) }

        Log.d("DIZIYO", "✅ $title | Poster: ${poster != null}")

        return newTvSeriesSearchResponse(title, link, TvType.TvSeries) {
            this.posterUrl = poster
        }
    }

    // ================= SEARCH =================

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=${query.replace(" ", "+")}&post_type=dizi"
        
        val doc = app.get(url, timeout = 30, interceptor = cfKiller).document

        return doc.select("article.item.item-overlay").mapNotNull { 
            it.toSearchResult() 
        }
    }

    // ================= LOAD =================

    override suspend fun load(url: String): LoadResponse? {
        Log.d("DIZIYO", "📥 Detay: $url")
        
        val doc = app.get(url, timeout = 30, interceptor = cfKiller).document

        val title = doc.selectFirst("h1[data-name]")?.attr("data-name")
            ?: doc.selectFirst("h1")?.text()?.trim()
            ?: return null

        val poster = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: doc.selectFirst(".poster img, .thumb img")?.let { img ->
                img.attr("data-wpfc-original-src").ifEmpty { img.attr("src") }
            }?.let { fixUrl(it) }

        val desc = doc.selectFirst("meta[property=og:description]")?.attr("content")
            ?: doc.selectFirst(".description, .summary")?.text()

        // Bölümleri bul
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

            val postId = Regex("""postid-(\d+)""").find(doc.html())?.groupValues?.get(1)
                ?: Regex("""data-id=["']?(\d+)["']?""").find(doc.html())?.groupValues?.get(1)
                ?: return false

            val ajaxUrl = "$mainUrl/wp-admin/admin-ajax.php"
            val type = if (data.contains("/dizi/") || data.contains("/bolum/")) "tv" else "movie"

            val response = app.post(
                ajaxUrl,
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

            val iframe = Regex("""src=["']([^"']+)["']""").find(response)?.groupValues?.get(1)
                ?.replace("\\/", "/") ?: return false

            when {
                iframe.contains(".m3u8", ignoreCase = true) -> {
                    callback.invoke(
                        newExtractorLink(name, "Direct", iframe, ExtractorLinkType.M3U8) {
                            referer = mainUrl
                        }
                    )
                }
                
                iframe.contains("video/") -> {
                    val hash = Regex("""video/([a-zA-Z0-9]+)""").find(iframe)?.groupValues?.get(1)
                    
                    if (hash != null) {
                        val videoRes = app.post(
                            "$iframe?do=getVideo",
                            data = mapOf("hash" to hash, "r" to mainUrl),
                            headers = mapOf("Referer" to iframe),
                            timeout = 30,
                            interceptor = cfKiller
                        ).text

                        val json = JSONObject(videoRes)
                        val sources = json.optJSONArray("videoSources") ?: return false

                        for (i in 0 until sources.length()) {
                            val file = sources.getJSONObject(i).getString("file")
                            M3u8Helper.generateM3u8(name, file, iframe).forEach(callback)
                        }
                    }
                }
                
                else -> loadExtractor(iframe, data, subtitleCallback, callback)
            }

            true

        } catch (e: Exception) {
            Log.e("DIZIYO", "❌ Hata: ${e.message}")
            false
        }
    }
}
