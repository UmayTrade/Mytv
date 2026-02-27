package com.UmayTrade

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import java.io.InputStream
import java.io.ByteArrayInputStream

class NeonSpor : MainAPI() {
    override var mainUrl          = "https://github.com"
    override var name             = "UmayCanli"
    override val hasMainPage      = true
    override var lang             = "tr"
    override val hasQuickSearch       = true
    override val hasDownloadSupport   = false
    override val supportedTypes       = setOf(TvType.Live)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val rawText = app.get(mainUrl).text
        val kanallar = IptvPlaylistParser().parseM3U(rawText)
        
        if (kanallar.items.isEmpty()) return newHomePageResponse(emptyList(), false)

        return newHomePageResponse(
            kanallar.items.groupBy { it.attributes["group-title"] }.map { group ->
                val title = group.key ?: "Genel"
                val show  = group.value.map { kanal ->
                    val streamurl   = kanal.url ?: ""
                    val channelname = kanal.title ?: "İsimsiz"
                    val posterurl   = kanal.attributes["tvg-logo"] ?: ""
                    val chGroup     = kanal.attributes["group-title"] ?: ""
                    val nation      = kanal.attributes["tvg-country"] ?: ""

                    newLiveSearchResponse(
                        channelname,
                        LoadData(streamurl, channelname, posterurl, chGroup, nation).toJson(),
                        type = TvType.Live
                    ) {
                        this.posterUrl = posterurl
                        this.lang = nation
                    }
                }
                HomePageList(title, show, isHorizontalImages = true)
            },
            hasNext = false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val kanallar = IptvPlaylistParser().parseM3U(app.get(mainUrl).text)
        return kanallar.items.filter { it.title?.lowercase()?.contains(query.lowercase()) == true }.map { kanal ->
            val streamurl   = kanal.url ?: ""
            val channelname = kanal.title ?: ""
            val posterurl   = kanal.attributes["tvg-logo"] ?: ""
            val chGroup     = kanal.attributes["group-title"] ?: ""
            val nation      = kanal.attributes["tvg-country"] ?: ""

            newLiveSearchResponse(
                channelname,
                LoadData(streamurl, channelname, posterurl, chGroup, nation).toJson(),
                type = TvType.Live
            ) {
                this.posterUrl = posterurl
                this.lang = nation
            }
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse {
        val loadData = fetchDataFromUrlOrJson(url)
        val plot = "Açılmayan kanallar için VPN gerekebilir."
        val kanallar = IptvPlaylistParser().parseM3U(app.get(mainUrl).text)
        val recommendations = mutableListOf<LiveSearchResponse>()
    
        for (kanal in kanallar.items) {
            if (kanal.attributes["group-title"] == loadData.group) {
                val rcChannelName = kanal.title ?: continue
                if (rcChannelName == loadData.title) continue

                recommendations.add(newLiveSearchResponse(
                    rcChannelName,
                    LoadData(kanal.url ?: "", rcChannelName, kanal.attributes["tvg-logo"] ?: "", kanal.attributes["group-title"] ?: "", kanal.attributes["tvg-country"] ?: "").toJson(),
                    type = TvType.Live
                ) {
                    this.posterUrl = kanal.attributes["tvg-logo"]
                    this.lang = kanal.attributes["tvg-country"]
                })
            }
        }
    
        return newLiveStreamLoadResponse(loadData.title, loadData.url, url) {
            this.posterUrl = loadData.poster
            this.plot = plot
            this.tags = listOf(loadData.group, loadData.nation)
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val loadData = fetchDataFromUrlOrJson(data)
        val kanallar = IptvPlaylistParser().parseM3U(app.get(mainUrl).text)
        val kanal = kanallar.items.firstOrNull { it.url == loadData.url } ?: return false

        callback.invoke(
            newExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = loadData.url,
                ExtractorLinkType.M3U8
            ) {
                this.headers = kanal.headers
                this.referer = kanal.headers["referrer"] ?: ""
                this.quality = Qualities.Unknown.value
            }
        )
        return true
    }

    data class LoadData(val url: String, val title: String, val poster: String, val group: String, val nation: String)

    private suspend fun fetchDataFromUrlOrJson(data: String): LoadData {
        return if (data.startsWith("{")) {
            parseJson<LoadData>(data)
        } else {
            val kanallar = IptvPlaylistParser().parseM3U(app.get(mainUrl).text)
            val kanal = kanallar.items.firstOrNull { it.url == data }
            LoadData(
                kanal?.url ?: data,
                kanal?.title ?: "Unknown",
                kanal?.attributes?.get("tvg-logo") ?: "",
                kanal?.attributes?.get("group-title") ?: "",
                kanal?.attributes?.get("tvg-country") ?: ""
            )
        }
    }
}

// --- YARDIMCI MODELLER ---

data class Playlist(val items: List<PlaylistItem> = emptyList())

data class PlaylistItem(
    val title: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val url: String? = null,
    val userAgent: String? = null
)

// --- PARSER SINIFI ---

class IptvPlaylistParser {
    companion object {
        const val EXT_M3U     = "#EXTM3U"
        const val EXT_INF     = "#EXTINF"
        const val EXT_VLC_OPT = "#EXTVLCOPT"
    }

    fun parseM3U(content: String): Playlist {
        return parseM3U(content.byteInputStream())
    }

    fun parseM3U(input: InputStream): Playlist {
        val reader = input.bufferedReader()
        val firstLine = reader.readLine()
        if (firstLine == null || !firstLine.startsWith(EXT_M3U)) {
            return Playlist(emptyList())
        }

        val playlistItems = mutableListOf<PlaylistItem>()
        var currentItem: PlaylistItem? = null
        var line: String? = reader.readLine()

        while (line != null) {
            val trimmedLine = line.trim()
            if (trimmedLine.isNotEmpty()) {
                if (trimmedLine.startsWith(EXT_INF)) {
                    val title = trimmedLine.split(",").lastOrNull()?.replace("\"", "")?.trim()
                    val attributes = getAttributes(trimmedLine)
                    currentItem = PlaylistItem(title = title, attributes = attributes)
                } else if (trimmedLine.startsWith(EXT_VLC_OPT)) {
                    currentItem?.let {
                        val userAgent = getTagValue(trimmedLine, "http-user-agent") ?: it.userAgent
                        val referrer = getTagValue(trimmedLine, "http-referrer")
                        val newHeaders = it.headers.toMutableMap()
                        userAgent?.let { ua -> newHeaders["user-agent"] = ua }
                        referrer?.let { ref -> newHeaders["referrer"] = ref }
                        currentItem = it.copy(userAgent = userAgent, headers = newHeaders)
                    }
                } else if (!trimmedLine.startsWith("#")) {
                    currentItem?.let {
                        val url = trimmedLine.split("|").firstOrNull()?.trim()
                        playlistItems.add(it.copy(url = url))
                        currentItem = null // Reset for next item
                    }
                }
            }
            line = reader.readLine()
        }
        return Playlist(playlistItems)
    }

    private fun getAttributes(line: String): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        val regex = Regex("([\\w-]+)=\"([^\"]*)\"")
        regex.findAll(line).forEach { match ->
            attributes[match.groupValues[1]] = match.groupValues[2]
        }
        return attributes
    }

    private fun getTagValue(line: String, key: String): String? {
        val regex = Regex("$key=(.*)", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groups?.get(1)?.value?.replace("\"", "")?.trim()
    }
}
