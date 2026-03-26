// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.DdosGuardKiller
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.jsoup.Jsoup

class Dizist : MainAPI() {
    override var mainUrl = "https://dizist.live"
    override var name = "Dizist"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.TvSeries)
    
    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 250L
    override var sequentialMainPageScrollDelay = 250L
    
    private var ddosGuardKiller = DdosGuardKiller(true)
    private var cookieler: Map<String, String>? = null
    private var cKey: String? = null
    private var cValue: String? = null
    private val initMutex = Mutex()

    private val defaultHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer" to "$mainUrl/"
    )

    private suspend fun initSession() {
        if (cookieler != null && cKey != null && cValue != null) return
        initMutex.withLock {
            if (cookieler != null && cKey != null && cValue != null) return@withLock
            try {
                val resp = app.get("$mainUrl/", headers = defaultHeaders, timeout = 120)
                cookieler = resp.cookies
                val document = resp.document
                cKey = document.selectFirst("input[name=cKey]")?.`val`()
                cValue = document.selectFirst("input[name=cValue]")?.`val`()
            } catch (e: Exception) {
                Log.e("kraptor_Dizist", "Oturum Hatası: ${e.message}")
            }
        }
    }

    override val mainPage = mainPageOf(
        "$mainUrl/" to "Yeni Eklenen Bölümler",
        "$mainUrl/yabanci-diziler" to "Yabancı Diziler",
        "$mainUrl/animeler" to "Animeler",
        "$mainUrl/bolumler" to "Son Bölümler",
        "$mainUrl/dil/turkce-altyazi" to "Türkçe Altyazı",
        "$mainUrl/dil/turkce-dublaj" to "Türkçe Dublaj",
        "$mainUrl/asyadizileri" to "Asya Dizileri",
        "$mainUrl/dil/yerli" to "Yerli Diziler",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        initSession()
        
        // Sayfalama mantığını düzelttik: Ana sayfa için page/x eklemiyoruz, kategoriler için ekliyoruz.
        val url = if (request.data == "$mainUrl/") {
            if (page <= 1) request.data else return newHomePageResponse(request.name, emptyList(), false)
        } else {
            if (page <= 1) request.data else "${request.data.removeSuffix("/")}/page/$page"
        }

        val response = app.get(url, cookies = cookieler ?: emptyMap(), headers = defaultHeaders, interceptor = ddosGuardKiller)
        val document = response.document

        // CSS Seçicilerini (Selector) Dizist'in yeni grid yapısına göre güncelledik
        val items = if (request.name == "Yeni Eklenen Bölümler") {
            document.select("div.poster-xs, div.poster-small")
        } else {
            // "Diziler", "Animeler" vb. kategoriler için poster-long ya da genel kart yapısı
            document.select("div.poster-long, div.poster-long.w-full, div.relative.group.overflow-hidden")
        }

        val home = items.mapNotNull { it.toMainPageResult() }
        
        // Yeni eklenenler sayfası tek sayfa, diğerlerinde hasNext kontrolü
        val hasNext = home.isNotEmpty() && request.name != "Yeni Eklenen Bölümler"
        
        return newHomePageResponse(request.name, home, hasNext = hasNext)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val a = this.selectFirst("a") ?: return null
        val rawHref = a.attr("href") ?: ""
        if (rawHref.isEmpty()) return null
        
        // URL'yi temizle ve dizi ana sayfasına yönlendir
        val href = fixUrlNull(rawHref.replace("/izle/", "/dizi/").replace(Regex("-[0-9]+-bolum.*$"), "")) ?: return null
        
        val title = a.attr("title").ifEmpty { this.selectFirst("img")?.attr("alt") } ?: "Bilinmeyen"
        
        val img = this.selectFirst("img")
        val posterUrl = fixUrlNull(
            img?.attr("data-srcset")?.substringBefore(" ") 
            ?: img?.attr("data-src") 
            ?: img?.attr("src")
        )

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { 
            this.posterUrl = posterUrl 
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        initSession()
        val apiResponse = app.post(
            "$mainUrl/bg/searchcontent", 
            cookies = cookieler ?: emptyMap(), 
            headers = defaultHeaders,
            data = mapOf(
                "cKey" to (cKey ?: ""),
                "cValue" to (cValue ?: ""),
                "searchTerm" to query
            )
        )
        
        return try {
            val dataObj = JSONObject(apiResponse.text).getJSONObject("data")
            val html = dataObj.getString("html")
            val doc = Jsoup.parseBodyFragment(html)
            doc.select("ul.flex.flex-wrap li").mapNotNull { it.toSearchResult() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val a = this.selectFirst("a") ?: return null
        val title = a.selectFirst("span.truncate")?.text()?.trim() ?: return null
        val href = fixUrlNull(a.attr("href")) ?: return null
        val img = a.selectFirst("img")
        val poster = fixUrlNull(img?.attr("data-srcset")?.substringBefore(" 1x") ?: img?.attr("src"))
            
        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = poster
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        initSession()
        val urlget = app.get(url, cookies = cookieler ?: emptyMap(), headers = defaultHeaders, interceptor = ddosGuardKiller)
        val document = urlget.document
        val text = urlget.text

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("a.block img")?.attr("data-srcset")?.substringBefore(" 1x"))
        val description = document.selectFirst("div.series-profile-summary > p:nth-child(3)")?.text()?.trim()
        val year = document.selectFirst("li.sm\\:w-1\\/5:nth-child(5) > p:nth-child(2)")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("span.block a").map { it.text() }
        val rating = document.selectFirst("strong.color-imdb")?.text()?.trim()
        
        val recommendations = document.select("div.poster-long, div[class*='poster-long']").mapNotNull { it.toRecommendationResult() }
        
        val duration = document.selectFirst("li.sm\\:w-1\\/5:nth-child(2) > p:nth-child(2)")?.text()?.replace(" dk", "")
            ?.split(" ")?.first()?.trim()?.toIntOrNull()
            
        val actors = document.select("li.w-auto.md\\:w-full.flex-shrink-0").mapNotNull { aktor ->
            val aktorIsim = aktor.selectFirst("p.truncate")?.text()?.trim() ?: return@mapNotNull null
            val aktorResim = fixUrlNull(aktor.selectFirst("img")?.attr("data-srcset"))?.substringBefore(" ")
            Actor(name = aktorIsim, fixUrlNull(aktorResim))
        }
        
        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }
            
        val regex = Regex(
            pattern = ",\"url\":\"([^\"]*)\",\"dateModified\":\"[^\"]*\"",
            options = setOf(RegexOption.IGNORE_CASE)
        )
        
        val bolumListesi: List<Episode> = regex.findAll(text)
            .map { match ->
                val raw = match.groupValues[1].replace("\\", "")
                val fullHref = if (!raw.contains("-bolum")) {
                    raw.replace("/sezon/", "/izle/").trimEnd('/') + "-1-bolum"
                } else {
                    raw
                }
                val href = fixUrlNull(fullHref)
                val bolumSayisi = href?.substringBefore("-bolum")?.substringAfterLast("-")?.toIntOrNull()
                val sezonSayisi = href?.substringBefore("-sezon")?.substringAfterLast("-")?.replace("-","")?.toIntOrNull()
                
                newEpisode(href) {
                    episode = bolumSayisi
                    name = "Bölüm"
                    season = sezonSayisi
                }
            }
            .toList()

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, bolumListesi) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from10(rating)
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val a = this.selectFirst("a") ?: return null
        val title = a.attr("title") ?: return null
        val href = fixUrlNull(a.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-srcset")?.substringBefore(" "))
        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        initSession()
        val document = app.get(data, cookies = cookieler ?: emptyMap(), headers = defaultHeaders).document
        val kaynakLinkleri = document.select("div.series-watch-alternatives li a.focus\\:outline-none")

        kaynakLinkleri.forEach { linkElem ->
            val href = linkElem.attr("href")
            try {
                val iframeSrc = if (href.contains("player=0")) {
                    fixUrlNull(document.selectFirst("iframe")?.attr("src")).toString()
                } else {
                    val yeniDoc = app.get(href, headers = defaultHeaders).document
                    fixUrlNull(yeniDoc.selectFirst("iframe")?.attr("src")).toString()
                }
                if (iframeSrc.isNotEmpty() && iframeSrc != "null") {
                    loadExtractor(iframeSrc, "$mainUrl/", subtitleCallback, callback)
                }
            } catch (e: Exception) {
                Log.e("kraptor_Dizist", "Link Hatası: ${e.message}")
            }
        }
        return true
    }
}
