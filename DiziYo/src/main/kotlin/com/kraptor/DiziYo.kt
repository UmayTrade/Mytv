package com.kraptor

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import org.jsoup.nodes.Element
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy

class DiziYoUltimate : MainAPI() {

    override var name = "DiziYo"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.TvSeries)

    override var mainUrl = "https://www.diziyo.so"
    
    // 🔥 CLOUDFLARE KILLER - Özel ayarlarla
    private val cfKiller = CloudflareKiller()
    
    // 🔥 GERÇEK TARAYICI HEADER'LARI
    private val chromeHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
        "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
        "Accept-Encoding" to "gzip, deflate, br",
        "Cache-Control" to "max-age=0",
        "Sec-Ch-Ua" to "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"",
        "Sec-Ch-Ua-Mobile" to "?0",
        "Sec-Ch-Ua-Platform" to "\"Windows\"",
        "Sec-Fetch-Dest" to "document",
        "Sec-Fetch-Mode" to "navigate",
        "Sec-Fetch-Site" to "none",
        "Sec-Fetch-User" to "?1",
        "Upgrade-Insecure-Requests" to "1",
        "Connection" to "keep-alive",
        "DNT" to "1"
    )

    // 🔥 AJAX HEADER'LARI
    private val ajaxHeaders = chromeHeaders + mapOf(
        "X-Requested-With" to "XMLHttpRequest",
        "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
        "Origin" to mainUrl,
        "Referer" to mainUrl
    )

    // ================= MAIN PAGE =================

    override val mainPage = mainPageOf(
        "/diziler/sayfa/" to "Yeni Diziler",
        "/dil/turkce-dublaj/sayfa/" to "Dublaj Diziler",
        "/dil/turkce-altyazi/sayfa/" to "Altyazılı Diziler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        
        val url = "$mainUrl${request.data}$page/"
        Log.d("DIZIYO", "📄 URL: $url")

        return try {
            // 🔥 ÖNCE ANA SAYFAYA GİR (Cookie almak için)
            val homeRes = app.get(mainUrl, headers = chromeHeaders, timeout = 30)
            val cookies = homeRes.cookies
            
            Log.d("DIZIYO", "🍪 Cookies: ${cookies.size}")

            // 🔥 SONRA HEDEF URL'YE GİT (Cookie'lerle)
            val doc = app.get(
                url = url,
                headers = chromeHeaders + mapOf("Referer" to mainUrl),
                cookies = cookies,
                timeout = 30,
                interceptor = cfKiller
            ).document

            val items = doc.select("article.item.item-overlay").mapNotNull { 
                it.toSearchResult() 
            }

            Log.d("DIZIYO", "🎬 ${request.name}: ${items.size} adet")

            if (items.isEmpty()) {
                Log.e("DIZIYO", "❌ Boş! HTML: ${doc.html().take(1000)}")
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

        val title = selectFirst(".overlay-title")?.text()?.trim() 
            ?: linkElement.attr("aria-label").trim()
            ?: return null

        val img = selectFirst("img")
        val poster = when {
            img?.hasAttr("data-wpfc-original-src") == true -> img.attr("data-wpfc-original-src")
            img?.hasAttr("data-src") == true -> img.attr("data-src")
            img?.hasAttr("src") == true -> img.attr("src")
            else -> null
        }?.let { fixUrl(it) }

        Log.d("DIZIYO", "✅ $title")

        return newTvSeriesSearchResponse(title, link, TvType.TvSeries) {
            this.posterUrl = poster
        }
    }

    // ================= SEARCH =================

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=${query.replace(" ", "+")}&post_type=dizi"
        
        return try {
            val homeRes = app.get(mainUrl, headers = chromeHeaders, timeout = 30)
            
            val doc = app.get(
                url = url,
                headers = chromeHeaders + mapOf("Referer" to mainUrl),
                cookies = homeRes.cookies,
                timeout = 30,
                interceptor = cfKiller
            ).document

            doc.select("article.item.item-overlay").mapNotNull { 
                it.toSearchResult() 
            }
        } catch (e: Exception) {
            Log.e("DIZIYO", "❌ Arama hatası: ${e.message}")
            emptyList()
        }
    }

    // ================= LOAD =================

    override suspend fun load(url: String): LoadResponse? {
        Log.d("DIZIYO", "📥 Detay: $url")
        
        return try {
            val homeRes = app.get(mainUrl, headers = chromeHeaders, timeout = 30)
            
            val doc = app.get(
                url = url,
                headers = chromeHeaders + mapOf("Referer" to mainUrl),
                cookies = homeRes.cookies,
                timeout = 30,
                interceptor = cfKiller
            ).document

            val title = doc.selectFirst("h1[data-name]")?.attr("data-name")
                ?: doc.selectFirst("h1")?.text()?.trim()
                ?: return null

            val poster = doc.selectFirst("meta[property=og:image]")?.attr("content")
                ?: doc.selectFirst(".poster img, .thumb img")?.let { img ->
                    img.attr("data-wpfc-original-src").ifEmpty { img.attr("src") }
                }?.let { fixUrl(it) }

            val desc = doc.selectFirst("meta[property=og:description]")?.attr("content")
                ?: doc.selectFirst(".description, .summary")?.text()

            val episodes = doc.select("#episodes a, .episode-list a, .episodes a").mapIndexed { index, element ->
                val epName = element.text().trim().ifEmpty { "Bölüm ${index + 1}" }
                val epUrl = fixUrl(element.attr("href"))
                newEpisode(epUrl) {
                    name = epName
                    episode = index + 1
                }
            }

            if (episodes.isNotEmpty()) {
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
        } catch (e: Exception) {
            Log.e("DIZIYO", "❌ Load hatası: ${e.message}")
            null
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
            val homeRes = app.get(mainUrl, headers = chromeHeaders, timeout = 30)
            
            val doc = app.get(
                url = data,
                headers = chromeHeaders + mapOf("Referer" to mainUrl),
                cookies = homeRes.cookies,
                timeout = 30,
                interceptor = cfKiller
            ).document

            val postId = Regex("""postid-(\d+)""").find(doc.html())?.groupValues?.get(1)
                ?: Regex("""data-id=["']?(\d+)["']?""").find(doc.html())?.groupValues?.get(1)
                ?: return false

            Log.d("DIZIYO", "🆔 Post ID: $postId")

            val ajaxUrl = "$mainUrl/wp-admin/admin-ajax.php"
            val type = if (data.contains("/dizi/") || data.contains("/bolum/")) "tv" else "movie"

            val response = app.post(
                url = ajaxUrl,
                data = mapOf(
                    "action" to "doo_player_ajax",
                    "post" to postId,
                    "nume" to "1",
                    "type" to type
                ),
                headers = ajaxHeaders + mapOf("Referer" to data),
                cookies = homeRes.cookies,
                timeout = 30,
                interceptor = cfKiller
            ).text

            Log.d("DIZIYO", "📡 AJAX: ${response.take(200)}")

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
                
                iframe.contains("video/") -> {
                    val hash = Regex("""video/([a-zA-Z0-9]+)""").find(iframe)?.groupValues?.get(1)
                    
                    if (hash != null) {
                        val videoRes = app.post(
                            "$iframe?do=getVideo",
                            data = mapOf("hash" to hash, "r" to mainUrl),
                            headers = ajaxHeaders + mapOf("Referer" to iframe),
                            cookies = homeRes.cookies,
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
            Log.e("DIZIYO", "❌ Link hatası: ${e.message}")
            false
        }
    }
}
