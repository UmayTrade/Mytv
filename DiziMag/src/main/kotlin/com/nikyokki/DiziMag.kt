package com.nikyokki

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.Base64
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class DiziMag : MainAPI() {
    // BOŞLUKSUZ!
    override var mainUrl = "https://dizimag.eu"
    override var name = "DiziMag"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    // Cloudflare için daha uzun bekleme
    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 1200L
    override var sequentialMainPageScrollDelay = 1200L

    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    )

    private fun getHeaders(): Map<String, String> {
        return mapOf(
            "User-Agent" to userAgents.random(),
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
            "Accept-Encoding" to "gzip, deflate, br",
            "DNT" to "1",
            "Connection" to "keep-alive",
            "Upgrade-Insecure-Requests" to "1",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "none",
            "Sec-Fetch-User" to "?1",
            "Cache-Control" to "no-cache",
            "Pragma" to "no-cache"
        )
    }

    override val mainPage = mainPageOf(
        "$mainUrl/kategori/aile" to "📺 Aile",
        "$mainUrl/kategori/aksiyon-macera" to "📺 Aksiyon & Macera",
        "$mainUrl/kategori/animasyon" to "📺 Animasyon",
        "$mainUrl/kategori/belgesel" to "📺 Belgesel",
        "$mainUrl/kategori/bilim-kurgu-fantazi" to "📺 Bilim Kurgu & Fantazi",
        "$mainUrl/kategori/dram" to "📺 Dram",
        "$mainUrl/kategori/gizem" to "📺 Gizem",
        "$mainUrl/kategori/komedi" to "📺 Komedi",
        "$mainUrl/kategori/savas-politik" to "📺 Savaş & Politik",
        "$mainUrl/kategori/suc" to "📺 Suç",
        
        "$mainUrl/kategori/aile?tur=film" to "🎬 Aile Filmleri",
        "$mainUrl/kategori/animasyon?tur=film" to "🎬 Animasyon Filmleri",
        "$mainUrl/kategori/bilim-kurgu?tur=film" to "🎬 Bilim Kurgu Filmleri",
        "$mainUrl/kategori/dram?tur=film" to "🎬 Dram Filmleri",
        "$mainUrl/kategori/fantastik?tur=film" to "🎬 Fantastik Filmleri",
        "$mainUrl/kategori/gerilim?tur=film" to "🎬 Gerilim Filmleri",
        "$mainUrl/kategori/gizem?tur=film" to "🎬 Gizem Filmleri",
        "$mainUrl/kategori/komedi?tur=film" to "🎬 Komedi Filmleri",
        "$mainUrl/kategori/korku?tur=film" to "🎬 Korku Filmleri",
        "$mainUrl/kategori/macera?tur=film" to "🎬 Macera Filmleri",
        "$mainUrl/kategori/romantik?tur=film" to "🎬 Romantik Filmleri",
        "$mainUrl/kategori/savas?tur=film" to "🎬 Savaş Filmleri",
        "$mainUrl/kategori/suc?tur=film" to "🎬 Suç Filmleri",
        "$mainUrl/kategori/tarih?tur=film" to "🎬 Tarih Filmleri",
        "$mainUrl/kategori/vahsi-bati?tur=film" to "🎬 Vahşi Batı Filmleri"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val baseUrl = request.data.trim()
        
        // URL oluşturma
        val url = when {
            page == 1 -> baseUrl
            baseUrl.contains("?") -> "$baseUrl&page=$page"
            else -> "$baseUrl/$page"
        }
        
        Log.d("DiziMag", "Fetching URL: $url")

        try {
            val response = app.get(
                url, 
                headers = getHeaders(), 
                referer = "$mainUrl/",
                timeout = 30  // Timeout artır
            )
            
            Log.d("DiziMag", "Response code: ${response.code}")
            
            if (response.code == 403) {
                throw ErrorLoadingException("Cloudflare 403 - Engel")
            }
            if (response.code == 503) {
                throw ErrorLoadingException("Cloudflare 503 - Service Unavailable")
            }
            if (!response.isSuccessful) {
                throw ErrorLoadingException("HTTP ${response.code}")
            }

            val document = response.document
            
            // Ana seçici
            var items = document.select("div.poster-long")
            Log.d("DiziMag", "Found ${items.size} items with 'div.poster-long'")
            
            // Boşsa alternatif dene
            if (items.isEmpty()) {
                items = document.select("div.poster, .movie-item, .series-item, .item")
                Log.d("DiziMag", "Alt selector found ${items.size} items")
            }
            
            // Hala boşsa tüm HTML'i logla (debug için)
            if (items.isEmpty()) {
                Log.e("DiziMag", "HTML Preview: ${document.html().take(500)}")
            }

            val home = items.mapNotNull { it.toSearchResult() }
            
            return newHomePageResponse(
                request.name, 
                home, 
                hasNext = home.isNotEmpty()
            )
            
        } catch (e: Exception) {
            Log.e("DiziMag", "Error loading main page: ${e.message}")
            throw e
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        // Esnek başlık seçimi
        val title = this.selectFirst("h2, h3, .title, .name, [class*='title']")?.text()?.trim() 
            ?: return null
        
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null

        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.let { img ->
                img.attr("data-src").takeIf { it.isNotBlank() }
                    ?: img.attr("src").takeIf { it.isNotBlank() }
                    ?: img.attr("data-original")
            }
        )

        // Tip belirleme
        val isTvSeries = when {
            href.contains("/dizi/") -> true
            href.contains("/film/") -> false
            else -> !title.contains("film", true) || title.contains("sezon", true)
        }

        return if (isTvSeries) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
    }

    // ... search ve load fonksiyonları öncekiyle aynı ...
}
