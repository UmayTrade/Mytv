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

    // 🔥 WWW'LU HALİ - HTML'den doğrulandı
    override var mainUrl = "https://www.diziyo.so"
    
    private val cfKiller = CloudflareKiller()

    // ================= MAIN PAGE =================

    override val mainPage = mainPageOf(
        "/diziler/page/" to "Diziler",
        "/bolumler/page/" to "Son Bölümler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        
        val url = "$mainUrl${request.data}$page/"
        Log.d("DIZIYO", "📄 URL: $url")

        val doc = app.get(url, timeout = 30, interceptor = cfKiller).document

        // 🔥 DOĞRU SEÇİCİLER - HTML analizinden
        val items = doc.select("article.item.item-overlay").mapNotNull { 
            it.toSearchResult() 
        }

        Log.d("DIZIYO", "🎬 Bulunan: ${items.size} adet")

        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        // Link
        val linkElement = selectFirst("a") ?: return null
        val href = linkElement.attr("href")
        val link = fixUrl(href)

        // Başlık - overlay-title içinde
        val title = selectFirst(".overlay-title")?.text()?.trim() ?: return null

        // 🔥 RESİM - data-wpfc-original-src attribute'unda!
        val img = selectFirst("img")
        val poster = when {
            img?.hasAttr("data-wpfc-original-src") == true -> img.attr("data-wpfc-original-src")
            img?.hasAttr("data-src") == true -> img.attr("data-src")
            img?.hasAttr("src") == true -> img.attr("src")
            else -> null
        }?.let { fixUrl(it) }

        // IMDb puanı (varsa)
        val imdb = selectFirst(".imdb-pill__val")?.text()

        // Tür belirle - URL'den
        val type = when {
            link.contains("/dizi/") -> TvType.TvSeries
            link.contains("/bolum/") -> TvType.TvSeries  // Bölüm sayfası
            else -> TvType.Movie
        }

        Log.d("DIZIYO", "✅ $title | $link | Poster: ${poster != null}")

        return if (type == TvType.TvSeries) {
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
        // 🔥 post_type=dizi eklendi - HTML'den doğrulandı
        val url = "$mainUrl/?s=${query.replace(" ", "+")}&post_type=dizi"
        
        val doc = app.get(url, timeout = 30, interceptor = cfKiller).document

        return doc.select("article.item.item-overlay, article.item").mapNotNull { 
            it.toSearchResult() 
        }
    }

    // ================= LOAD (Detay Sayfası) =================

    override suspend fun load(url: String): LoadResponse? {
        Log.d("DIZIYO", "📥 Detay: $url")
        
        val doc = app.get(url, timeout = 30, interceptor = cfKiller).document

        // Başlık - h1 data-name veya normal h1
        val title = doc.selectFirst("h1[data-name]")?.attr("data-name")
            ?: doc.selectFirst("h1")?.text()?.trim()
            ?: return null

        // Poster - meta veya sayfadaki resim
        val poster = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: doc.selectFirst(".poster img, .thumb img")?.let { img ->
                img.attr("data-wpfc-original-src").ifEmpty {
                    img.attr("src")
                }
            }?.let { fixUrl(it) }

        // Açıklama
        val desc = doc.selectFirst("meta[property=og:description]")?.attr("content")
            ?: doc.selectFirst(".description, .summary")?.text()

        // Bölümleri bul - farklı seçiciler dene
        val episodeSelectors = listOf(
            "#episodes a",
            ".episode-list a", 
            ".episodes a",
            ".season-list a",
            ".bolum-list a"
        )
        
        var episodes = emptyList<Episode>()
        
        for (selector in episodeSelectors) {
            val elements = doc.select(selector)
            if (elements.isNotEmpty()) {
                episodes = elements.mapIndexed { index, element ->
                    val epName = element.text().trim().ifEmpty { "Bölüm ${index + 1}" }
                    val epUrl = fixUrl(element.attr("href"))
                    newEpisode(epUrl) {
                        name = epName
                        episode = index + 1
                    }
                }
                Log.d("DIZIYO", "📺 Bölüm seçici: $selector, ${episodes.size} bölüm")
                break
            }
        }

        return if (episodes.isNotEmpty()) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = desc
            }
        } else {
            // Film veya tek bölümlük içerik
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

            // Post ID bul - farklı patternler
            val postId = Regex("""postid-(\d+)""").find(doc.html())?.groupValues?.get(1)
                ?: Regex("""data-id=["']?(\d+)["']?""").find(doc.html())?.groupValues?.get(1)
                ?: Regex("""post_id["']?\s*:\s*["']?(\d+)""").find(doc.html())?.groupValues?.get(1)
                ?: return false

            Log.d("DIZIYO", "🆔 Post ID: $postId")

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

            // iframe src bul
            val iframe = Regex("""src=["']([^"']+)["']""").find(response)?.groupValues?.get(1)
                ?.replace("\\/", "/") ?: return false

            Log.d("DIZIYO", "🌐 iframe: $iframe")

            when {
                iframe.contains(".m3u8", ignoreCase = true) -> {
                    callback.invoke(
                        newExtractorLink(name, "Direct", iframe, ExtractorLinkType.M3U8) {
                            referer = mainUrl
                        }
                    )
                }
                
                iframe.contains("video/") || iframe.contains("embed/") -> {
                    val hash = Regex("""video/([a-zA-Z0-9]+)""").find(iframe)?.groupValues?.get(1)
                        ?: Regex("""embed/([a-zA-Z0-9]+)""").find(iframe)?.groupValues?.get(1)
                    
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
                            val source = sources.getJSONObject(i)
                            val file = source.getString("file")
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
