package com.cloudstream.plugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import org.jsoup.nodes.Element
import android.util.Base64
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class HdfilmcehennemiProvider : MainAPI() {
    override var mainUrl = "https://www.hdfilmcehennemi.nl"
    override var name = "HDFilmCehennemi"
    override val hasMainPage = true
    override var lang = "tr"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "" to "Son Eklenen Filmler",
        "dizi/" to "Son Eklenen Diziler",
        "category/tavsiye-filmler-izle2/" to "Tavsiye Filmler",
        "imdb-7-puan-uzeri-filmler-2/" to "IMDb 7+ Filmler"
    )

    // SABİT referer - her yerde mainUrl kullan
    private val playerReferer get() = mainUrl
    private val defaultHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) {
            if (request.data.isEmpty()) mainUrl else "$mainUrl/${request.data}"
        } else {
            if (request.data.isEmpty()) "$mainUrl/page/$page/"
            else "$mainUrl/${request.data}page/$page/"
        }

        val doc = app.get(url, headers = defaultHeaders).document
        val home = doc.select("a.poster, div.poster, div.mini-poster, .poster-wrapper")
            .mapNotNull { it.toSearchResult() }
            .distinctBy { it.url }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = false
            ),
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val linkElem = if (this.tagName() == "a") this
                       else this.selectFirst("a[href]") ?: return null
        val href = fixUrl(linkElem.attr("href"))
        if (href == mainUrl || href.endsWith("/#") ||
            href.contains("/category/") || href.contains("/tur/") ||
            href.contains("/page/")) return null

        val title = this.selectFirst(".poster-title, .mini-poster-title, .title, h2, h3")
            ?.text()?.trim()?.ifEmpty { null }
            ?: linkElem.attr("title").trim().ifEmpty { null }
            ?: linkElem.attr("aria-label").trim().ifEmpty { null }
            ?: return null

        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("data-src")?.ifEmpty { null }
                ?: this.selectFirst("img")?.attr("src")
                    ?.let { if (it.startsWith("data:")) null else it }
                ?: this.selectFirst("img")?.attr("srcset")
                    ?.split(" ")?.firstOrNull()
        )

        val isTvSeries = href.contains("/dizi/") ||
                this.selectFirst(".badge-dizi, .is-series, .mini-poster-meta")
                    ?.text()?.contains("Dizi", ignoreCase = true) == true

        return if (isTvSeries) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        // ✅ DÜZELTME: URL encode
        val encoded = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "$mainUrl/search?q=$encoded"

        val jsonResp = app.get(
            searchUrl,
            referer = mainUrl,
            headers = defaultHeaders + mapOf(
                "X-Requested-With" to "fetch",
                "Accept" to "application/json"
            )
        ).text

        val results = mutableListOf<SearchResponse>()
        val combinedHtml = jsonResp
            .replace("\\\"", "\"")
            .replace("\\/", "/")
            .replace("\\n", "\n")
        val fragDoc = org.jsoup.Jsoup.parse(combinedHtml)

        fragDoc.select("a[href*='hdfilmcehennemi']").forEach { link ->
            val href = fixUrl(link.attr("href"))
            if (!href.contains("/category/") && !href.contains("/tur/") && href != mainUrl) {
                val title = link.selectFirst("strong, .title, h3, h4")?.text()?.trim()
                    ?: link.attr("title").trim()
                val poster = fixUrlNull(
                    link.selectFirst("img")?.attr("data-src")
                        ?: link.selectFirst("img")?.attr("src")
                            ?.let { if (it.startsWith("data:")) null else it }
                )
                if (title.isNotEmpty()) {
                    val isSeries = href.contains("/dizi/")
                    if (isSeries) {
                        results.add(newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                            this.posterUrl = poster
                        })
                    } else {
                        results.add(newMovieSearchResponse(title, href, TvType.Movie) {
                            this.posterUrl = poster
                        })
                    }
                }
            }
        }
        return results.distinctBy { it.url }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = defaultHeaders).document

        val rawTitle = doc.selectFirst("h1, .poster-title, .movie-title")?.text()?.trim()
            ?: doc.selectFirst("meta[property='og:title']")?.attr("content")?.trim()
            ?: "Film"
        val title = rawTitle.replace(Regex("""(?i)\s*(izle|film izle|hd film izle).*"""), "").trim()

        val posterUrl = fixUrlNull(
            doc.selectFirst("meta[property='og:image']")?.attr("content")
                ?: doc.selectFirst(".poster-media img, .movie-poster img, .poster img")?.attr("data-src")
                ?: doc.selectFirst(".poster-media img, .movie-poster img, .poster img")?.attr("src")
        )
        val description = doc.selectFirst(
            ".movie-story, .story, .overview, p.description, .entry-content, " +
                    ".film-ozeti, .ozet, meta[name='description']"
        )?.text()?.trim()
            ?: doc.selectFirst("meta[property='og:description']")?.attr("content")?.trim()
        val year = doc.selectFirst("a[href*='/yil/'], span.year, .release-date")
            ?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val score = Score.from10(
            doc.selectFirst(".imdb-score, .rating, .score")
                ?.text()?.trim()?.replace(",", ".")?.toDoubleOrNull()
        )
        val tags = doc.select("a[href*='/tur/']").map { it.text().trim() }
            .filter { it.isNotEmpty() }

        val isTvSeries = url.contains("/dizi/") ||
                doc.select(".season-wrapper, .episode-list, .season, select#season-select")
                    .isNotEmpty()

        return if (isTvSeries) {
            val episodes = mutableListOf<Episode>()

            // ✅ DÜZELTME: Sezonları hem select hem div'den topla
            val seasonContainers = doc.select(".season-wrapper, .season, .episodes-list, " +
                    "div[data-season], div.bolumler")

            if (seasonContainers.isNotEmpty()) {
                seasonContainers.forEachIndexed { seasonIdx, seasonElem ->
                    val seasonNum = seasonElem.attr("data-season")
                        .toIntOrNull() ?: (seasonIdx + 1)
                    seasonElem.select("a[href*='/bolum/'], a[href*='/sezon/'], " +
                            ".episode-item a, li a[href]").forEachIndexed { epIdx, epElem ->
                        val epUrl = fixUrl(epElem.attr("href"))
                        if (epUrl.isBlank() || epUrl == url) return@forEachIndexed
                        val epName = epElem.text().trim()
                            .ifEmpty { "Bölüm ${epIdx + 1}" }
                        episodes.add(
                            newEpisode(epUrl) {
                                this.name = epName
                                this.season = seasonNum
                                this.episode = epIdx + 1
                            }
                        )
                    }
                }
            } else {
                // Fallback: sayfadaki tüm /bolum/ linkleri tek sezon kabul et
                doc.select("a[href*='/bolum/']").forEachIndexed { idx, a ->
                    val epUrl = fixUrl(a.attr("href"))
                    if (epUrl.isBlank()) return@forEachIndexed
                    episodes.add(
                        newEpisode(epUrl) {
                            this.name = a.text().trim().ifEmpty { "Bölüm ${idx + 1}" }
                            this.season = 1
                            this.episode = idx + 1
                        }
                    )
                }
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = posterUrl
                this.plot = description
                this.year = year
                this.tags = tags
                this.score = score
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = posterUrl
                this.plot = description
                this.year = year
                this.tags = tags
                this.score = score
            }
        }
    }

    // ---------- JS unpacker (daha sağlam) ----------
    private fun unpackJs(packedCode: String): String {
        val match = Regex(
            """eval\s*\(\s*function\s*\(\s*p\s*,\s*a\s*,\s*c\s*,\s*k\s*,\s*e\s*,\s*d\s*\)\s*\{[\s\S]*?\}\s*\(\s*'([\s\S]*?)'\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*'([\s\S]*?)'\s*\.split\('\|'\)"""
        ).find(packedCode) ?: Regex(
            """}\s*\(\s*'([\s\S]*?)'\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*'([\s\S]*?)'\.split\('\|'\)"""
        ).find(packedCode) ?: return packedCode

        val payload = match.groupValues[1]
        val radix = match.groupValues[2].toIntOrNull() ?: 36
        val syms = match.groupValues[4].split("|")
        val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

        fun lookup(word: String): String {
            var res = 0
            for (c in word) {
                val idx = chars.indexOf(c)
                if (idx >= 0) res = res * radix + idx
            }
            return if (res < syms.size && syms[res].isNotEmpty()) syms[res] else word
        }

        return Regex("""\b\w+\b""").replace(payload) { lookup(it.value) }
    }

    // ---------- dc_ stream decoder (regex'ler düzeltildi) ----------
    private fun decodeStreamUrl(embedHtml: String): String? {
        val unpacked = unpackJs(embedHtml)

        val callMatch = Regex("""dc_[A-Za-z0-9_]+\s*\(\s*\[(.*?)\]\s*\)""")
            .find(unpacked) ?: return null
        val rawArray = callMatch.groupValues[1]
        val parts = rawArray.split(",").map { it.trim('"', '\'', ' ', ';') }

        val funcMatch = Regex(
            """function\s+dc_[A-Za-z0-9_]+\s*\([^)]*\)\s*\{([\s\S]*?)(?:return\s+unmix;|return\s+result;)"""
        ).find(unpacked) ?: return null
        val body = funcMatch.groupValues[1]

        var curr = parts.joinToString("")

        data class Op(val index: Int, val type: String, val value: Any?)
        val ops = mutableListOf<Op>()

        Regex("""atob\s*\(""").findAll(body).forEach {
            ops.add(Op(it.range.first, "atob", null))
        }
        Regex("""reverse\s*\(\s*\)""").findAll(body).forEach {
            ops.add(Op(it.range.first, "reverse", null))
        }
        // ✅ DÜZELTME: doğru regex
        Regex("""replace\s*\(\s*/\[a-zA-Z\]/g""").findAll(body).forEach { match ->
            val sub = body.substring(match.range.first,
                (match.range.first + 200).coerceAtMost(body.length))
            val shiftMatch = Regex("""o\s*-\s*base\s*\+\s*(\d+)""").find(sub)
            val shift = shiftMatch?.groupValues?.get(1)?.toIntOrNull() ?: 6
            ops.add(Op(match.range.first, "rot", shift))
        }
        Regex("""for\s*\(""").findAll(body).forEach { match ->
            val accMatch = Regex("""var\s+acc\s*=\s*(\d+)""").find(body)
            val stepMatch = Regex("""acc\s*=\s*\(\s*acc\s*\+\s*(\d+)\s*\)""").find(body)
            if (accMatch != null && stepMatch != null) {
                ops.add(Op(match.range.first, "xor",
                    Pair(accMatch.groupValues[1].toInt(), stepMatch.groupValues[1].toInt())))
            }
        }

        ops.sortBy { it.index }

        for (op in ops) {
            when (op.type) {
                "atob" -> {
                    val pad = (4 - curr.length % 4) % 4
                    curr += "=".repeat(pad)
                    curr = String(Base64.decode(curr, Base64.DEFAULT),
                        StandardCharsets.ISO_8859_1)
                }
                "reverse" -> curr = curr.reversed()
                "rot" -> {
                    val shift = op.value as Int
                    curr = curr.map { c ->
                        when (c) {
                            in 'a'..'z' -> ((c.code - 97 + shift) % 26 + 97).toChar()
                            in 'A'..'Z' -> ((c.code - 65 + shift) % 26 + 65).toChar()
                            else -> c
                        }
                    }.joinToString("")
                }
                "xor" -> {
                    val (startAcc, step) = op.value as Pair<*, *>
                    var acc = startAcc as Int
                    val st = step as Int
                    val unmix = StringBuilder()
                    for (char in curr) {
                        val byte = char.code and 0xFF
                        acc = (acc + st) % 256
                        val plain = byte xor acc
                        acc = (acc + byte) % 256
                        unmix.append(plain.toChar())
                    }
                    curr = unmix.toString()
                    break
                }
            }
        }

        return if (curr.startsWith("http")) curr else null
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, headers = defaultHeaders).document
        val embedSources = mutableListOf<Pair<String, String>>()

        // 1) alternative-links (data-video)
        doc.select(".alternative-links").forEach { div ->
            val langAttr = div.attr("data-lang")
            val langLabel = when (langAttr) {
                "tr" -> "Türkçe Dublaj"
                "en" -> "Türkçe Altyazılı"
                else -> "TR-EN Dual"
            }

            div.select("button[data-video], a[data-video]").forEach { btn ->
                val videoId = btn.attr("data-video")
                val btnName = btn.text().trim().ifEmpty { "Alternatif" }
                if (videoId.isEmpty()) return@forEach

                try {
                    val jsonUrl = "$mainUrl/video/$videoId/"
                    val jsonResp = app.get(
                        jsonUrl,
                        referer = data,
                        headers = defaultHeaders + mapOf(
                            "X-Requested-With" to "fetch",
                            "Accept" to "application/json"
                        )
                    ).text

                    // ✅ DÜZELTME: JSON içindeki HTML'i de parse et
                    val cleaned = jsonResp.replace("\\/", "/")
                    val iframeMatch = Regex("""(?:data-src|src)\\?=\\?["']([^"']+)""")
                        .find(cleaned)
                    if (iframeMatch != null) {
                        val iframeUrl = fixUrl(iframeMatch.groupValues[1])
                        if (iframeUrl.isNotBlank())
                            embedSources.add(iframeUrl to "$langLabel ($btnName)")
                    }
                } catch (_: Exception) { /* ignore */ }
            }
        }

        // 2) doğrudan iframe
        doc.select("iframe[src], iframe[data-src]").forEach {
            val src = it.attr("src").ifEmpty { it.attr("data-src") }
            if (src.isNotEmpty() && !src.contains("youtube.com") &&
                !src.contains("youtu.be")) {
                embedSources.add(fixUrl(src) to "Varsayılan")
            }
        }

        for ((sourceUrl, optionLabel) in embedSources.distinctBy { it.first }) {
            try {
                if (sourceUrl.contains("hdfilmcehennemi") ||
                    sourceUrl.contains("rapid") ||
                    sourceUrl.contains("closeload") ||
                    sourceUrl.contains("playmix") ||
                    sourceUrl.contains("rplayer")) {

                    val embedDoc = app.get(sourceUrl, referer = data,
                        headers = defaultHeaders).text

                    val streamUrls = mutableListOf<String>()

                    decodeStreamUrl(embedDoc)?.let { streamUrls.add(it) }

                    Regex("""(https?://[^\s"'<>]+\.(?:m3u8|txt|mp4)[^\s"'<>]*)""")
                        .findAll(embedDoc).forEach { m ->
                            val u = m.value.replace("\\/", "/")
                            if (!u.contains("player") && !u.contains("favicon") &&
                                !u.contains(".vtt")) {
                                streamUrls.add(u)
                            }
                        }

                    // ✅ DÜZELTME: Referer = mainUrl (sabit .mobi DEĞİL)
                    val playerHeaders = mapOf(
                        "Referer" to playerReferer,
                        "User-Agent" to defaultHeaders["User-Agent"]!!
                    )

                    for (videoUrl in streamUrls.distinct()) {
                        // ✅ DÜZELTME: önce M3U8 master'ı doğrudan emit et
                        val masterLink = newExtractorLink(
                            source = name,
                            name = "$name - $optionLabel",
                            url = videoUrl,
                            type = ExtractorLinkType.M3U8
                        ) {
                            this.referer = playerReferer
                            this.headers = playerHeaders
                            this.quality = Qualities.Unknown.value
                        }
                        callback.invoke(masterLink)

                        // Alt kaliteler (opsiyonel, hata fırlatırsa yut)
                        try {
                            val subs = M3u8Helper.generateM3u8(
                                videoUrl,
                                playerReferer,
                                playerHeaders
                            )
                            subs.forEach { link ->
                                callback.invoke(
                                    newExtractorLink(
                                        source = link.source,
                                        name = "$name - $optionLabel (${link.name})",
                                        url = link.url,
                                        type = if (link.isM3u8) ExtractorLinkType.M3U8
                                               else ExtractorLinkType.VIDEO
                                    ) {
                                        this.referer = playerReferer
                                        this.headers = playerHeaders
                                        this.quality = link.quality
                                    }
                                )
                            }
                        } catch (_: Exception) { /* alt kalite yoksa yok */ }
                    }

                    // VTT altyazı
                    Regex(""""file"\s*:\s*"([^"]+\.vtt[^"]*)".{0,50}?"label"\s*:\s*"([^"]+)"""")
                        .findAll(embedDoc).forEach { m ->
                            val subUrl = m.groupValues[1].replace("\\/", "/")
                            val subLang = m.groupValues[2]
                                .replace("\\u00fc", "ü").replace("\\u00e7", "ç")
                                .replace("\\u0131", "ı").replace("\\u00f6", "ö")
                            subtitleCallback.invoke(SubtitleFile(lang = subLang, url = subUrl))
                        }
                } else {
                    loadExtractor(sourceUrl, subtitleCallback, callback)
                }
            } catch (_: Exception) { /* ignore */ }
        }

        return true
    }
}
