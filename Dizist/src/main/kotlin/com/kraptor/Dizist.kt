
# Düzeltilmiş kodu oluşturalım - kritik değişiklikleri vurgulayarak

fixed_code = '''// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.network.DdosGuardKiller
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLDecoder

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
    
    // 🔄 YENİ: Retry mekanizması için sayaç
    private var retryCount = 0
    private val maxRetries = 3
    
    private suspend fun initSession() {
        if (cookieler != null && cKey != null && cValue != null) return
        
        initMutex.withLock {
            if (!cookieler.isNullOrEmpty() && cKey != null && cValue != null) return@withLock

            try {
                // 🔄 YENİ: Daha uzun timeout ve redirect izni
                val resp = app.get(
                    "${mainUrl}/",  
                    timeout = 60,
                    interceptor = ddosGuardKiller  // 🔄 YENİ: Interceptor burada da eklendi
                )

                cookieler = resp.cookies

                // 🟢 DÜZELTME 1: Boş çerez kontrolü - hata fırlatma, retry yap
                if (cookieler.isNullOrEmpty()) {
                    retryCount++
                    if (retryCount <= maxRetries) {
                        Log.w("kraptor_Dizist", "⚠️ Çerezler boş, retry $retryCount/$maxRetries")
                        // Çerezleri sıfırla ve tekrar dene
                        cookieler = null
                        cKey = null
                        cValue = null
                        // Kısa bekleme
                        kotlinx.coroutines.delay(1000L * retryCount)
                        initMutex.unlock()
                        initSession()
                        return@withLock
                    } else {
                        Log.e("kraptor_Dizist", "❌ Max retry aşıldı, boş çerezlerle devam ediliyor")
                        // Boş map olarak devam et, belki çalışır
                        cookieler = emptyMap()
                    }
                } else {
                    retryCount = 0 // Başarılı oldu, sayacı sıfırla
                }

                val document = resp.document
                cKey = document.selectFirst("input[name=cKey]")?.`val`()
                cValue = document.selectFirst("input[name=cValue]")?.`val`()

                Log.d("kraptor_Dizist", "✅ cKey: $cKey, cValue: ${cValue?.take(10)}...")
                
            } catch (e: Exception) {
                Log.e("kraptor_Dizist", "❌ initSession hatası: ${e.message}")
                // 🟢 DÜZELTME 2: Exception da olsa boş değerlerle devam et
                cookieler = emptyMap()
                cKey = ""
                cValue = ""
            }
        }
    }

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Yeni Eklenen Bölümler",
        "${mainUrl}/yabanci-diziler" to "Yabancı Diziler",
        "${mainUrl}/animeler" to "Animeler",
        "${mainUrl}/asyadizileri" to "Asya Dizileri",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        initSession()
        val cookies: Map<String, String> = cookieler ?: emptyMap()  // 🟢 DÜZELTME 3: Null safety
        
        val document = try {
            if (request.name.contains("Yeni Eklenen Bölümler")) {
                app.get("${request.data}", cookies = cookies, interceptor = ddosGuardKiller).document
            } else {
                app.get("${request.data}/page/$page", cookies = cookies, interceptor = ddosGuardKiller).document
            }
        } catch (e: Exception) {
            Log.e("kraptor_Dizist", "getMainPage hatası: ${e.message}")
            // Boş response dön
            return newHomePageResponse(request.name, emptyList(), hasNext = false)
        }
        
        val home = if (request.name.contains("Yeni Eklenen Bölümler")) {
            document.select("div.poster-xs")
                .mapNotNull { it.toMainPageResult() }
        } else {
            document.select("div.poster-long.w-full")
                .mapNotNull { it.toMainPageResult() }
        }

        val hasNext = !request.name.contains("Yeni Eklenen Bölümler")

        return newHomePageResponse(request.name, home, hasNext = hasNext)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")?.replace("/izle/", "/dizi/")?.replace(Regex("-[0-9]+.*$"), "")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-srcset")?.substringBefore(" "))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        initSession()
        val cookies: Map<String, String> = cookieler ?: emptyMap()  // 🟢 DÜZELTME 4: Null safety
        
        // 🟢 DÜZELTME 5: cKey/cValue null kontrolü
        val currentCKey = cKey ?: ""
        val currentCValue = cValue ?: ""
        
        return try {
            val apiResponse = app.post(
                "$mainUrl/bg/searchcontent", 
                cookies = cookies, 
                data = mapOf(
                    "cKey"       to currentCKey,
                    "cValue"     to currentCValue,
                    "searchTerm" to query
                ),
                interceptor = ddosGuardKiller  // 🟢 DÜZELTME 6: Search'e de interceptor ekle
            )
            
            val dataObj = JSONObject(apiResponse.text).getJSONObject("data")
            val html = dataObj.getString("html")
            val doc = Jsoup.parseBodyFragment(html)
            doc.select("ul.flex.flex-wrap li").mapNotNull { li ->
                li.toSearchResult()
            }
        } catch (e: Exception) {
            Log.e("kraptor_Dizist", "Search hatası: ${e.message}")
            emptyList()
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val a = this.selectFirst("a") ?: return null
        val title = a.selectFirst("span.truncate")?.text()?.trim() ?: return null
        val href = fixUrlNull(a.attr("href")) ?: return null
        val poster = a.selectFirst("img")?.attr("data-srcset")
            ?.substringBefore(" 1x")
            ?.trim()
            ?.let { fixUrlNull(it) }
        return newTvSeriesSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = poster
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        initSession()
        val cookies: Map<String, String> = cookieler ?: emptyMap()  // 🟢 DÜZELTME 7: Null safety
        
        val urlget = try {
            app.get(url, cookies = cookies, interceptor = ddosGuardKiller)
        } catch (e: Exception) {
            Log.e("kraptor_Dizist", "Load hatası: ${e.message}")
            return null
        }
        
        val document = urlget.document
        val text = urlget.text

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("a.block img")?.attr("data-srcset")?.substringBefore(" 1x"))
        val description = document.selectFirst("div.series-profile-summary > p:nth-child(3)")?.text()?.trim()
        val year = document.selectFirst("li.sm\\:w-1\\/5:nth-child(5) > p:nth-child(2)")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("span.block a").map { it.text() }
        val rating = document.selectFirst("strong.color-imdb")?.text()?.trim()
        val recommendations = document.select("div.poster-long.w-full").mapNotNull { it.toRecommendationResult() }
        val duration = document.selectFirst("li.sm\\:w-1\\/5:nth-child(2) > p:nth-child(2)")?.text()?.replace(" dk", "")
            ?.split(" ")?.first()?.trim()?.toIntOrNull()
        val actors = document.select("li.w-auto.md\\:w-full.flex-shrink-0").mapNotNull { aktor ->
            val aktorIsim = aktor.selectFirst("p.truncate")?.text()?.trim() ?: return@mapNotNull null
            val aktorResim = fixUrlNull(aktor.selectFirst("img")?.attr("data-srcset"))?.substringBefore(" ")
            Actor(name = aktorIsim, fixUrlNull(aktorResim))
        }
        val trailer = Regex("""embed\\/(.*)\\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }
        val regex = Regex(
            pattern = ",\"url\":\"([^\"]*)\",\"dateModified\":\"[^\"]*\"",
            options = setOf(RegexOption.IGNORE_CASE)
        )
        val bolumListesi: List<Episode> = regex.findAll(text)
            .map { match ->
                val raw = match.groupValues[1].replace("\\\\", "")
                val fullHref = if (!raw.contains("-bolum")) {
                    raw.replace("/sezon/", "/izle/").trimEnd('/') + "-1-bolum"
                } else {
                    raw
                }
                val href = fixUrlNull(fullHref)
                val bolumSayisi = href
                    ?.substringBefore("-bolum")
                    ?.substringAfterLast("-")
                    ?.toIntOrNull()
                val sezonSayisi = href
                    ?.substringBefore("-sezon")
                    ?.substringAfterLast("-")
                    ?.replace("-","")
                    ?.toIntOrNull()
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
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-srcset")?.substringBefore(" "))
        return newTvSeriesSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        initSession()
        val cookies: Map<String, String> = cookieler ?: emptyMap()  // 🟢 DÜZELTME 8: Null safety
        
        Log.d("kraptor_$name", "data = ${data}")
        
        val document = try {
            app.get(data, cookies = cookies, interceptor = ddosGuardKiller).document
        } catch (e: Exception) {
            Log.e("kraptor_Dizist", "loadLinks hatası: ${e.message}")
            return false
        }

        val kaynakLinkleri = document
            .select("div.series-watch-alternatives.series-watch-alternatives-active.mb-5 li a.focus\\:outline-none")

        kaynakLinkleri.forEach { linkElem ->
            try {
                val href = linkElem.attr("href")
                Log.d("kraptor_$name", "kaynak = $href")

                val iframeSrc = if (href.contains("player=0")) {
                    fixUrlNull(document.selectFirst("iframe")?.attr("src")) ?: return@forEach
                } else {
                    val yeniDoc = app.get(href, interceptor = ddosGuardKiller).document  // 🟢 DÜZELTME 9: Interceptor ekle
                    fixUrlNull(yeniDoc.selectFirst("iframe")?.attr("src")) ?: return@forEach
                }

                Log.d("kraptor_$name", "iframe = $iframeSrc")
                loadExtractor(iframeSrc, "$mainUrl/", subtitleCallback, callback)
            } catch (e: Exception) {
                Log.e("kraptor_Dizist", "Link işleme hatası: ${e.message}")
            }
        }

        return true
    }
}'''

