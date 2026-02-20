package com.nikyokki

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class FullHDFilmIzlede : MainAPI() {

    // Site adresi gönderdiğin HTML'e göre güncellendi
    override var mainUrl = "https://www.hdfilmizle.life"
    override var name = "HdFilmIzle"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie)

    // Kategori linkleri sitenin yeni yapısına göre düzeltildi
    override val mainPage = mainPageOf(
        "$mainUrl/tur/aile-1" to "Aile",
        "$mainUrl/tur/aksiyon-2" to "Aksiyon",
        "$mainUrl/tur/komedi-1" to "Komedi",
        "$mainUrl/tur/dram-1" to "Dram",
        "$mainUrl/tur/korku-1" to "Korku",
        "$mainUrl/tur/bilim-kurgu-1" to "Bilim Kurgu"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) {
            request.data
        } else {
            "${request.data}/sayfa/$page"
        }

        val document = app.get(url).document
        // Gönderdiğin HTML'deki kapsayıcıyı (play-that-video) hedefliyoruz
        val home = document.select("div.play-that-video").mapNotNull { it.toSearchResponse() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        // Resim etiketinden başlığı (alt) ve görseli (data-src) alıyoruz
        val img = selectFirst("img")
        val title = img?.attr("alt") ?: return null
        
        // Linki bulmak için kapsayıcıyı veya içindeki a etiketini kontrol ediyoruz
        val href = fixUrlNull(selectFirst("a")?.attr("href") ?: parent()?.attr("href")) ?: return null
        
        // Gönderdiğin HTML'de data-src kullanılmış, ona öncelik veriyoruz
        val poster = fixUrlNull(img.attr("data-src").ifBlank { img.attr("src") })

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = poster
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.post(
            "$mainUrl/ara",
            headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            data = mapOf("kelime" to query)
        ).document

        return document.select("div.play-that-video").mapNotNull { it.toSearchResponse() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        // Başlık genellikle h1 veya h2 içindedir
        val title = document.selectFirst("h1")?.text()?.replace(" izle", "")?.trim() 
                    ?: document.selectFirst("div.movieBar h2")?.text()?.replace(" izle", "")?.trim()
                    ?: return null

        val poster = fixUrlNull(document.selectFirst("picture img")?.attr("data-src") ?: document.selectFirst("div.moviePoster img")?.attr("src"))
        val description = document.selectFirst("div.movieDescription, div.content")?.text()?.trim()

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        // İframe kaynağını bul
        val iframeSrc = document.selectFirst("iframe")?.attr("src") ?: return false
        
        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = iframeSrc,
                type = ExtractorLinkType.M3U8
            ) {
                referer = "$mainUrl/"
                quality = Qualities.Unknown.value
            }
        )
        return true
    }
}
