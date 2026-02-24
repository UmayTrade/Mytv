package com.nikyokki

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.WebViewResolver
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import org.jsoup.nodes.Element
import java.util.Base64
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class DiziMag : MainAPI() {
    override var mainUrl = "https://dizimag.eu"
    override var name = "DiziMag"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 2000L
    override var sequentialMainPageScrollDelay = 2000L

    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
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
            "Cache-Control" to "no-cache"
        )
    }

    // WebView kullanarak Cloudflare bypass
    private suspend fun bypassCloudflare(url: String): String {
        Log.d("DiziMag", "WebView bypass: $url")
        
        // WebViewResolver - operator invoke kullan
        val webViewResult = WebViewResolver(
            interceptUrl = Regex(".*"),
            timeout = 30000
        )(
            url = url,
            headers = getHeaders()
        )
        
        return webViewResult?.second ?: throw ErrorLoadingException("WebView failed")
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
        
        val url = when {
            page == 1 -> baseUrl
            baseUrl.contains("?") -> "$baseUrl&page=$page"
            else -> "$baseUrl/$page"
        }
        
        Log.d("DiziMag", "getMainPage: $url")

        return try {
            // Önce normal istek dene
            val response = app.get(url, headers = getHeaders(), referer = "$mainUrl/", timeout = 30)
            
            if (response.code == 200 && !response.text.contains("cf-browser-verification")) {
                parseMainPage(response.document, request.name)
            } else {
                throw ErrorLoadingException("Cloudflare detected")
            }
        } catch (e: Exception) {
            Log.d("DiziMag", "Using WebView bypass: ${e.message}")
            val html = bypassCloudflare(url)
            val doc = org.jsoup.Jsoup.parse(html)
            parseMainPage(doc, request.name)
        }
    }

    private fun parseMainPage(document: org.jsoup.nodes.Document, name: String): HomePageResponse {
        var items = document.select("div.poster-long")
        Log.d("DiziMag", "Found ${items.size} items")
        
        if (items.isEmpty()) {
            items = document.select("div.poster, .movie-item, .series-item")
            Log.d("DiziMag", "Alt selector found ${items.size}")
        }

        val home = items.mapNotNull { it.toSearchResult() }
        
        return newHomePageResponse(name, home, hasNext = home.isNotEmpty())
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2, h3, .title")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        val isTvSeries = !href.contains("/film/")

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

    override suspend fun search(query: String): List<SearchResponse> {
        return try {
            val searchHeaders = getHeaders().plus(mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "Content-Type" to "application/x-www-form-urlencoded",
                "Accept" to "application/json, text/javascript, */*; q=0.01",
                "Origin" to mainUrl
            ))

            val response = app.post(
                url = "$mainUrl/search",
                data = mapOf("query" to query),
                headers = searchHeaders,
                referer = "$mainUrl/"
            )

            if (!response.isSuccessful) {
                Log.e("DiziMag", "Search failed: ${response.code}")
                return emptyList()
            }

            val searchResult = response.parsedSafe<SearchResult>()
            val html = searchResult?.theme ?: return emptyList()
            
            val doc = org.jsoup.Jsoup.parse(html)
            doc.select("ul li").mapNotNull { element ->
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                if (href.contains("/dizi/") || href.contains("/film/")) {
                    element.toPostSearchResult()
                } else null
            }
        } catch (e: Exception) {
            Log.e("DiziMag", "Search error: ${e.message}")
            emptyList()
        }
    }

    private fun Element.toPostSearchResult(): SearchResponse? {
        val title = this.selectFirst("span, h4")?.text()?.trim() ?: return null
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
        val document = try {
            val response = app.get(url, headers = getHeaders(), referer = "$mainUrl/", timeout = 30)
            
            if (response.code == 200 && !response.text.contains("cf-browser-verification")) {
                response.document
            } else {
                throw ErrorLoadingException("Cloudflare detected")
            }
        } catch (e: Exception) {
            Log.d("DiziMag", "Load using WebView: $url")
            val html = bypassCloudflare(url)
            org.jsoup.Jsoup.parse(html)
        }
        
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
                ?: document.selectFirst("meta[property=og:image]")?.attr("content")
        )

        val year = document.selectFirst("h1 span")?.text()
            ?.substringAfter("(")?.substringBefore(")")
            ?.toIntOrNull()

        val rating = document.selectFirst("span.color-imdb")?.text()?.trim()

        val duration = document.selectXpath("//span[text()='Süre']/following-sibling::p")
            .text().trim().split(" ").firstOrNull()?.toIntOrNull()

        val description = document.selectFirst("div.series-profile-summary p")?.text()?.trim()
            ?: document.selectFirst("meta[property=og:description]")?.attr("content")

        val tags = document.selectFirst("div.series-profile-type")?.select("a")
            ?.mapNotNull { it.text().trim().takeIf { t -> t.isNotBlank() } }

        val trailer = document.selectFirst("div.series-profile-trailer")?.attr("data-yt")
            ?: document.selectFirst("button[data-yt]")?.attr("data-yt")
        
        val trailerUrl = trailer?.let { "https://www.youtube.com/embed/$it" }

        val actors = document.select("div.series-profile-cast li").mapNotNull { element ->
            val img = fixUrlNull(element.selectFirst("img")?.attr("data-src"))
            val name = element.selectFirst("h5.truncate")?.text()?.trim() ?: return@mapNotNull null
            Actor(name, img)
        }

        val isTvSeries = url.contains("/dizi/") || 
                        document.select("div.series-profile-episode-list").isNotEmpty()

        return if (isTvSeries) {
            val episodes = mutableListOf<Episode>()
            
            document.select("div.series-profile-episode-list").forEachIndexed { seasonIndex, seasonElement ->
                val seasonNumber = seasonIndex + 1
                
                seasonElement.select("li").forEachIndexed { episodeIndex, episodeElement ->
                    val epName = episodeElement.selectFirst("h6.truncate a")?.text()?.trim()
                        ?: "Bölüm ${episodeIndex + 1}"
                        
                    val epHref = fixUrlNull(
                        episodeElement.selectFirst("h6.truncate a")?.attr("href")
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

            newTvSeriesLoadResponse(displayTitle, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.score = Score.from10(rating)
                addActors(actors)
                trailerUrl?.let { addTrailer(it) }
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.score = Score.from10(rating)
                this.duration = duration
                addActors(actors)
                trailerUrl?.let { addTrailer(it) }
            }
        }
    }

    private fun decryptAES(password: String, cipherText: String, iv: String, salt: String?): String {
        return try {
            val key = generateKey(password, salt?.hexToBytes())
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val ivSpec = IvParameterSpec(iv.hexToBytes())
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), ivSpec)
            
            val encrypted = Base64.getDecoder().decode(cipherText)
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("DiziMag", "AES error: ${e.message}")
            throw e
        }
    }

    private fun generateKey(password: String, salt: ByteArray?): ByteArray {
        val md = java.security.MessageDigest.getInstance("MD5")
        val key = md.digest(password.toByteArray(Charsets.UTF_8) + (salt ?: byteArrayOf()))
        return key.copyOf(16)
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
        // Session cookie al
        val mainPageHtml = try {
            bypassCloudflare(mainUrl)
        } catch (e: Exception) {
            app.get(mainUrl, headers = getHeaders()).text
        }
        
        val ciSession = Regex("""ci_session=([^;]+)""").find(mainPageHtml)?.groupValues?.get(1) ?: ""

        val document = try {
            app.get(
                url = data,
                headers = getHeaders(),
                cookies = mapOf("ci_session" to ciSession),
                referer = "$mainUrl/"
            ).document
        } catch (e: Exception) {
            val html = bypassCloudflare(data)
            org.jsoup.Jsoup.parse(html)
        }

        val iframeSrc = fixUrlNull(
            document.selectFirst("div#tv-spoox2 iframe")?.attr("src")
                ?: document.selectFirst("iframe[src*=player]")?.attr("src")
                ?: document.selectFirst("iframe")?.attr("src")
        ) ?: run {
            Log.e("DiziMag", "Iframe not found")
            return false
        }

        Log.d("DiziMag", "Iframe: $iframeSrc")

        val playerDoc = try {
            app.get(iframeSrc, headers = getHeaders(), referer = "$mainUrl/").document
        } catch (e: Exception) {
            val html = bypassCloudflare(iframeSrc)
            org.jsoup.Jsoup.parse(html)
        }

        playerDoc.select("script").forEach { script ->
            val scriptContent = script.data() ?: script.html()
            
            if (scriptContent.contains("bePlayer")) {
                val pattern = Pattern.compile("""bePlayer\s*\(\s*['"]([^'"]+)['"]\s*,\s*['"](\{[^}]*\})['"]\s*\)""")
                val matcher = pattern.matcher(scriptContent)
                
                if (matcher.find()) {
                    val key = matcher.group(1)
                    val jsonCipher = matcher.group(2)
                    
                    try {
                        val cipherData = parseJson<CipherData>(jsonCipher.replace("\\/", "/"))
                        val decrypted = decryptAES(key, cipherData.ct, cipherData.iv, cipherData.s)
                        val jsonData = parseJson<PlayerData>(decrypted)
                        
                        jsonData.strSubtitles?.forEach { sub ->
                            subtitleCallback.invoke(
                                SubtitleFile(
                                    lang = sub.label ?: "Unknown",
                                    url = fixUrl(sub.file)
                                )
                            )
                        }
                        
                        callback.invoke(
                            newExtractorLink(
                                source = this.name,
                                name = "${this.name} ${jsonData.videoType?.uppercase() ?: "HD"}",
                                url = jsonData.videoLocation,
                                type = ExtractorLinkType.M3U8
                            ) {
                                this.headers = mapOf(
                                    "Accept" to "*/*",
                                    "Referer" to iframeSrc
                                )
                                this.referer = iframeSrc
                                this.quality = Qualities.Unknown.value
                            }
                        )
                        
                        return true
                    } catch (e: Exception) {
                        Log.e("DiziMag", "Decrypt failed: ${e.message}")
                    }
                }
            }
        }

        return loadExtractor(iframeSrc, "$mainUrl/", subtitleCallback, callback)
    }

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
