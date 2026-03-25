// ! PRO DiziYo Plugin - Fixlenmiş & Stabil

package com.kraptor

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.json.JSONObject
import java.net.URI

class DiziYo : MainAPI() {

    override var mainUrl = "https://www.diziyo.so"
    override var name = "DiziYo"
    override val hasMainPage = true
    override var lang = "tr"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Anime)

    // ================= MAIN PAGE =================

    override val mainPage = mainPageOf(
        "$mainUrl/filmdil/turkce-dublaj-film-izle/?sf_paged=" to "Dublaj Filmler",
        "$mainUrl/dil/turkce-altyazi-dizi-izle/page" to "Diziler",
        "$mainUrl/dil/turkce-altyazi-anime-izle/page" to "Anime"
    )

    private fun buildUrl(url: String, page: Int): String {
        return if (url.contains("sf_paged=")) {
            url.replace(Regex("sf_paged=\\d+"), "sf_paged=$page")
        } else {
            "$url$page/"
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get(buildUrl(request.data, page)).document
        val home = doc.select("div.items article").mapNotNull { it.toSearch() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearch(): SearchResponse? {
        val title = selectFirst("div.data")?.text() ?: return null
        val link = fixUrlNull(selectFirst("a")?.attr("href")) ?: return null
        val poster = fixUrlNull(selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, link, TvType.Movie) {
            this.posterUrl = poster
        }
    }

    // ================= SEARCH =================

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=$query").document
        return doc.select("div.result-item article").mapNotNull { it.toSearch() }
    }

    // ================= LOAD =================

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document

        val title = doc.selectFirst("h1")?.text() ?: return null
        val poster = fixUrlNull(doc.selectFirst("div.poster img")?.attr("src"))
        val desc = doc.selectFirst("div.wp-content p")?.text()

        val isSeries = doc.selectFirst("#episodes") != null

        return if (isSeries) {

            val episodes = doc.select("#episodes li a").map {
                newEpisode(fixUrlNull(it.attr("href"))) {
                    name = it.text()
                }
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = desc
            }

        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = desc
            }
        }
    }

    // ================= LINK ÇEKME =================

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        try {
            val doc = app.get(data).document

            val postId = Regex("postid-(\\d+)")
                .find(doc.html())
                ?.groupValues?.get(1) ?: return false

            val ajax = "$mainUrl/wp-admin/admin-ajax.php"

            val ajaxRes = app.post(
                ajax,
                data = mapOf(
                    "action" to "doo_player_ajax",
                    "post" to postId,
                    "nume" to "1",
                    "type" to if (data.contains("/dizi/")) "tv" else "movie"
                ),
                headers = mapOf(
                    "X-Requested-With" to "XMLHttpRequest",
                    "Referer" to data
                )
            ).text

            val iframe = Regex("""src="(https?:\/\/[^"]+)"""")
                .find(ajaxRes)
                ?.groupValues?.get(1)
                ?: return false

            val hash = Regex("""video\/([a-zA-Z0-9]+)""")
                .find(iframe)
                ?.groupValues?.get(1)
                ?: return false

            val json = app.post(
                "$iframe?do=getVideo",
                data = mapOf(
                    "hash" to hash,
                    "r" to mainUrl
                ),
                headers = mapOf(
                    "X-Requested-With" to "XMLHttpRequest",
                    "Referer" to iframe
                )
            ).text

            Log.d("DIZIYO", json)

            val file = JSONObject(json)
                .getJSONArray("videoSources")
                .getJSONObject(0)
                .getString("file")

            // MASTER M3U8
            val m3u8 = app.get(file).text

            val base = file.substringBeforeLast("/") + "/"

            m3u8.lines().forEachIndexed { i, line ->
                if (line.contains("RESOLUTION") && i + 1 < m3u8.lines().size) {

                    val quality = when {
                        line.contains("1920x1080") -> Qualities.P1080.value
                        line.contains("1280x720") -> Qualities.P720.value
                        line.contains("854x480") -> Qualities.P480.value
                        line.contains("640x360") -> Qualities.P360.value
                        else -> Qualities.Unknown.value
                    }

                    val video = base + m3u8.lines()[i + 1]

                    callback.invoke(
                        newExtractorLink(
                            source = name,
                            name = "$name ${quality}p",
                            url = video,
                            type = ExtractorLinkType.M3U8
                        ) {
                            this.quality = quality
                            this.headers = mapOf("Referer" to iframe)
                        }
                    )
                }
            }

            return true

        } catch (e: Exception) {
            Log.e("DIZIYO", "ERROR", e)
            return false
        }
    }
}
