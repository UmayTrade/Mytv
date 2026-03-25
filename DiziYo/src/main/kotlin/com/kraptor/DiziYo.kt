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

    // 🔥 GÜNCELLENMİŞ DOMAIN HAVUZU
    private val domains = listOf(
        "https://www.diziyo.so",
        "https://www.diziyo.cx", 
        "https://www.diziyo.sx",
        "https://www.diziyo.xyz",
        "https://diziyo.nl",
        "https://diziyo.cx"
    )

    override var mainUrl = domains.first()

    // ================= DOMAIN AUTO SWITCH =================

    private suspend fun getWorkingDomain(): String {
        for (domain in domains) {
            try {
                val res = app.get(domain, timeout = 15, interceptor = CloudflareKiller())
                if (res.code == 200) {
                    Log.d("DIZIYO", "✅ WORKING DOMAIN: $domain")
                    mainUrl = domain
                    return domain
                }
            } catch (e: Exception) {
                Log.d("DIZIYO", "❌ FAILED: $domain - ${e.message}")
            }
        }
        return domains.first()
    }

    // ================= MAIN PAGE =================

    override val mainPage = mainPageOf(
        "filmler/page/" to "Filmler",
        "diziler/page/" to "Diziler", 
        "anime/page/" to "Anime"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        
        val domain = getWorkingDomain()
        
        // 🔥 DÜZELTİLMİŞ URL YAPISI
        val url = "$domain/${request.data}$page/"
        
        Log.d("DIZIYO", "📄 Loading: $url")

        val doc = app.get(url, interceptor = CloudflareKiller()).document

        // 🔥 GÜNCELLENMİŞ SEÇİCİLER - DiziYo yapısına göre
        val list = doc.select(".item, .movie-item, .post-item, article.item, .card, .content-item, .poster, .film-item").mapNotNull { 
            it.toSearch() 
        }

        // Eğer boşsa alternatif seçiciler dene
        val finalList = if (list.isEmpty()) {
            doc.select("article, .movie, .dizi, .anime, .video-item, .list-item").mapNotNull { 
                it.toSearch() 
            }
        } else list

        Log.d("DIZIYO", "🎬 Found ${finalList.size} items")

        return newHomePageResponse(request.name, finalList)
    }

    private fun Element.toSearch(): SearchResponse? {
        // 🔥 GÜNCELLENMİŞ BAŞLIK SEÇİCİLERİ
        val title = selectFirst("h2, h3, .title, .name, .item-title, a[title], .film-title")?.let {
            it.text().ifEmpty { it.attr("title") }
        }?.trim() ?: return null

        // 🔥 GÜNCELLENMİŞ LİNK SEÇİCİLERİ
        val link = fixUrlNull(
            selectFirst("a")?.attr("href")
            ?: selectFirst("a[data-url]")?.attr("data-url")
        ) ?: return null

        // 🔥 GÜNCELLENMİŞ POSTER SEÇİCİLERİ
        val poster = fixUrlNull(
            selectFirst("img")?.attr("data-src")
            ?: selectFirst("img")?.attr("data-original")
            ?: selectFirst("img")?.attr("src")
            ?: selectFirst("img")?.attr("data-lazy-src")
            ?: selectFirst(".poster img")?.attr("src")
            ?: selectFirst(".thumb img")?.attr("src")
        )

        // Tür belirleme
        val type = when {
            link.contains("/dizi/") -> TvType.TvSeries
            link.contains("/anime/") -> TvType.Anime
            else -> TvType.Movie
        }

        Log.d("DIZIYO", "📌 Parsed: $title -> $link")

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
        val domain = getWorkingDomain()
        
        val searchUrl = "$domain/?s=${query.replace(" ", "+")}"
        Log.d("DIZIYO", "🔍 Searching: $searchUrl")

        val doc = app.get(searchUrl, interceptor = CloudflareKiller()).document

        return doc.select(".result-item, article, .item, .movie-item, .search-item").mapNotNull { 
            it.toSearch() 
        }
    }

    // ================= LOAD =================

    override suspend fun load(url: String): LoadResponse? {
        Log.d("DIZIYO", "📥 Loading details: $url")

        val doc = app.get(url, interceptor = CloudflareKiller()).document

        // 🔥 GÜNCELLENMİŞ DETAY SEÇİCİLERİ
        val title = doc.selectFirst("h1, .movie-title, .series-title, .entry-title")?.text()?.trim() 
            ?: return null
            
        val poster = fixUrlNull(
            doc.selectFirst(".poster img, .thumb img, .movie-poster img")?.attr("src")
            ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
        )
        
        val desc = doc.selectFirst(".description, .summary, .plot, .content p, .info p")?.text()
            ?: doc.selectFirst("meta[property=og:description]")?.attr("content")

        // Bölümleri kontrol et
        val episodeElements = doc.select(".episode-list li a, .episodes a, .season-list a, #episodes li a")
        
        return if (episodeElements.isNotEmpty()) {
            // Dizi/Anime
            val episodes = episodeElements.mapIndexed { index, element ->
                val epName = element.text().ifEmpty { "Bölüm ${index + 1}" }
                val epUrl = fixUrlNull(element.attr("href")) ?: return@mapIndexed null
                newEpisode(epUrl) {
                    name = epName
                    episode = index + 1
                }
            }.filterNotNull()

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

    // ================= LOAD LINKS =================

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("DIZIYO", "▶️ Loading links: $data")

        return try {
            val doc = app.get(data, interceptor = CloudflareKiller()).document

            // Post ID bul
            val postId = Regex("postid-(\\d+)").find(doc.html())?.groupValues?.get(1)
                ?: Regex("""data-post=["']?(\d+)["']?""").find(doc.html())?.groupValues?.get(1)
                ?: Regex("""post_id["']?\s*:\s*["']?(\d+)""").find(doc.html())?.groupValues?.get(1)

            if (postId == null) {
                Log.e("DIZIYO", "❌ Post ID bulunamadı")
                return false
            }

            Log.d("DIZIYO", "🆔 Post ID: $postId")

            val ajaxUrl = "$mainUrl/wp-admin/admin-ajax.php"
            
            // Film ve dizi için farklı tip kontrolü
            val type = if (data.contains("/dizi/") || data.contains("/anime/")) "tv" else "movie"

            val ajaxRes = app.post(
                ajaxUrl,
                data = mapOf(
                    "action" to "doo_player_ajax",
                    "post" to postId,
                    "nume" to "1",
                    "type" to type
                ),
                headers = mapOf(
                    "X-Requested-With" to "XMLHttpRequest",
                    "Referer" to data,
                    "Content-Type" to "application/x-www-form-urlencoded"
                ),
                interceptor = CloudflareKiller()
            ).text

            Log.d("DIZIYO", "📡 AJAX Response: ${ajaxRes.take(200)}")

            // iframe'leri çıkar
            val servers = Regex("""src=["'](https?:\\/\\/[^"']+)["']""").findAll(ajaxRes)
                .map { it.groupValues[1].replace("\\/", "/") }
                .toList()

            Log.d("DIZIYO", "🌐 Found ${servers.size} servers")

            for (server in servers) {
                try {
                    when {
                        server.contains(".m3u8", ignoreCase = true) -> {
                            callback.invoke(
                                newExtractorLink(name, "Direct m3u8", server, ExtractorLinkType.M3U8) {
                                    this.referer = mainUrl
                                }
                            )
                        }
                        
                        server.contains("video/") || server.contains("embed/") -> {
                            val hash = Regex("""video\\/([a-zA-Z0-9]+)""").find(server)?.groupValues?.get(1)
                                ?: Regex("""embed\\/([a-zA-Z0-9]+)""").find(server)?.groupValues?.get(1)
                            
                            if (hash != null) {
                                val videoRes = app.post(
                                    "$server?do=getVideo",
                                    data = mapOf("hash" to hash, "r" to mainUrl),
                                    headers = mapOf("Referer" to server),
                                    interceptor = CloudflareKiller()
                                ).text

                                val json = JSONObject(videoRes)
                                val sources = json.getJSONArray("videoSources")
                                
                                for (i in 0 until sources.length()) {
                                    val source = sources.getJSONObject(i)
                                    val file = source.getString("file")
                                    val label = source.optString("label", "Unknown")
                                    
                                    M3u8Helper.generateM3u8(
                                        name,
                                        file,
                                        server
                                    ).forEach(callback)
                                }
                            }
                        }
                        
                        else -> {
                            loadExtractor(server, data, subtitleCallback, callback)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DIZIYO", "❌ Server failed: $server - ${e.message}")
                }
            }

            true
        } catch (e: Exception) {
            Log.e("DIZIYO", "❌ Total failure: ${e.message}")
            false
        }
    }
}
