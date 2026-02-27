package com.keyiflerolsun

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import okhttp3.Interceptor

class RecTV : MainAPI() {
    override var mainUrl = "https://m.prectv51.sbs" [cite: 2]
    override var name = "RecTV" [cite: 2]
    override val hasMainPage = true [cite: 2]
    override var lang = "tr" [cite: 2]
    override val hasQuickSearch = false [cite: 2]
    override val supportedTypes = setOf(TvType.Movie, TvType.Live, TvType.TvSeries) [cite: 2]

    private val swKey = "4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452" [cite: 2]

    override val mainPage = mainPageOf(
        "${mainUrl}/api/channel/by/filtres/0/0/SAYFA/${swKey}/" to "Canlı", [cite: 2]
        "${mainUrl}/api/movie/by/filtres/0/created/SAYFA/${swKey}/" to "Son Filmler", [cite: 2]
        "${mainUrl}/api/serie/by/filtres/0/created/SAYFA/${swKey}/" to "Son Diziler", [cite: 2]
        "${mainUrl}/api/movie/by/filtres/14/created/SAYFA/${swKey}/" to "Aile", [cite: 2]
        "${mainUrl}/api/movie/by/filtres/1/created/SAYFA/${swKey}/" to "Aksiyon", [cite: 2]
        "${mainUrl}/api/movie/by/filtres/13/created/SAYFA/${swKey}/" to "Animasyon", [cite: 2]
        "${mainUrl}/api/movie/by/filtres/19/created/SAYFA/${swKey}/" to "Belgesel", [cite: 2]
        "${mainUrl}/api/movie/by/filtres/4/created/SAYFA/${swKey}/" to "Bilim Kurgu", [cite: 2]
        "${mainUrl}/api/movie/by/filtres/2/created/SAYFA/${swKey}/" to "Dram", [cite: 2]
        "${mainUrl}/api/movie/by/filtres/10/created/SAYFA/${swKey}/" to "Fantastik", [cite: 2]
        "${mainUrl}/api/movie/by/filtres/3/created/SAYFA/${swKey}/" to "Komedi", [cite: 2]
        "${mainUrl}/api/movie/by/filtres/8/created/SAYFA/${swKey}/" to "Korku", [cite: 2]
        "${mainUrl}/api/movie/by/filtres/17/created/SAYFA/${swKey}/" to "Macera", [cite: 2]
        "${mainUrl}/api/movie/by/filtres/5/created/SAYFA/${swKey}/" to "Romantik" [cite: 2]
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val p = page - 1 [cite: 2]
        val url = request.data.replace("SAYFA", "$p") [cite: 2]
        val home = app.get(url, headers = mapOf("User-Agent" to "okhttp/4.12.0")).text [cite: 2]
        
        val items = AppUtils.tryParseJson<List<RecItem>>(home) ?: return newHomePageResponse(request.name, emptyList()) [cite: 2]

        val movies = items.map { item ->
            val toDict = jacksonObjectMapper().writeValueAsString(item) [cite: 2]
            val isLive = item.label?.equals("CANLI", ignoreCase = true) == true [cite: 2]

            if (isLive) {
                newLiveSearchResponse(item.title, toDict, TvType.Live) {
                    this.posterUrl = item.image [cite: 2]
                    this.score = Score.from10(item.imdb) [cite: 2]
                }
            } else {
                newMovieSearchResponse(item.title, toDict, TvType.Movie) {
                    this.posterUrl = item.image [cite: 2]
                    this.score = Score.from10(item.imdb) [cite: 2]
                }
            }
        }

        return newHomePageResponse(request.name, movies) [cite: 2]
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val home = app.get(
            "${mainUrl}/api/search/${query}/${swKey}/", [cite: 2]
            headers = mapOf("User-Agent" to "okhttp/4.12.0") [cite: 2]
        ).text
        val veriler = AppUtils.tryParseJson<RecSearch>(home) [cite: 2]

        val sonuclar = mutableListOf<SearchResponse>()

        veriler?.channels?.let { channels ->
            for (item in channels) {
                val toDict = jacksonObjectMapper().writeValueAsString(item) [cite: 2]
                sonuclar.add(newMovieSearchResponse(item.title, toDict, TvType.Movie) {
                    this.posterUrl = item.image [cite: 2]
                    this.score = Score.from10(item.imdb) [cite: 2]
                })
            }
        }

        veriler?.posters?.let { posters ->
            for (item in posters) {
                val toDict = jacksonObjectMapper().writeValueAsString(item) [cite: 2]
                sonuclar.add(newMovieSearchResponse(item.title, toDict, TvType.Movie) { 
                    this.posterUrl = item.image [cite: 2]
                })
            }
        }

        return sonuclar [cite: 2]
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query) [cite: 2]

