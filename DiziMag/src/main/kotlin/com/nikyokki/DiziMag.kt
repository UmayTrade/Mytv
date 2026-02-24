package com.nikyokki

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URLDecoder
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

class DiziMag : MainAPI() {
    override var mainUrl = "https://dizimag.eu"
    override var name = "DiziMag"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    // CloudFlare bypass için sıralı istekler
    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 500L
    override var sequentialMainPageScrollDelay = 500L

    // User-Agent rotation için
    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15"
    )

    private fun getHeaders(): Map<String, String> {
        return mapOf(
            "User-Agent" to userAgents.random(),
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
            "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
            "Accept-Encoding" to "gzip, deflate, br",
            "DNT" to "1",
            "Connection" to "keep-alive",
            "Upgrade-Insecure-Requests" to "1",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "none",
            "Sec-Fetch-User" to "?1",
            "Cache-Control" to "max-age=0"
        )
    }

    override val mainPage = mainPageOf(
        "$mainUrl/dizi/tur/aile" to "Aile",
        "$mainUrl/dizi/tur/aksiyon-macera" to "Aksiyon & Macera",
        "$mainUrl/dizi/tur/animasyon" to "Animasyon",
        "$mainUrl/dizi/tur/belgesel" to "Belgesel",
        "$mainUrl/dizi/tur/bilim-kurgu-fantazi" to "Bilim Kurgu & Fantazi",
        "$mainUrl/dizi/tur/dram" to "Dram",
        "$mainUrl/dizi/tur/gizem" to "Gizem",
        "$mainUrl/dizi/tur/komedi" to "Komedi",
        "$mainUrl/dizi/tur/savas-politik" to "Savaş & Politik",
        "$mainUrl/dizi/tur/suc" to "Suç",
        
        "$mainUrl/film/tur/aile" to "🎬 Aile Filmleri",
        "$mainUrl/film/tur/animasyon" to "🎬 Animasyon Filmleri",
        "$mainUrl/film/tur/bilim-kurgu" to "🎬 Bilim Kurgu Filmleri",
        "$mainUrl/film/tur/dram" to "🎬 Dram Filmleri",
        "$mainUrl/film/tur/fantastik" to "🎬 Fantastik Filmleri",
        "$mainUrl/film/tur/gerilim" to "🎬 Gerilim Filmleri",
        "$mainUrl/film/tur/gizem" to "🎬 Gizem Filmleri",
        "$mainUrl/film/tur/komedi" to "🎬 Komedi Filmleri",
        "$mainUrl/film/tur/korku" to "🎬 Korku Filmleri",
        "$mainUrl/film/tur/macera" to "🎬 Macera Filmleri",
        "$mainUrl/film/tur/romantik" to "🎬 Romantik Filmleri",
        "$mainUrl/film/tur/savas" to "🎬 Savaş Filmleri",
        "$mainUrl/film/tur/suc" to "🎬 Suç Filmleri",
        "$mainUrl/film/tur/tarih" to "🎬 Tarih Filmleri",
        "$mainUrl/film/tur/vahsi-bati" to "🎬 Vahşi Batı Filmleri"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data else "${request.data}/$page"
        
        val response = app.get(url, headers = getHeaders(), referer = "$mainUrl/")
        
        if (!response.isSuccessful) {
            throw ErrorLoadingException("Cloudflare koruması veya erişim hatası: ${response.code}")
        }

        val document = response.document
        val home = document.select("div.poster-long").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home, hasNext = home.isNotEmpty())
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val titleElement = this.selectFirst("div.poster-long-subject h2") 
            ?: this.selectFirst("h2.truncate")
            ?: return null
        
        val title = titleElement.text().trim()
        
        val href = fixUrlNull(
            this.selectFirst("div.poster-long-subject a")?.attr("href")
                ?: this.selectFirst("a")?.attr("href")
        ) ?: return null

        val posterUrl = fixUrlNull(
            this.selectFirst("div.poster-long-image img")?.attr("data-src")
                ?: this.selectFirst("img")?.attr("data-src")
                ?: this.selectFirst("img")?.attr("src")
        )

        val score = this.selectFirst("span.rating")?.text()?.trim()
            ?: this.selectFirst("span.color-imdb")?.text()?.trim()

        val isTvSeries = href.contains("/dizi/") || !href.contains("/film/")

        return if (isTvSeries) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
                this.score = Score.from10(score)
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
                this.score = Score.from10(score)
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchHeaders = getHeaders().plus(mapOf(
            "X-Requested-With" to "XMLHttpRequest",
            "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
            "Accept" to "application/json, text/javascript, */*; q=0.01",
            "Origin" to mainUrl
        ))

        val response = app.post(
            "$mainUrl/search",
            data = mapOf("query" to query),
            headers = searchHeaders,
            referer = "$mainUrl/"
        )

        if (!response.isSuccessful) {
            Log.e("DiziMag", "Search failed: ${response.code}")
            return emptyList()
        }

        return try {
            val searchResult = response.parsedSafe<SearchResult>()
            val html = searchResult?.theme ?: return emptyList()
            
            val document = Jsoup.parse(html)
            document.select("ul li").mapNotNull { element ->
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                if (href.contains("/dizi/") || href.contains("/film/")) {
                    element.toPostSearchResult()
                } else null
            }
        } catch (e: Exception) {
            Log.e("DiziMag", "Search parse error: ${e.message}")
            emptyList()
        }
    }

    private fun Element.toPostSearchResult(): SearchResponse? {
        val title = this.selectFirst("span")?.text()?.trim()
            ?: this.selectFirst("h4")?.text()?.trim()
            ?: return null
            
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return if (href.contains("/dizi/")) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val response = app.get(url, headers = getHeaders(), referer = "$mainUrl/")
        
        if (!response.isSuccessful) {
            throw ErrorLoadingException("İçerik yüklenemedi: ${response.code}")
        }

        val document = response.document
        
        // Temel bilgiler
        val title = document.selectFirst("div.page-title h1 a")?.text()?.trim()
            ?: document.selectFirst("div.page-title h1")?.text()?.trim()
            ?: document.selectFirst("h1")?.text()?.trim()
            ?: return null

        val originalTitle = document.selectFirst("div.page-title p")?.text()?.trim() ?: ""
        val displayTitle = if (originalTitle.isNotBlank() && !title.contains(originalTitle, true)) {
            "$title - $originalTitle"
        } else {
            title
        }

        val poster = fixUrlNull(
            document.selectFirst("div.series-profile-image img")?.attr("src")
                ?: document.selectFirst("div.poster img")?.attr("src")
                ?: document.selectFirst("meta[property=og:image]")?.attr("content")
        )

        val year = document.selectFirst("h1 span")?.text()
            ?.substringAfter("(")?.substringBefore(")")
            ?.toIntOrNull()
            ?: document.selectFirst("div.series-profile-info a[href*=yil]")?.text()?.toIntOrNull()

        val rating = document.selectFirst("span.color-imdb")?.text()?.trim()
            ?: document.selectFirst("div.imdb span")?.text()?.trim()

        val duration = document.selectXpath("//span[text()='Süre']/following-sibling::p")
            .text()
            .trim()
            .split(" ")
            .firstOrNull()
            ?.toIntOrNull()
            ?: document.selectFirst("span[property=duration]")?.text()?.toIntOrNull()

        val description = document.selectFirst("div.series-profile-summary p")?.text()?.trim()
            ?: document.selectFirst("div.summary p")?.text()?.trim()
            ?: document.selectFirst("meta[property=og:description]")?.attr("content")

        val tags = document.selectFirst("div.series-profile-type")?.select("a")
            ?.mapNotNull { it.text().trim().takeIf { t -> t.isNotBlank() } }
            ?: document.select("div.genres a").mapNotNull { it.text().trim() }

        // Trailer
        val trailer = document.selectFirst("div.series-profile-trailer")?.attr("data-yt")
            ?: document.selectFirst("button[data-yt]")?.attr("data-yt")
        
        val trailerUrl = trailer?.let { "https://www.youtube.com/embed/$it" }

        // Oyuncular
        val actors = document.select("div.series-profile-cast li").mapNotNull { element ->
            val img = fixUrlNull(element.selectFirst("img")?.attr("data-src"))
            val name = element.selectFirst("h5.truncate")?.text()?.trim()
                ?: element.selectFirst("span.name")?.text()?.trim()
                ?: return@mapNotNull null
            Actor(name, img)
        }

        // Bölümleri veya film kaynağını al
        return if (url.contains("/dizi/") || document.select("div.series-profile-episode-list").isNotEmpty()) {
            loadTvSeries(document, displayTitle, url, poster, year, description, tags, rating, actors, trailerUrl)
        } else {
            loadMovie(document, title, url, poster, year, description, tags, rating, duration, actors, trailerUrl)
        }
    }

    private fun loadTvSeries(
        document: Element,
        title: String,
        url: String,
        poster: String?,
        year: Int?,
        description: String?,
        tags: List<String>?,
        rating: String?,
        actors: List<Actor>,
        trailer: String?
    ): LoadResponse {
        val episodes = mutableListOf<Episode>()
        
        document.select("div.series-profile-episode-list").forEachIndexed { seasonIndex, seasonElement ->
            val seasonNumber = seasonIndex + 1
            
            seasonElement.select("li").forEachIndexed { episodeIndex, episodeElement ->
                val epName = episodeElement.selectFirst("h6.truncate a")?.text()?.trim()
                    ?: episodeElement.selectFirst("a")?.text()?.trim()
                    ?: "Bölüm ${episodeIndex + 1}"
                    
                val epHref = fixUrlNull(
                    episodeElement.selectFirst("h6.truncate a")?.attr("href")
                        ?: episodeElement.selectFirst("a")?.attr("href")
                ) ?: return@forEachIndexed

                episodes.add(
                    newEpisode(epHref) {
                        this.name = epName
                        this.season = seasonNumber
                        this.episode = episodeIndex + 1
                    }
                )
            }
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.year = year
            this.plot = description
            this.tags = tags
            this.score = Score.from10(rating)
            addActors(actors)
            trailer?.let { addTrailer(it) }
        }
    }

    private fun loadMovie(
        document: Element,
        title: String,
        url: String,
        poster: String?,
        year: Int?,
        description: String?,
        tags: List<String>?,
        rating: String?,
        duration: Int?,
        actors: List<Actor>,
        trailer: String?
    ): LoadResponse {
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.year = year
            this.plot = description
            this.tags = tags
            this.score = Score.from10(rating)
            this.duration = duration
            addActors(actors)
            trailer?.let { addTrailer(it) }
        }
    }

    // CryptoJS AES decryption için
    private fun decryptAES(password: String, cipherText: String, iv: String, salt: String?): String {
        return try {
            val key = generateKey(password, salt?.hexToBytes())
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val ivSpec = IvParameterSpec(iv.hexToBytes())
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), ivSpec)
            
            val encrypted = Base64.getDecoder().decode(cipherText)
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("DiziMag", "AES decrypt error: ${e.message}")
            throw e
        }
    }

    private fun generateKey(password: String, salt: ByteArray?): ByteArray {
        // OpenSSL EVP_BytesToKey compatible key derivation
        val md = java.security.MessageDigest.getInstance("MD5")
        val key = md.digest(password.toByteArray(Charsets.UTF_8) + (salt ?: byteArrayOf()))
        return key.copyOf(16) // 128-bit key
    }

    private fun String.hexToBytes(): ByteArray {
        val len = this.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(this[i], 16) shl 4) + 
                          Character.digit(this[i + 1], 16)).toByte()
        }
        return data
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val headers = getHeaders().plus(mapOf(
            "Referer" to "$mainUrl/",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        ))

        // Session cookie al
        val mainPage = app.get(mainUrl, headers = headers)
        val ciSession = mainPage.cookies["ci_session"] ?: ""

        val document = app.get(
            data,
            headers = headers,
            cookies = mapOf("ci_session" to ciSession),
            referer = "$mainUrl/"
        ).document

        // iframe bul
        val iframeSrc = fixUrlNull(
            document.selectFirst("div#tv-spoox2 iframe")?.attr("src")
                ?: document.selectFirst("iframe[src*=player]")?.attr("src")
                ?: document.selectFirst("iframe")?.attr("src")
        ) ?: run {
            Log.e("DiziMag", "Iframe not found in: $data")
            return false
        }

        Log.d("DiziMag", "Found iframe: $iframeSrc")

        // Player sayfasını al
        val playerResponse = app.get(iframeSrc, headers = headers, referer = "$mainUrl/")
        val playerDoc = playerResponse.document

        // bePlayer şifrelemesini çöz
        playerDoc.select("script").forEach { script ->
            val scriptContent = script.data() ?: script.html()
            
            if (scriptContent.contains("bePlayer")) {
                Log.d("DiziMag", "Found bePlayer script")
                
                val pattern = Pattern.compile("""bePlayer\s*\(\s*['"]([^'"]+)['"]\s*,\s*['"](\{[^}]*\})['"]\s*\)""")
                val matcher = pattern.matcher(scriptContent)
                
                if (matcher.find()) {
                    val key = matcher.group(1)
                    val jsonCipher = matcher.group(2)
                    
                    Log.d("DiziMag", "Decrypting with key: $key")
                    
                    try {
                        val cipherData = parseJson<CipherData>(jsonCipher.replace("\\/", "/"))
                        
                        val decrypted = decryptAES(
                            key,
                            cipherData.ct,
                            cipherData.iv,
                            cipherData.s
                        )
                        
                        Log.d("DiziMag", "Decrypted: ${decrypted.take(200)}...")
                        
                        val jsonData = parseJson<PlayerData>(decrypted)
                        
                        // Altyazıları ekle
                        jsonData.strSubtitles?.forEach { sub ->
                            subtitleCallback.invoke(
                                SubtitleFile(
                                    lang = sub.label ?: "Unknown",
                                    url = fixUrl(sub.file)
                                )
                            )
                        }
                        
                        // Video kaynağını ekle
                        callback.invoke(
                            newExtractorLink(
                                source = this.name,
                                name = "${this.name} ${jsonData.videoType?.uppercase() ?: "HD"}",
                                url = jsonData.videoLocation,
                                type = ExtractorLinkType.M3U8
                            ) {
                                this.headers = mapOf(
                                    "Accept" to "*/*",
                                    "Accept-Language" to "tr-TR,tr;q=0.9",
                                    "Origin" to "https://epikplayer.xyz",
                                    "Referer" to iframeSrc
                                )
                                this.referer = iframeSrc
                                this.quality = Qualities.Unknown.value
                            }
                        )
                        
                        return true
                        
                    } catch (e: Exception) {
                        Log.e("DiziMag", "Decryption failed: ${e.message}")
                    }
                }
            }
        }

        // Yedek: Doğrudan extractor dene
        return loadExtractor(iframeSrc, "$mainUrl/", subtitleCallback, callback)
    }

    // Data classes
    data class SearchResult(
        @JsonProperty("success") val success: Boolean?,
        @JsonProperty("theme") val theme: String?
    )

    data class CipherData(
        @JsonProperty("ct") val ct: String,
        @JsonProperty("iv") val iv: String,
        @JsonProperty("s") val s: String?
    )

    data class PlayerData(
        @JsonProperty("videoLocation") val videoLocation: String,
        @JsonProperty("videoType") val videoType: String?,
        @JsonProperty("strSubtitles") val strSubtitles: List<SubtitleData>?
    )

    data class SubtitleData(
        @JsonProperty("file") val file: String,
        @JsonProperty("label") val label: String?,
        @JsonProperty("kind") val kind: String?
    )
}
