// ! DiziYo Ultimate - FINAL (Cloudflare FIX + Stabil)

package com.kraptor

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.json.JSONObject

class DiziYo : MainAPI() {

    override var mainUrl = "https://www.diziyo.so"
    override var name = "DiziYo Ultimate"
    override val hasMainPage = true
    override var lang = "tr"

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime
    )

    // ================= MAIN PAGE =================

    override val mainPage = mainPageOf(
        "$mainUrl/filmdil/turkce-dublaj-film-izle/?sf_paged=" to "🔥 Dublaj",
        "$mainUrl/dil/turkce-altyazi-dizi-izle/page" to "📺 Diziler",
        "$mainUrl/dil/turkce-altyazi-anime-izle/page" to "🍥 Anime"
    )

    private fun buildUrl(base: String, page: Int): String {
        return if (base.contains("sf_paged=")) {
            base.replace(Regex("sf_paged=\\d+"), "sf_paged=$page")
        } else "$base$page/"
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {

        val doc = app.get(
            buildUrl(request.data, page),
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to mainUrl
            )
        ).document

        val home = doc.select("div.items > article, article")
            .mapNotNull { it.toSearchFix() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchFix(): SearchResponse? {

        val link = fixUrlNull(selectFirst("a")?.attr("href")) ?: return null

        val title = selectFirst("h3, h2, .data, .title")
            ?.text()
            ?.replace("izle", "", true)
            ?.trim()
            ?: return null

        val poster = fixUrlNull(
            selectFirst("img")?.attr("data-src")
                ?: selectFirst("img")?.attr("data-lazy-src")
                ?: selectFirst("img")?.attr("src")
        )

        return newMovieSearchResponse(title, link, TvType.Movie) {
            this.posterUrl = poster
        }
    }

    // ================= SEARCH =================

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get(
            "$mainUrl/?s=$query",
            headers = mapOf("User-Agent" to USER_AGENT)
        ).document

        return doc.select("div.result-item article")
            .mapNotNull { it.toSearchFix() }
    }

    // ================= LOAD =================

    override suspend fun load(url: String): LoadResponse? {

        val doc = app.get(
            url,
            headers = mapOf("User-Agent" to USER_AGENT)
        ).document

        val title = doc.selectFirst("h1")?.text() ?: return null

        val poster = fixUrlNull(
            doc.selectFirst("div.poster img")?.attr("data-src")
                ?: doc.selectFirst("div.poster img")?.attr("src")
        )

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

    // ================= LINK ENGINE =================

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        try {

            val doc = app.get(
                data,
                headers = mapOf("User-Agent" to USER_AGENT)
            ).document

            val postId = Regex("postid-(\\d+)")
                .find(doc.html())
                ?.groupValues?.get(1)
                ?: return false

            val ajaxRes = app.post(
                "$mainUrl/wp-admin/admin-ajax.php",
                data = mapOf(
                    "action" to "doo_player_ajax",
                    "post" to postId,
                    "nume" to "1",
                    "type" to if (data.contains("/dizi/")) "tv" else "movie"
                ),
                headers = mapOf(
                    "X-Requested-With" to "XMLHttpRequest",
                    "Referer" to data,
                    "User-Agent" to USER_AGENT
                )
            ).text

            val iframes = Regex("""src="(https?:\/\/[^"]+)"""")
                .findAll(ajaxRes)
                .map { it.groupValues[1] }
                .toList()

            iframes.forEach { iframe ->

                try {

                    val hash = Regex("""video\/([a-zA-Z0-9]+)""")
                        .find(iframe)
                        ?.groupValues?.get(1) ?: return@forEach

                    val json = app.post(
                        "$iframe?do=getVideo",
                        data = mapOf(
                            "hash" to hash,
                            "r" to mainUrl
                        ),
                        headers = mapOf(
                            "X-Requested-With" to "XMLHttpRequest",
                            "Referer" to iframe,
                            "User-Agent" to USER_AGENT
                        )
                    ).text

                    val obj = JSONObject(json)
                    val sources = obj.getJSONArray("videoSources")

                    for (i in 0 until sources.length()) {

                        val file = sources.getJSONObject(i).getString("file")

                        if (file.contains(".m3u8")) {

                            val master = app.get(file).text
                            val base = file.substringBeforeLast("/") + "/"
                            val lines = master.lines()

                            for (j in lines.indices) {

                                if (lines[j].contains("RESOLUTION") && j + 1 < lines.size) {

                                    val quality = when {
                                        lines[j].contains("1920x1080") -> Qualities.P1080.value
                                        lines[j].contains("1280x720") -> Qualities.P720.value
                                        lines[j].contains("854x480") -> Qualities.P480.value
                                        lines[j].contains("640x360") -> Qualities.P360.value
                                        else -> Qualities.Unknown.value
                                    }

                                    val video = base + lines[j + 1]

                                    callback.invoke(
                                        newExtractorLink(
                                            source = name,
                                            name = "$name ${quality}p",
                                            url = video,
                                            type = ExtractorLinkType.M3U8
                                        ) {
                                            this.quality = quality
                                            this.headers = mapOf(
                                                "Referer" to iframe,
                                                "User-Agent" to USER_AGENT
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // SUBTITLE
                    if (obj.has("tracks")) {
                        val tracks = obj.getJSONArray("tracks")

                        for (i in 0 until tracks.length()) {
                            val sub = tracks.getJSONObject(i)

                            subtitleCallback.invoke(
                                SubtitleFile(
                                    sub.getString("label"),
                                    sub.getString("file")
                                )
                            )
                        }
                    }

                } catch (e: Exception) {
                    Log.e("DIZIYO_IFRAME", "ERR", e)
                }
            }

            return true

        } catch (e: Exception) {
            Log.e("DIZIYO", "FATAL", e)
            return false
        }
    }
}
