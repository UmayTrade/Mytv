// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.Interceptor
import java.util.Base64

class RecTV : MainAPI() {
    override var mainUrl              = "https://m.prectv51.sbs" // mainUrl güncellendi
    override var name                 = "RecTV"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie, TvType.Live, TvType.TvSeries)

    private val swKey = "4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452"

    private var currentToken: String? = null
    private var tokenExpirationTime: Long = 0L 
    
    private val AUTH_URL = "${mainUrl}/api/attest/nonce"

    private suspend fun getValidToken(): String {
        val currentTime = System.currentTimeMillis()
        // Token yoksa veya süresi bitmeye yakınsa (30 saniye kala) yenile
        if (currentToken == null || tokenExpirationTime < (currentTime + 30000L)) {
            refreshToken()
        }
        return currentToken ?: throw IllegalStateException("Token yenilenemedi.")
    }

    private suspend fun refreshToken() {
        Log.d(name, "Token yenileniyor...")
        val response = app.get(AUTH_URL, headers = mapOf("User-Agent" to "googleusercontent"))

        if (response.isSuccessful) {
            val authResponse = try {
                jacksonObjectMapper().readValue<AuthResponse>(response.text)
            } catch (_: Exception) {
                AuthResponse(accessToken = response.text.trim()) 
            }

            currentToken = authResponse.accessToken
            val expirationSeconds = authResponse.expiresIn
            
            if (expirationSeconds != null) {
                tokenExpirationTime = System.currentTimeMillis() + (expirationSeconds * 1000L)
            } else {
                try {
                    val parts = currentToken!!.split(".")
                    if (parts.size == 3) {
                        val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))
                        val jwtPayload = jacksonObjectMapper().readValue<JWTPayload>(payloadJson)
                        tokenExpirationTime = jwtPayload.expiration * 1000L
                    } else {
                        tokenExpirationTime = System.currentTimeMillis() + (60 * 60 * 1000L) 
                    }
                } catch (e: Exception) {
                    tokenExpirationTime = System.currentTimeMillis() + (60 * 60 * 1000L) 
                }
            }
        }
    }

    override val mainPage = mainPageOf(
        "${mainUrl}/api/channel/by/filtres/0/0/SAYFA/${swKey}/"      to "Canlı",
        "${mainUrl}/api/movie/by/filtres/0/created/SAYFA/${swKey}/"  to "Son Filmler",
        "${mainUrl}/api/serie/by/filtres/0/created/SAYFA/${swKey}/"  to "Son Diziler",
        "${mainUrl}/api/movie/by/filtres/14/created/SAYFA/${swKey}/" to "Aile",
        "${mainUrl}/api/movie/by/filtres/1/created/SAYFA/${swKey}/"  to "Aksiyon",
        "${mainUrl}/api/movie/by/filtres/13/created/SAYFA/${swKey}/" to "Animasyon",
        "${mainUrl}/api/movie/by/filtres/19/created/SAYFA/${swKey}/" to "Belgesel",
        "${mainUrl}/api/movie/by/filtres/4/created/SAYFA/${swKey}/"  to "Bilim Kurgu",
        "${mainUrl}/api/movie/by/filtres/2/created/SAYFA/${swKey}/"  to "Dram",
        "${mainUrl}/api/movie/by/filtres/10/created/SAYFA/${swKey}/" to "Fantastik",
        "${mainUrl}/api/movie/by/filtres/3/created/SAYFA/${swKey}/"  to "Komedi",
        "${mainUrl}/api/movie/by/filtres/8/created/SAYFA/${swKey}/"  to "Korku",
        "${mainUrl}/api/movie/by/filtres/17/created/SAYFA/${swKey}/" to "Macera",
        "${mainUrl}/api/movie/by/filtres/5/created/SAYFA/${swKey}/"  to "Romantik"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val p = page - 1
        val validToken = getValidToken()
        val url = request.data.replace("SAYFA", "$p")
        
        val home = app.get(url, headers = mapOf(
            "User-Agent" to "googleusercontent", 
            "Referer" to "https://twitter.com/", 
            "authorization" to "Bearer $validToken"
        )).text

        val items = AppUtils.tryParseJson<List<RecItem>>(home) ?: return newHomePageResponse(request.name, emptyList())

        val movies = items.map { item ->
            val toDict = jacksonObjectMapper().writeValueAsString(item)
            val isLive = item.label?.contains("CANLI", ignoreCase = true) == true

            if (isLive) {
                newLiveSearchResponse(item.title, toDict, TvType.Live) { this.posterUrl = item.image }
            } else {
                newMovieSearchResponse(item.title, toDict, TvType.Movie) { this.posterUrl = item.image }
            }
        }

        return newHomePageResponse(request.name, movies)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val home = app.get("${mainUrl}/api/search/${query}/${swKey}/", headers = mapOf("User-Agent" to "okhttp/4.12.0")).text
        val veriler = AppUtils.tryParseJson<RecSearch>(home) ?: return emptyList()

        val sonuclar = mutableListOf<SearchResponse>()
        
        (veriler.channels.orEmpty() + veriler.posters.orEmpty()).forEach { item ->
            val drama = jacksonObjectMapper().writeValueAsString(item)
            sonuclar.add(newMovieSearchResponse(item.title, drama, TvType.Movie) { this.posterUrl = item.image })
        }

        return sonuclar
    }

    override suspend fun load(url: String): LoadResponse? {
        val veri = AppUtils.tryParseJson<RecItem>(url) ?: return null

        if (veri.type == "serie") {
            val diziReq = app.get("${mainUrl}/api/season/by/serie/${veri.id}/${swKey}/", headers = mapOf("User-Agent" to "okhttp/4.12.0")).text
            val sezonlar = AppUtils.tryParseJson<List<RecDizi>>(diziReq) ?: return null
            val episodesList = mutableListOf<Episode>()
            val numberRegex = Regex("\\d+")

            for (sezon in sezonlar) {
                for (bolum in sezon.episodes) {
                    episodesList.add(newEpisode(bolum.sources.firstOrNull()?.url ?: "") {
                        this.name = bolum.title
                        this.season = numberRegex.find(sezon.title)?.value?.toIntOrNull()
                        this.episode = numberRegex.find(bolum.title)?.value?.toIntOrNull()
                        this.posterUrl = veri.image
                    })
                }
            }

            return newTvSeriesLoadResponse(veri.title, url, TvType.TvSeries, episodesList) {
                this.posterUrl = veri.image
                this.plot = veri.description
                this.year = veri.year
                this.tags = veri.genres?.map { it.title }
            }
        }

        val isLive = veri.label?.contains("CANLI", ignoreCase = true) == true
        return if (isLive) {
            newLiveStreamLoadResponse(veri.title, url, url) {
                this.posterUrl = veri.image
                this.plot = veri.description
            }
        } else {
            newMovieLoadResponse(veri.title, url, TvType.Movie, url) {
                this.posterUrl = veri.image
                this.plot = veri.description
                this.year = veri.year
                this.tags = veri.genres?.map { it.title }
            }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        if (data.startsWith("http")) {
            callback.invoke(newExtractorLink(this.name, this.name, data, INFER_TYPE) {
                this.referer = "https://twitter.com/"
                this.quality = Qualities.Unknown.value
            })
            return true
        }

        val veri = AppUtils.tryParseJson<RecItem>(data) ?: return false
        veri.sources.forEach { source ->
            callback.invoke(newExtractorLink(this.name, "${this.name} - ${source.type}", source.url, if (source.type == "mp4") ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8) {
                this.referer = "https://twitter.com/"
                this.quality = Qualities.Unknown.value
            })
        }
        return true
    }

    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return Interceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .removeHeader("If-None-Match")
                .header("User-Agent", "googleusercontent")
                .build())
        }
    }
}
