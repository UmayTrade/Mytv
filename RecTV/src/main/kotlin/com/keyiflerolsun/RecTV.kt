package com.keyiflerolsun

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import okhttp3.Interceptor

class RecTV : MainAPI() {
    override var mainUrl = "https://m.prectv51.sbs"
    override var name = "RecTV"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Movie, TvType.Live, TvType.TvSeries)

    private val swKey = "4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452"

    override val mainPage = mainPageOf(
        "${mainUrl}/api/channel/by/filtres/0/0/SAYFA/${swKey}/" to "Canlı",
        "${mainUrl}/api/movie/by/filtres/0/created/SAYFA/${swKey}/" to "Son Filmler",
        "${mainUrl}/api/serie/by/filtres/0/created/SAYFA/${swKey}/" to "Son Diziler",
        "${mainUrl}/api/movie/by/filtres/14/created/SAYFA/${swKey}/" to "Aile",
        "${mainUrl}/api/movie/by/filtres/1/created/SAYFA/${swKey}/" to "Aksiyon",
        "${mainUrl}/api/movie/by/filtres/13/created/SAYFA/${swKey}/" to "Animasyon",
        "${mainUrl}/api/movie/by/filtres/19/created/SAYFA/${swKey}/" to "Belgesel",
        "${mainUrl}/api/movie/by/filtres/4/created/SAYFA/${swKey}/" to "Bilim Kurgu",
        "${mainUrl}/api/movie/by/filtres/2/created/SAYFA/${swKey}/" to "Dram",
        "${mainUrl}/api/movie/by/filtres/10/created/SAYFA/${swKey}/" to "Fantastik",
        "${mainUrl}/api/movie/by/filtres/3/created/SAYFA/${swKey}/" to "Komedi",
        "${mainUrl}/api/movie/by/filtres/8/created/SAYFA/${swKey}/" to "Korku",
        "${mainUrl}/api/movie/by/filtres/17/created/SAYFA/${swKey}/" to "Macera",
        "${mainUrl}/api/movie/by/filtres/5/created/SAYFA/${swKey}/" to "Romantik"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val p = page - 1
        val url = request.data.replace("SAYFA", "$p")
        val home = app.get(url, headers = mapOf("User-Agent" to "okhttp/4.12.0")).text
        
        val items = AppUtils.tryParseJson<List<RecItem>>(home) ?: return newHomePageResponse(request.name, emptyList())

        val movies = items.map { item ->
            val toDict = jacksonObjectMapper().writeValueAsString(item)
            val isLive = item.label?.equals("CANLI", ignoreCase = true) == true

            if (isLive) {
                newLiveSearchResponse(item.title, toDict, TvType.Live) {
                    this.posterUrl = item.image
                    this.score = Score.from10(item.imdb)
                }
            } else {
                newMovieSearchResponse(item.title, toDict, TvType.Movie) {
                    this.posterUrl = item.image
                    this.score = Score.from10(item.imdb)
                }
            }
        }

        return newHomePageResponse(request.name, movies)
    }

    override suspend fun load(url: String): LoadResponse? {
        val veri = AppUtils.tryParseJson<RecItem>(url) ?: return null

        if (veri.type == "serie") {
            val diziReq = app.get("${mainUrl}/api/season/by/serie/${veri.id}/${swKey}/", headers = mapOf("User-Agent" to "okhttp/4.12.0")).text
            val sezonlar = AppUtils.tryParseJson<List<RecDizi>>(diziReq) ?: return null
            val episodes = mutableListOf<Episode>()

            sezonlar.forEach { sezon ->
                val sNum = Regex("\\d+").find(sezon.title)?.value?.toIntOrNull()
                sezon.episodes.forEach { bolum ->
                    episodes.add(newEpisode(bolum.sources.firstOrNull()?.url ?: "") {
                        this.name = bolum.title
                        this.season = sNum
                        this.episode = Regex("\\d+").find(bolum.title)?.value?.toIntOrNull()
                        this.posterUrl = veri.image
                    })
                }
            }

            return newTvSeriesLoadResponse(veri.title, url, TvType.TvSeries, episodes) {
                this.posterUrl = veri.image
                this.plot = veri.description
                this.year = veri.year
                this.tags = veri.genres?.map { it.title }
                this.score = Score.from10(veri.imdb)
            }
        }

        val isLive = veri.label?.equals("CANLI", ignoreCase = true) == true
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
                this.score = Score.from10(veri.imdb)
            }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        if (data.startsWith("http")) {
            callback.invoke(newExtractorLink(this.name, this.name, data, "https://twitter.com/", Qualities.Unknown.value, INFER_TYPE))
            return true
        }

        val veri = AppUtils.tryParseJson<RecItem>(data) ?: return false
        veri.sources.forEach { source ->
            callback.invoke(
                ExtractorLink(
                    this.name,
                    "${this.name} - ${source.type}",
                    source.url,
                    "https://twitter.com/",
                    Qualities.Unknown.value,
                    if (source.type == "mp4") ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8
                )
            )
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