    override suspend fun load(url: String): LoadResponse? {
        val veri = AppUtils.tryParseJson<RecItem>(url) ?: return null [cite: 2]

        if (veri.type == "serie") { [cite: 2]
            val diziReq = app.get("${mainUrl}/api/season/by/serie/${veri.id}/${swKey}/", headers = mapOf("User-Agent" to "okhttp/4.12.0")).text [cite: 2]
            val sezonlar = AppUtils.tryParseJson<List<RecDizi>>(diziReq) ?: return null [cite: 2]
            val episodesList = mutableListOf<Episode>()

            val numberRegex = Regex("\\d+") [cite: 2]

            for (sezon in sezonlar) {
                for (bolum in sezon.episodes) {
                    episodesList.add(newEpisode(bolum.sources.firstOrNull()?.url ?: "") { [cite: 2]
                        this.name = bolum.title [cite: 2]
                        this.season = numberRegex.find(sezon.title)?.value?.toIntOrNull() [cite: 2]
                        this.episode = numberRegex.find(bolum.title)?.value?.toIntOrNull() [cite: 2]
                        this.posterUrl = veri.image [cite: 2]
                    })
                }
            }

            return newTvSeriesLoadResponse(veri.title, url, TvType.TvSeries, episodesList) {
                this.posterUrl = veri.image [cite: 2]
                this.plot = veri.description [cite: 2]
                this.year = veri.year [cite: 2]
                this.tags = veri.genres?.map { it.title } [cite: 2]
                this.score = Score.from10(veri.imdb) [cite: 2]
            }
        }

        val isLive = veri.label?.equals("CANLI", ignoreCase = true) == true [cite: 2]
        return if (isLive) {
            newLiveStreamLoadResponse(veri.title, url, url) {
                this.posterUrl = veri.image [cite: 2]
                this.plot = veri.description [cite: 2]
            }
        } else {
            newMovieLoadResponse(veri.title, url, TvType.Movie, url) {
                this.posterUrl = veri.image [cite: 2]
                this.plot = veri.description [cite: 2]
                this.year = veri.year [cite: 2]
                this.tags = veri.genres?.map { it.title } [cite: 2]
                this.score = Score.from10(veri.imdb) [cite: 2]
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (data.startsWith("http")) {
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = data,
                    type = INFER_TYPE
                ) {
                    this.referer = "https://twitter.com/" [cite: 2]
                    this.quality = Qualities.Unknown.value [cite: 2]
                }
            )
            return true
        }

        val veri = AppUtils.tryParseJson<RecItem>(data) ?: return false [cite: 2]

        for (source in veri.sources) {
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = "${this.name} - ${source.type}", [cite: 2]
                    url = source.url, [cite: 2]
                    type = if (source.type == "mp4") ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8 [cite: 2]
                ) {
                    this.referer = "https://twitter.com/" [cite: 2]
                    this.quality = Qualities.Unknown.value [cite: 2]
                }
            )
        }

        return true
    }

    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request() [cite: 2]
            val modifiedRequest = originalRequest.newBuilder()
                .removeHeader("If-None-Match") [cite: 2]
                .header("User-Agent", "googleusercontent") [cite: 2]
                .build()
            chain.proceed(modifiedRequest) [cite: 2]
        }
    }
}
