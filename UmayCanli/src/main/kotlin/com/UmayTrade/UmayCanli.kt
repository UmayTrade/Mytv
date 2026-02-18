package com.UmayTrade

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import java.io.InputStream

class NeonSpor : MainAPI() {
    override var mainUrl          = "https://raw.githubusercontent.com/UmayTrade/extensions/refs/heads/master/umaylist.m3u"
    override var name             = "UmayCanli"
    override val hasMainPage      = true
    override var lang             = "tr"
    override val hasQuickSearch       = true
    override val hasDownloadSupport   = false
    override val supportedTypes       = setOf(TvType.Live)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val rawText = app.get(mainUrl).text
        val kanallar = IptvPlaylistParser().parseM3U(rawText)
        
        // Liste boşsa boş response dön, çökme
        if (kanallar.items.isEmpty()) return newHomePageResponse(emptyList(), false)

        return newHomePageResponse(
            kanallar.items.groupBy { it.attributes["group-title"] }.map { group ->
                val title = group.key ?: "Genel"
                val show  = group.value.map { kanal ->
                    val streamurl   = kanal.url.toString()
                    val channelname = kanal.title.toString()
                    val posterurl   = kanal.attributes["tvg-logo"].toString()
                    val chGroup     = kanal.attributes["group-title"].toString()
                    val nation      = kanal.attributes["tvg-country"].toString()

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
        if (kanallar.items.isEmpty()) return emptyList()

        return kanallar.items.filter { it.title.toString().lowercase().contains(query.lowercase()) }.map { kanal ->
            val streamurl   = kanal.url.toString()
            val channelname = kanal.title.toString()
            val posterurl   = kanal.attributes["tvg-logo"].toString()
            val chGroup     = kanal.attributes["group-title"].toString()
            val nation      = kanal.attributes["tvg-country"].toString()

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
        val nation: String = if (loadData.group == "NSFW") {
            "⚠️🔞🔞🔞 » ${loadData.group} | ${loadData.nation} « 🔞🔞🔞⚠️"
        } else {
            "» ${loadData.group} | ${loadData.nation} «"
        }
    
        val plot = "Açılmayan kanallar için VPN gerekebilir."
        val kanallar = IptvPlaylistParser().parseM3U(app.get(mainUrl).text)
        val recommendations = mutableListOf<LiveSearchResponse>()
    
        for (kanal in kanallar.items) {
            if (kanal.attributes["group-title"].toString() == loadData.group) {
                val rcChannelName = kanal.title.toString()
                if (rcChannelName == loadData.title) continue

                recommendations.add(newLiveSearchResponse(
                    rcChannelName,
                    LoadData(kanal.url.toString(), rcChannelName, kanal.attributes["tvg-logo"].toString(), kanal.attributes["group-title"].toString(), kanal.attributes["tvg-country"].toString()).toJson(),
                    type = TvType.Live
                ) {
                    this.posterUrl = kanal.attributes["tvg-logo"].toString()
                    this.lang = kanal.attributes["tvg-country"].toString()
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
        
        // first yerine firstOrNull kullanarak çökme engellendi
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
        if (data.startsWith("{")) {
            return parseJson<LoadData>(data)
        } else {
            val kanallar = IptvPlaylistParser().parseM3U(app.get(mainUrl).text)
            // Güvenli arama
            val kanal = kanallar.items.firstOrNull { it.url == data } 
                ?: return LoadData(data, "Unknown", "", "", "")

            return LoadData(
                kanal.url.toString(),
                kanal.title.toString(),
                kanal.attributes["tvg-logo"].toString(),
                kanal.attributes["group-title"].toString(),
                kanal.attributes["tvg-country"].toString()
            )
        }
    }
}

// IptvPlaylistParser sınıfındaki düzeltme:
class IptvPlaylistParser {
    // ... (Diğer metodlar aynı)

    fun parseM3U(input: InputStream): Playlist {
        val reader = input.bufferedReader()
        val firstLine = reader.readLine()
        if (firstLine == null || !firstLine.isExtendedM3u()) {
            return Playlist(emptyList()) // Hata atmak yerine boş liste dönmek daha güvenli
        }

        val playlistItems: MutableList<PlaylistItem> = mutableListOf()
        var currentIndex = 0
        var line: String? = reader.readLine()

        while (line != null) {
            if (line.isNotEmpty()) {
                if (line.startsWith(EXT_INF)) {
                    val title = line.getTitle()
                    val attributes = line.getAttributes()
                    playlistItems.add(PlaylistItem(title, attributes))
                } else if (line.startsWith(EXT_VLC_OPT)) {
                    // Liste boş değilse işlem yap
                    if (playlistItems.isNotEmpty()) {
                        val item = playlistItems[currentIndex]
                        val userAgent = item.userAgent ?: line.getTagValue("http-user-agent")
                        val referrer = line.getTagValue("http-referrer")
                        val headers = mutableMapOf<String, String>()
                        if (userAgent != null) headers["user-agent"] = userAgent
                        if (referrer != null) headers["referrer"] = referrer

                        playlistItems[currentIndex] = item.copy(
                            userAgent = userAgent,
                            headers = headers
                        )
                    }
                } else if (!line.startsWith("#")) {
                    // URL Satırı
                    if (playlistItems.isNotEmpty()) {
                        val item = playlistItems[currentIndex]
                        val url = line.getUrl()
                        val userAgent = line.getUrlParameter("user-agent")
                        val referrer = line.getUrlParameter("referer")
                        val urlHeaders = if (referrer != null) { item.headers + mapOf("referrer" to referrer) } else item.headers

                        playlistItems[currentIndex] = item.copy(
                            url = url,
                            headers = item.headers + urlHeaders,
                            userAgent = userAgent ?: item.userAgent
                        )
                        currentIndex++
                    }
                }
            }
            line = reader.readLine()
        }
        return Playlist(playlistItems)
    }
    // ... (Yardımcı extension metodları aynı kalabilir)
}
