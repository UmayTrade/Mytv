package com.nikyokki

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class FullHDFilmIzlede : MainAPI() {

    override var mainUrl = "https://www.hdfilmizle.life"
    override var name = "HdFilmIzle"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie)

    // Kategori linklerini verdiğin formata göre güncelledim (-1 ekleri dahil)
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
            // Sitenin sayfalama yapısı genellikle /sayfa/2 şeklindedir
            "${request.data}/sayfa/$page"
        }

        val document = app.get(url).document
        // Gönderdiğin yeni HTML'deki ana kapsayıcı: div.card
        val home = document.select("div.card").mapNotNull { it.toSearchResponse() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        // Resim etiketinden başlığı alıyoruz
        val img = selectFirst("img.img-fluid")
        val title = img?.attr("alt") ?: return null
        
        // Gönderdiğin HTML'de link (href) doğrudan görünmüyor. 
        // Genelde div.card içinde bir 'a' etiketi başlığı veya resmi kapsar.
        val href = fixUrlNull(selectFirst("a")?.attr("href")) ?: return null
        
        // Görseli data-src veya normal src'den çekiyoruz
        val poster = fixUrlNull(img.attr("data-src").ifBlank { img.attr("src") })

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = poster
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        // Arama kısmında site bazen .to bazen .life kullanabiliyor, mainUrl üzerinden gidiyoruz
        val document = app.post(
            "$mainUrl/ara",
            headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            data = mapOf("kelime" to query)
        ).document

        return document.select("div.card").mapNotNull { it.toSearchResponse() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        // Film detay sayfasındaki başlık ve poster seçicileri
        val title = document.selectFirst("h1")?.text()?.replace(" izle", "")?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("picture img")?.attr("data-src") 
                    ?: document.selectFirst("img.img-fluid")?.attr("src"))
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
        
        // Gönderdiğin HTML'de iframe 'vpx' class'ına sahip ve data-src kullanıyor
        val iframe = document.selectFirst("iframe.vpx")
        val iframeSrc = iframe?.attr("data-src") ?: iframe?.attr("src") ?: return false
        
        // Linki dışa aktarıyoruz
        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = fixUrl(iframeSrc),
                type = ExtractorLinkType.M3U8
            ) {
                referer = "$mainUrl/"
                quality = Qualities.Unknown.value
            }
        )
        return true
    }
}
