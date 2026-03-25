package com.kraptor

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import org.jsoup.nodes.Element
import org.json.JSONObject

class DiziYoUltimate : MainAPI() {

    override var name = "DiziYo Ultimate"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Anime)

    // 🔥 DOMAIN HAVUZU (AUTO SWITCH)
    private val domains = listOf(
        "https://www.diziyo.nl",
        "https://www.diziyo.cx",
        "https://www.diziyo.sx",
        "https://www.diziyo.xyz"
    )

    override var mainUrl = domains.first()

    // ================= DOMAIN AUTO SWITCH =================

    private suspend fun getWorkingDomain(): String {
        for (domain in domains) {
            try {
                val res = app.get(domain, timeout = 10, interceptor = CloudflareKiller())
                if (res.text.contains("dizi") || res.code == 200) {
                    Log.d("DIZIYO", "WORKING DOMAIN: $domain")
                    return domain
                }
            } catch (_: Exception) {}
        }
        return domains.first()
    }

    // ================= MAIN PAGE =================

    override val mainPage = mainPageOf(
        "/filmdil/turkce-dublaj-film-izle/?sf_paged=" to "Dublaj Filmler",
        "/dil/turkce-altyazi-dizi-izle/page" to "Diziler",
        "/dil/turkce-altyazi-anime-izle/page" to "Anime"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {

        mainUrl = getWorkingDomain()

        val url = if (request.data.contains("sf_paged=")) {
            mainUrl + request.data.replace(Regex("sf_paged=\\d+"), "sf_paged=$page")
        } else {
            "$mainUrl${request.data}$page/"
        }

        val doc = app.get(url, interceptor = CloudflareKiller()).document

        val list = doc.select("article").mapNotNull { it.toSearch() }

        return newHomePageResponse(request.name, list)
    }

    private fun Element.toSearch(): SearchResponse? {
        val title = selectFirst("h3, h2, .title")?.text() ?: return null
        val link = fixUrlNull(selectFirst("a")?.attr("href")) ?: return null
        val poster = fixUrlNull(
            selectFirst("img")?.attr("data-src")
                ?: selectFirst("img")?.attr("src")
        )

        return newMovieSearchResponse(title, link, TvType.Movie) {
            this.posterUrl = poster
        }
    }

    // ================= SEARCH =================

    override suspend fun search(query: String): List<SearchResponse> {

        mainUrl = getWorkingDomain()

        val doc = app.get(
            "$mainUrl/?s=$query",
            interceptor = CloudflareKiller()
        ).document

        return doc.select("article").mapNotNull { it.toSearch() }
    }

    // ================= LOAD =================

    override suspend fun load(url: String): LoadResponse? {

        val doc = app.get(url, interceptor = CloudflareKiller()).document

        val title = doc.selectFirst("h1")?.text() ?: return null
        val poster = fixUrlNull(doc.selectFirst("img")?.attr("src"))
        val desc = doc.selectFirst("p")?.text()

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

    // ================= MULTI SERVER LINK =================

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        return try {

            val doc = app.get(data, interceptor = CloudflareKiller()).document

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
                    "type" to "movie"
                ),
                headers = mapOf(
                    "X-Requested-With" to "XMLHttpRequest",
                    "Referer" to data
                ),
                interceptor = CloudflareKiller()
            ).text

            // 🔥 TÜM iframe'leri al (multi server)
            val servers = Regex("""src="(https?:\/\/[^"]+)"""")
                .findAll(ajaxRes)
                .map { it.groupValues[1] }
                .toList()

            for (server in servers) {

                try {
                    Log.d("DIZIYO", "SERVER: $server")

                    // 1. Direkt m3u8
                    if (server.contains(".m3u8")) {
                        callback.invoke(
                            newExtractorLink(name, "Direct", server, ExtractorLinkType.M3U8)
                        )
                        continue
                    }

                    // 2. Hash sistemi
                    val hash = Regex("""video\/([a-zA-Z0-9]+)""")
                        .find(server)
                        ?.groupValues?.get(1)

                    if (hash != null) {

                        val json = app.post(
                            "$server?do=getVideo",
                            data = mapOf(
                                "hash" to hash,
                                "r" to mainUrl
                            ),
                            headers = mapOf("Referer" to server),
                            interceptor = CloudflareKiller()
                        ).text

                        val file = JSONObject(json)
                            .getJSONArray("videoSources")
                            .getJSONObject(0)
                            .getString("file")

                        M3u8Helper.generateM3u8(
                            name,
                            file,
                            server
                        ).forEach(callback)

                        continue
                    }

                    // 3. Extractor fallback
                    loadExtractor(server, data, subtitleCallback, callback)

                } catch (e: Exception) {
                    Log.e("DIZIYO", "SERVER FAIL", e)
                }
            }

            true

        } catch (e: Exception) {
            Log.e("DIZIYO", "TOTAL FAIL", e)
            false
        }
    }
}
