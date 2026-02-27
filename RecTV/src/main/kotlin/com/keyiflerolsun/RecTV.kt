package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.Interceptor
import java.util.Base64

// ----------------- UTILS -----------------
private val Int.toMillis: Long
    get() = this * 1000L

class RecTV : MainAPI() {
    override var mainUrl = "https://rectv13.cloudflareaccess.com"
    override var name = "RecTV"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Movie, TvType.Live, TvType.TvSeries)

    private val swKey = "4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452"
    private var currentToken: String? = null
    private var tokenExpirationTime: Long = 0L
    private val AUTH_URL = "$mainUrl/api/attest/nonce"

    // ---------------- TOKEN ----------------
    private suspend fun getValidToken(): String {
        val now = System.currentTimeMillis()
        if (currentToken == null || tokenExpirationTime < now + 30.toMillis) {
            refreshToken()
        }
        return currentToken ?: throw IllegalStateException("Token alınamadı")
    }

    private suspend fun refreshToken() {
        Log.d(name, "Token yenileniyor...")
        val response = app.get(AUTH_URL, headers = mapOf("User-Agent" to "googleusercontent"))

        if (!response.isSuccessful) {
            throw Exception("Token alınamadı: ${response.text}")
        }

        val authResponse = try {
            jacksonObjectMapper().readValue<AuthResponse>(response.text)
        } catch (_: Exception) {
            AuthResponse(accessToken = response.text.trim())
        }

        currentToken = authResponse.accessToken

        tokenExpirationTime = authResponse.expiresIn?.let { System.currentTimeMillis() + it.toMillis } ?: run {
            currentToken?.let { token ->
                try {
                    val payload = token.split(".")[1]
                    val json = String(Base64.getUrlDecoder().decode(payload))
                    val jwtPayload = jacksonObjectMapper().readValue<JWTPayload>(json)
                    jwtPayload.expiration * 1000L
                } catch (_: Exception) {
                    System.currentTimeMillis() + 60.toMillis
                }
            } ?: System.currentTimeMillis() + 60.toMillis
        }

        Log.d(name, "Token yenilendi. Geçerlilik: $tokenExpirationTime")
    }

    // ---------------- ANA SAYFA ----------------
    override val mainPage = mainPageOf(
        "${mainUrl}/api/channel/by/filtres/0/0/SAYFA/$swKey/" to "Canlı",
        "${mainUrl}/api/movie/by/filtres/0/created/SAYFA/$swKey/" to "Son Filmler",
        "${mainUrl}/api/serie/by/filtres/0/created/SAYFA/$swKey/" to "Son Diziler",
        "${mainUrl}/api/movie/by/filtres/14/created/SAYFA/$swKey/" to "Aile",
        "${mainUrl}/api/movie/by/filtres/1/created/SAYFA/$swKey/" to "Aksiyon",
        "${mainUrl}/api/movie/by/filtres/13/created/SAYFA/$swKey/" to "Animasyon",
        "${mainUrl}/api/movie/by/filtres/19/created/SAYFA/$swKey/" to "Belgesel",
        "${mainUrl}/api/movie/by/filtres/4/created/SAYFA/$swKey/" to "Bilim Kurgu",
        "${mainUrl}/api/movie/by/filtres/2/created/SAYFA/$swKey/" to "Dram",
        "${mainUrl}/api/movie/by/filtres/10/created/SAYFA/$swKey/" to "Fantastik",
        "${mainUrl}/api/movie/by/filtres/3/created/SAYFA/$swKey/" to "Komedi",
        "${mainUrl}/api/movie/by/filtres/8/created/SAYFA/$swKey/" to "Korku",
        "${mainUrl}/api/movie/by/filtres/17/created/SAYFA/$swKey/" to "Macera",
        "${mainUrl}/api/movie/by/filtres/5/created/SAYFA/$swKey/" to "Romantik"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageNum = page - 1
        val validToken = getValidToken()
        val url = request.data.replace("SAYFA", "$pageNum")

        val res = app.get(url, headers = mapOf(
            "User-Agent" to "googleusercontent",
            "Referer" to "https://twitter.com/",
            "Authorization" to "Bearer $validToken"
        ))

        val items = AppUtils.tryParseJson<List<RecItem>>(res.text) ?: emptyList()

        val responses = items.map { item ->
            val jsonStr = jacksonObjectMapper().writeValueAsString(item)
            if (item.label.equals("canlı", ignoreCase = true)) {
                newLiveSearchResponse(item.title, jsonStr, TvType.Live) { posterUrl = item.image }
            } else {
                newMovieSearchResponse(item.title, jsonStr, TvType.Movie) { posterUrl = item.image }
            }
        }

        return newHomePageResponse(request.name, responses)
    }

    // ---------------- ARAMA ----------------
    override suspend fun search(query: String): List<SearchResponse> {
        val res = app.get("$mainUrl/api/search/$query/$swKey/", headers = mapOf("User-Agent" to "okhttp/4.12.0"))
        val data = AppUtils.tryParseJson<RecSearch>(res.text) ?: return emptyList()

        val results = mutableListOf<SearchResponse>()
        data.channels?.forEach { ch ->
            val jsonStr = jacksonObjectMapper().writeValueAsString(ch)
            results.add(newMovieSearchResponse(ch.title, jsonStr, TvType.Movie) { posterUrl = ch.image })
        }
        data.posters?.forEach { mv ->
            val jsonStr = jacksonObjectMapper().writeValueAsString(mv)
            results.add(newMovieSearchResponse(mv.title, jsonStr, TvType.Movie) { posterUrl = mv.image })
        }

        return results
    }

    override suspend fun quickSearch(query: String) = search(query)

    // ---------------- YÜKLEME ----------------
    override suspend fun load(url: String): LoadResponse? {
        val item = AppUtils.tryParseJson<RecItem>(url) ?: return null

        if (item.type == "serie") {
            val seasonsRes = app.get("$mainUrl/api/season/by/serie/${item.id}/$swKey/", headers = mapOf("User-Agent" to "okhttp/4.12.0"))
            val seasons = AppUtils.tryParseJson<List<RecDizi>>(seasonsRes.text) ?: return null

            val episodesMap = mutableMapOf<DubStatus, MutableList<Episode>>()
            val numRegex = Regex("\\d+")

            seasons.forEach { season ->
                val status = when {
                    season.title.contains("altyazı", ignoreCase = true) -> DubStatus.Subbed
                    season.title.contains("dublaj", ignoreCase = true) -> DubStatus.Dubbed
                    else -> DubStatus.None
                }

                season.episodes.forEach { ep ->
                    episodesMap.getOrPut(status) { mutableListOf() }.add(newEpisode(ep.sources.first().url) {
                        name = ep.title
                        season = numRegex.find(season.title)?.value?.toIntOrNull()
                        episode = numRegex.find(ep.title)?.value?.toIntOrNull()
                        description = season.title.substringAfter(".S ")
                        posterUrl = item.image
                    })
                }
            }

            return newAnimeLoadResponse(name = item.title, url = url, type = TvType.TvSeries, comingSoonIfNone = false) {
                episodes = episodesMap.mapValues { it.value.toList() }.toMutableMap()
                posterUrl = item.image
                plot = item.description
                year = item.year
                tags = item.genres?.map { it.title }
            }
        }

        return if (item.label.equals("canlı", ignoreCase = true)) {
            newLiveStreamLoadResponse(item.title, url, url) {
                posterUrl = item.image
                plot = item.description
                tags = item.genres?.map { it.title }
            }
        } else {
            newMovieLoadResponse(item.title, url, TvType.Movie, url) {
                posterUrl = item.image
                plot = item.description
                year = item.year
                tags = item.genres?.map { it.title }
            }
        }
    }

    // ---------------- LİNKLER ----------------
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val item = AppUtils.tryParseJson<RecItem>(data)

        if (item == null && data.startsWith("http")) {
            callback(newExtractorLink(source = name, name = name, url = data, type = INFER_TYPE) {
                headers = mapOf("Referer" to "https://twitter.com/")
                quality = Qualities.Unknown.value
            })
            return true
        }

        item?.sources?.forEach { src ->
            callback(newExtractorLink(source = name, name = "$name - ${src.type}", url = src.url,
                type = if (src.type == "mp4") ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8) {
                referer = "https://twitter.com/"
                quality = Qualities.Unknown.value
            })
        }

        return true
    }

    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return Interceptor { chain ->
            val req = chain.request().newBuilder()
                .removeHeader("If-None-Match")
                .header("User-Agent", "googleusercontent")
                .build()
            chain.proceed(req)
        }
    }
}
