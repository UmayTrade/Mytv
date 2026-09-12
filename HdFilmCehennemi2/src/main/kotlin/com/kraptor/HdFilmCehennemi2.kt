// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Base64
import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class HdFilmCehennemi2 : MainAPI() {
    override var mainUrl              = "https://www.hdfilmcehennemi2.biz"
    override var name                 = "HdFilmCehennemi2"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/filmler/"                          to "Yeni Eklenenler",
        "${mainUrl}/en-cok-izlenen-filmler/"           to "En Çok İzlenenler",
        "${mainUrl}/en-cok-yorumlananlar/"             to "En Çok Yorumlananlar",
        "${mainUrl}/en-cok-begenilenler/"              to "En Çok Beğenilenler",
        "${mainUrl}/imdb-puani/"                       to "IMDB 7+",
        "${mainUrl}/tur/aksiyon-filmleri-izle/"        to "Aksiyon",
        "${mainUrl}/tur/bilim-kurgu-filmleri-izle/"    to "Bilim Kurgu",
        "${mainUrl}/tur/korku-filmleri/"               to "Korku",
        "${mainUrl}/tur/macera-filmleri/"              to "Macera",
        "${mainUrl}/tur/dram-filmleri/"                to "Dram",
        "${mainUrl}/tur/komedi-filmleri/"              to "Komedi",
        "${mainUrl}/tur/gerilim-filmleri/"             to "Gerilim",
        "${mainUrl}/tur/romantik-filmler/"             to "Romantik",
        "${mainUrl}/tur/fantastik-filmleri/"           to "Fantastik",
        "${mainUrl}/tur/gizem-filmleri/"               to "Gizem",
        "${mainUrl}/tur/suc-filmleri/"                 to "Suç",
        "${mainUrl}/tur/savas-filmleri/"               to "Savaş",
        "${mainUrl}/tur/tarih-filmleri/"               to "Tarih",
        "${mainUrl}/tur/belgesel-filmleri/"            to "Belgesel",
        "${mainUrl}/tur/animasyon-film-izle/"          to "Animasyon",
        "${mainUrl}/tur/aile-filmleri/"                to "Aile",
        "${mainUrl}/tur/biyografi-filmleri/"           to "Biyografi",
        "${mainUrl}/tur/spor-filmleri/"                to "Spor",
        "${mainUrl}/tur/muzik-filmleri/"               to "Müzik",
        "${mainUrl}/tur/western-filmleri/"             to "Western"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Yeni site yapısı: /filmler/ sayfası ?page=N ile sayfalanıyor
        val url = if (page == 1) {
            request.data
        } else {
            "${request.data}?page=$page"
        }
        val document = app.get(url).document

        // ✅ Yeni seçici: article içeren a[href] kartları
        val home = document.select("a.group\\/poster, article").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    /**
     * Kart yapısı:
     * <a href="..." class="group/poster">
     *   <article>...
     *     <img src="..." alt="Title">
     *     <h3 title="Title">Title</h3>
     *   </article>
     * </a>
     */
    private fun Element.toMainPageResult(): SearchResponse? {
        // Element <a> ise doğrudan kullan, değilse içindeki <a>'yı bul
        val anchor = if (this.tagName() == "a") this else this.selectFirst("a[href]") ?: return null
        val href = fixUrlNull(anchor.attr("href")) ?: return null

        // /filmler/ gibi kategorileri atla
        if (href.contains("/tur/") || href.contains("/yil/") || href.contains("/kategori/") ||
            href.contains("/ulke/") || href.contains("/dizi-izle") || href.contains("/filmler")) {
            return null
        }

        val img = anchor.selectFirst("img")
        val title = img?.attr("alt")?.trim()
            ?: anchor.selectFirst("h3")?.attr("title")?.trim()
            ?: anchor.selectFirst("h3")?.text()?.trim()
            ?: return null

        val posterUrl = fixUrlNull(img?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        // Site artık /api/search?q= endpoint'i kullanıyor (JSON döner)
        // Ancak HTML fallback olarak /?s= de var
        return try {
            val apiUrl = "${mainUrl}/api/search?q=${query}"
            val response = app.get(apiUrl).text

            // JSON parse
            val json = org.json.JSONObject(response)
            val data = json.optJSONArray("data") ?: return emptyList()
            val results = mutableListOf<SearchResponse>()

            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val title = item.optString("title", "").ifBlank { continue }
                val slug = item.optString("slug", "")
                if (slug.isBlank()) continue
                val href = "$mainUrl/$slug"
                val posterUrl = item.optString("posterUrl", "").ifBlank { null }
                results.add(newMovieSearchResponse(title, href, TvType.Movie) {
                    this.posterUrl = posterUrl
                })
            }
            results
        } catch (e: Exception) {
            Log.e("cehennem", "Search API hatası: ${e.message}")
            // HTML fallback
            val document = app.get("${mainUrl}/?s=${query}").document
            document.select("article").mapNotNull { it.toMainPageResult() }
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        // ✅ Başlık: <h1 class="text-4xl font-bold text-white mb-2">Supergirl izle</h1>
        val title = document.selectFirst("h1.text-4xl, h1")?.text()?.trim()
            ?.replace(Regex("\\s*izle\\s*$", RegexOption.IGNORE_CASE), "")
            ?.trim()
            ?: return null

        // ✅ Poster: og:image meta veya detay sayfasındaki poster
        val poster = fixUrlNull(
            document.selectFirst("meta[property=og:image]")?.attr("content")
                ?: document.selectFirst("div.hdf-detail-poster img")?.attr("src")
        )

        // ✅ Açıklama: og:description veya "Özet" bölümü
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")
            ?: document.selectFirst("section.hdf-panel:has(h2:contains(Özet)) div[x-ref=content]")?.text()?.trim()

        // ✅ Yıl: /yil/YYYY/ linkinden
        val year = document.selectFirst("a[href*='/yil/']")?.text()?.trim()?.toIntOrNull()
            ?: Regex("""/yil/(\d{4})""").find(document.html())?.groupValues?.get(1)?.toIntOrNull()

        // ✅ Puan: yıldız svg yanındaki sayı
        val rating = document.selectFirst("span:has(svg.text-yellow-500)")?.text()?.trim()
            ?.replace(Regex("[^0-9.,]"), "")?.replace(",", ".")

        // ✅ Türler: /tur/ linkleri
        val tags = document.select("a[href*='/tur/']").map { it.text().trim() }
            .filter { it.isNotBlank() }
            .distinct()

        // ✅ Oyuncular: .flex-none.w-24 blokları
        val actors = document.select("div.flex-none.w-24").mapNotNull { el ->
            val name = el.selectFirst("h4")?.text()?.trim() ?: return@mapNotNull null
            val role = el.selectFirst("p")?.text()?.trim()
            if (name.isBlank()) null else Actor(name, role)
        }

        // ✅ Fragman: youtube embed
        val trailer = Regex("""youtube\.com/embed/([A-Za-z0-9_-]+)""")
            .find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot      = description
            this.year      = year
            this.tags      = tags
            if (!rating.isNullOrBlank()) this.score = Score.from10(rating)
            addActors(actors)
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        // ✅ VİDEO KAYNAĞI: JavaScript içindeki base64 encoded template
        // x-data="videoPlayerData(JSON.parse('{\"dual\":[{\"link\":\"...\",\"template\":\"BASE64...\"}]}')"
        val videoMatch = Regex(
            """videoPlayerData\(JSON\.parse\('(.+?)'\),\s*'(\w+)'""",
            RegexOption.DOT_MATCHES_ALL
        ).find(document.html())

        if (videoMatch == null) {
            Log.e("cehennem", "videoPlayerData bulunamadı")
            return false
        }

        val jsonStr = videoMatch.groupValues[1]
            .replace("\\u0022", "\"")
            .replace("\\/", "/")
            .replace("\\n", "\n")

        try {
            val json = org.json.JSONObject(jsonStr)
            var found = false

            // Tüm dil seçeneklerini gez (dual, tr, sub vs.)
            json.keys().forEach { langKey ->
                val videos = json.optJSONArray(langKey) ?: return@forEach
                for (i in 0 until videos.length()) {
                    val video = videos.getJSONObject(i)
                    val link = video.optString("link", "")
                    val templateB64 = video.optString("template", "")
                    val serviceName = video.optString("service_name", "VIP")

                    if (link.isBlank() || templateB64.isBlank()) continue

                    // Template base64 decode
                    val template = try {
                        String(Base64.decode(templateB64, Base64.DEFAULT), Charsets.UTF_8)
                    } catch (e: Exception) {
                        Log.e("cehennem", "Template decode hatası: ${e.message}")
                        continue
                    }

                    // {url} ve {slug} placeholder'larını doldur
                    val slug = video.optString("slug", "")
                    val iframeHtml = template
                        .replace("{url}", link)
                        .replace("{slug}", slug)

                    // iframe'den data-src veya src çek
                    val srcRegex = Regex("""(?:data-)?src=["']([^"']+)["']""")
                    var videoUrl = srcRegex.find(iframeHtml)?.groupValues?.get(1) ?: continue

                    // Protokol ekle
                    if (videoUrl.startsWith("//")) videoUrl = "https:$videoUrl"
                    else if (videoUrl.startsWith("/")) videoUrl = "$mainUrl$videoUrl"

                    Log.d("cehennem", "Video URL ($langKey / $serviceName): $videoUrl")

                    loadExtractor(
                        url = videoUrl,
                        referer = data,
                        subtitleCallback = subtitleCallback,
                        callback = callback
                    )
                    found = true
                }
            }

            return found
        } catch (e: Exception) {
            Log.e("cehennem", "JSON parse hatası: ${e.message}")
            return false
        }
    }
}
