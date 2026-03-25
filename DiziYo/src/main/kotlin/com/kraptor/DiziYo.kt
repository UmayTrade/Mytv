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

    // 🔥 ÇALIŞAN DOMAIN
    override var mainUrl = "https://diziyo.so"

    // 🔥 USER-AGENT (Tarayıcı gibi görün)
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.0"

    // 🔥 CLOUDFLARE KILLER (Bir kez oluştur, her yerde kullan)
    private val cfKiller = CloudflareKiller()

    // 🔥 HEADER'LAR
    private val headers = mapOf(
        "User-Agent" to userAgent,
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
        "Accept-Encoding" to "gzip, deflate, br",
        "Connection" to "keep-alive",
        "Upgrade-Insecure-Requests" to "1"
    )

    // ================= MAIN PAGE =================

    override val mainPage = mainPageOf(
        "/filmler/page/" to "Filmler",
        "/diziler/page/" to "Diziler",
        "/anime/page/" to "Anime"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        
        val url = "$mainUrl${request.data}$page/"
        Log.d("DIZIYO", "📄 Yükleniyor: $url")

        // 🔥 TIMEOUT 30 SANİYE + HEADER'LAR
        val doc = app.get(
            url = url,
            headers = headers,
            timeout = 30,  // 30 saniye bekle
            interceptor = cfKiller
        ).document

        // 🔥 DİZİYO.SO SEÇİCİLERİ (item sınıfı)
        val items = doc.select("div.item, article.item, .movie-item, .video-item")

        Log.d("DIZIYO", "🎬 Bulunan: ${items.size} adet")

        val list = items.mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, list)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        // Başlık
        val titleElement = selectFirst("h3, h2, .title, a[title]")
        val title = titleElement?.text()?.trim() 
            ?: titleElement?.attr("title")?.trim() 
            ?: return null

        // Link
        val href = selectFirst("a")?.attr("href") ?: return null
        val link = fixUrl(href)

        // Poster
        val poster = fixUrlNull(
            selectFirst("img")?.attr("data-src")
            ?: selectFirst("img")?.attr("data-original")
            ?: selectFirst("img")?.attr("src")
        )

        // Tür belirle
        val type = when {
            link.contains("/dizi/") -> TvType.TvSeries
            link.contains("/anime/") -> TvType.Anime
            else -> TvType.Movie
        }

        Log.d("DIZIYO", "✅ $title")

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
        
        val doc = app.get(
            url = url,
            headers = headers,
            timeout = 30,
            interceptor = cfKiller
        ).document

        return doc.select("div.item, article.item, .result-item").mapNotNull { 
            it.toSearchResult() 
        }
    }

    // ================= LOAD (Detay Sayfası) =================

    override suspend fun load(url: String): LoadResponse? {
        Log.d("DIZIYO", "📥 Detay: $url")

        val doc = app.get(
            url = url,
            headers = headers,
            timeout = 30,
            interceptor = cfKiller
        ).document

        // Başlık
        val title = doc.selectFirst("h1[data-name], h1.title, h1")?.attr("data-name")
            ?: doc.selectFirst("h1")?.text()?.trim()
            ?: return null

        // Poster
        val poster = fixUrlNull(
            doc.selectFirst(".poster img")?.attr("src")
            ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
        )

        // Açıklama
        val desc = doc.selectFirst(".description, .summary, .plot")?.text()
            ?: doc.selectFirst("meta[property=og:description]")?.attr("content")

        // Bölümleri kontrol et
        val episodes = doc.select("div#episodes a, .episode-list a, .episodes a").mapIndexed { index, element ->
            val epName = element.text().trim().ifEmpty { "Bölüm ${index + 1}" }
            val epUrl = fixUrl(element.attr("href"))
            newEpisode(epUrl) {
                name = epName
                episode = index + 1
            }
        }

        return if (episodes.isNotEmpty()) {
            // Dizi/Anime
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = desc
            }
        } else {
            // Film
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = desc
            }
        }
    }

    // ================= LOAD LINKS (Video Oynatma) =================

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("DIZIYO", "▶️ Link: $data")

        return try {
            val doc = app.get(
                url = data,
                headers = headers,
                timeout = 30,
                interceptor = cfKiller
            ).document

            // Post ID bul
            val postId = Regex("""postid-(\d+)""").find(doc.html())?.groupValues?.get(1)
                ?: Regex("""data-id=["']?(\d+)["']?""").find(doc.html())?.groupValues?.get(1)
                ?: return false

            Log.d("DIZIYO", "🆔 Post ID: $postId")

            // AJAX isteği
            val ajaxUrl = "$mainUrl/wp-admin/admin-ajax.php"
            val type = if (data.contains("/dizi/") || data.contains("/anime/")) "tv" else "movie"

            val response = app.post(
                url = ajaxUrl,
                data = mapOf(
                    "action" to "doo_player_ajax",
                    "post" to postId,
                    "nume" to "1",
                    "type" to type
                ),
                headers = headers + mapOf(
                    "X-Requested-With" to "XMLHttpRequest",
                    "Referer" to data,
                    "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                    "Origin" to mainUrl
                ),
                timeout = 30,
                interceptor = cfKiller
            ).text

            Log.d("DIZIYO", "📡 Cevap: ${response.take(100)}")

            // iframe src bul
            val iframeUrl = Regex("""src=["']([^"']+)["']""").find(response)?.groupValues?.get(1)
                ?.replace("\\/", "/")
                ?: return false

            Log.d("DIZIYO", "🌐 iframe: $iframeUrl")

            // Direkt m3u8 mi?
            if (iframeUrl.contains(".m3u8")) {
                callback.invoke(
                    newExtractorLink(name, "Direct", iframeUrl, ExtractorLinkType.M3U8) {
                        this.referer = mainUrl
                    }
                )
                return true
            }

            // Hash sistemini dene
            val hash = Regex("""video/([a-zA-Z0-9]+)""").find(iframeUrl)?.groupValues?.get(1)
            if (hash != null) {
                val videoResponse = app.post(
                    url = "$iframeUrl?do=getVideo",
                    data = mapOf("hash" to hash, "r" to mainUrl),
                    headers = mapOf("Referer" to iframeUrl),
                    timeout = 30,
                    interceptor = cfKiller
                ).text

                val json = JSONObject(videoResponse)
                val sources = json.optJSONArray("videoSources") ?: return false

                for (i in 0 until sources.length()) {
                    val source = sources.getJSONObject(i)
                    val file = source.getString("file")
                    
                    M3u8Helper.generateM3u8(name, file, iframeUrl).forEach(callback)
                }
                return true
            }

            // Fallback: Extractor dene
            loadExtractor(iframeUrl, data, subtitleCallback, callback)

        } catch (e: Exception) {
            Log.e("DIZIYO", "❌ Hata: ${e.message}")
            false
        }
    }
}
