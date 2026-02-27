package com.keyiflerolsun

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

// ✅ DOSYA SEVİYESİ EXTENSION (CLASS DIŞINDA)
val Int.toMillis: Long
    get() = this * 1000L

class RecTV : MainAPI() {

    override var mainUrl = "https://rectv.example"   // BURAYI SİTENLE DEĞİŞTİR
    override var name = "RecTV"
    override val hasMainPage = true
    override var lang = "tr"

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    // ---------------- MAIN PAGE ----------------

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document

        val items = document.select("div.item").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            listOf(
                HomePageList(
                    name = "Son Eklenenler",
                    list = items
                )
            ),
            hasNext = false
        )
    }

    // ---------------- SEARCH ----------------

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("div.item").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val poster = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = poster
        }
    }

    // ---------------- LOAD ----------------

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text() ?: return null
        val poster = fixUrlNull(document.selectFirst("img")?.attr("src"))
        val plot = document.selectFirst(".description")?.text()

        val links = document.select("iframe").mapNotNull {
            it.attr("src")
        }

        return newMovieLoadResponse(title, url, TvType.Movie, links) {
            this.posterUrl = poster
            this.plot = plot
        }
    }

    // ---------------- LINKS ----------------

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        callback.invoke(
            ExtractorLink(
                source = name,
                name = name,
                url = data,
                referer = mainUrl,
                quality = Qualities.Unknown.value,
                isM3u8 = data.contains(".m3u8")
            )
        )

        return true
    }
}