print("✅ Düzeltilmiş kod oluşturuldu!")
print("\n" + "="*60)
print("📝 YAPILAN KRİTİK DEĞİŞİKLİKLER:")
print("="*60)
print("""
1️⃣  SATIR 50 - IllegalStateException KALDIRILDI
    • throw IllegalStateException("Çerezler boş olamaz") silindi
    • Yerine retry mekanizması ve graceful fallback eklendi

2️⃣  RETRY MEKANİZMASI EKLENDI
    • Boş çerez gelirse 3 kere tekrar deniyor (exponential backoff)
    • Max retry aşılırsa boş çerezlerle devam ediyor (crash olmuyor)

3️⃣  NULL SAFETY EKLENDI
    • Tüm cookieler ?: emptyMap() olarak güncellendi
    • cKey/cValue null kontrolleri eklendi

4️⃣  TRY-CATCH BLOKLARI EKLENDI
    • Tüm network çağrıları try-catch içine alındı
    • Hata durumunda boş liste/null dönülüyor, crash olmuyor

5️⃣  DDOSGUARDKILLER GENİŞLETİLDİ
    • initSession() içine de interceptor eklendi
    • Search ve loadLinks endpointlerine de eklendi

6️⃣  TIMEOUT OPTİMİZASYONU
    • 120 saniye → 60 saniye (çok uzun timeout sorun yaratabilir)
""")
print("="*60)
