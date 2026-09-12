package com.UmayTrade

import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities

class VidMolyExtractor : ExtractorApi() {
    override val name = "VidMoly"
    override val mainUrl = "https://vidmoly.net"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        // URL'den dil hash'ini al
        val langTag = url.substringAfterLast("#", "")
        val cleanUrl = url.substringBefore("#")

        val dilLabel = when (langTag) {
            "dublaj" -> " - Türkçe Dublaj"
            "altyazi" -> " - Türkçe Altyazılı"
            else -> ""
        }
        val displayName = "$name$dilLabel"

        Log.d(name, "Dil >> $displayName")
        Log.d(name, "cleanUrl >> $cleanUrl")

        // VidMoly embed sayfası referer olarak kendi URL'sini bekler.
        // Ana site referer'i bazen 403 döndürür.
        val embedReferer = cleanUrl

        val response = try {
            app.get(
                cleanUrl,
                referer = embedReferer,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0.0.0 Safari/537.36",
                    "Accept" to "*/*",
                    "Accept-Language" to "tr-TR,tr;q=0.9,en;q=0.8"
                )
            ).text
        } catch (e: Exception) {
            Log.e(name, "İstek hatası: ${e.message}")
            return
        }

        if (response.isBlank()) {
            Log.e(name, "Boş yanıt döndü")
            return
        }

        Log.d(name, "VidMoly HTML uzunluğu: ${response.length}")

        // 1) Doğrudan m3u8 linkini yakala
        val hlsRegexes = listOf(
            Regex("""sources\s*:\s*\[\s*\{\s*file\s*:\s*['"]([^'"]+)['"]"""),
            Regex("""file\s*:\s*['"](https?://[^'"]+\.m3u8[^'"]*)['"]"""),
            Regex("""file\s*:\s*['"](https?://[^'"]+urlset/master[^'"]*)['"]"""),
            Regex("""file\s*:\s*['"]([^'"]+\.m3u8[^'"]*)['"]"""),
            Regex("""(https?://[^"'\s]+\.m3u8[^"'\s]*)"""),
            Regex("""(https?://[^"'\s]+/master\.m3u8[^"'\s]*)""")
        )

        var hlsUrl: String? = null
        for (regex in hlsRegexes) {
            val match = regex.find(response) ?: continue
            val candidate = match.groupValues[1].trim()
            if (candidate.isNotBlank()) {
                hlsUrl = if (candidate.startsWith("//")) "https:$candidate" else candidate
                break
            }
        }

        // 2) eval / packer ile şifrelenmiş m3u8 linki var mı?
        if (hlsUrl == null) {
            val packed = unpackPacker(response)
            if (packed != null) {
                for (regex in hlsRegexes) {
                    val match = regex.find(packed) ?: continue
                    val candidate = match.groupValues[1].trim()
                    if (candidate.isNotBlank()) {
                        hlsUrl = if (candidate.startsWith("//")) "https:$candidate" else candidate
                        break
                    }
                }
            }
        }

        // 3) jwplayer setup içindeki sources'i manuel ayıkla (ek garanti)
        if (hlsUrl == null) {
            val sourcesBlock = Regex("""sources\s*:\s*(\[.*?\])""", RegexOption.DOT_MATCHES_ALL)
                .find(response)?.groupValues?.get(1)
            if (!sourcesBlock.isNullOrBlank()) {
                val m = Regex("""file\s*:\s*['"]([^'"]+)['"]""").find(sourcesBlock)
                if (m != null) {
                    val candidate = m.groupValues[1].trim()
                    hlsUrl = if (candidate.startsWith("//")) "https:$candidate" else candidate
                }
            }
        }

        if (hlsUrl.isNullOrBlank()) {
            Log.e(name, "m3u8 linki bulunamadı. HTML (ilk 1500 karakter):")
            Log.e(name, response.take(1500))
            return
        }

        Log.d(name, "HLS URL: $hlsUrl")

        // Kalite bilgisini linkten çıkar
        val quality = when {
            hlsUrl.contains("1080", true) -> Qualities.P1080.value
            hlsUrl.contains("720", true) -> Qualities.P720.value
            hlsUrl.contains("480", true) -> Qualities.P480.value
            hlsUrl.contains("360", true) -> Qualities.P360.value
            else -> Qualities.Unknown.value
        }

        callback.invoke(
            ExtractorLink(
                source = name,
                name = displayName,
                url = hlsUrl,
                referer = embedReferer,
                quality = quality,
                type = ExtractorLinkType.M3U8
            )
        )

        // 4) Altyazıları çek (varsa)
        try {
            val subtitleRegex = Regex(
                """tracks\s*:\s*\[(.*?)\]""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            )
            val tracksBlock = subtitleRegex.find(response)?.groupValues?.get(1)
            if (!tracksBlock.isNullOrBlank()) {
                val trackRegex = Regex(
                    """\{\s*file\s*:\s*['"]([^'"]+)['"][^}]*?label\s*:\s*['"]([^'"]+)['"]""",
                    RegexOption.IGNORE_CASE
                )
                trackRegex.findAll(tracksBlock).forEach { m ->
                    val subUrl = m.groupValues[1].trim()
                    val label = m.groupValues[2].trim()
                    if (subUrl.isNotBlank()) {
                        val fixedSub = if (subUrl.startsWith("//")) "https:$subUrl" else subUrl
                        subtitleCallback.invoke(
                            SubtitleFile(label, fixedSub)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(name, "Altyazı parse hatası: ${e.message}")
        }
    }

    /**
     * Dean Edwards Packer ile şifrelenmiş script'i çözer.
     * VidMoly bazen m3u8 linkini eval(function(p,a,c,k,e,d){...}) içinde gizler.
     */
    private fun unpackPacker(html: String): String? {
        return try {
            val packedRegex = Regex(
                """eval\(function\(p,a,c,k,e,d\)\{.*?\}\s*\(\s*['"](.*?)['"]\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*['"](.*?)['"]\.split\('\|'\)""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            )
            val m = packedRegex.find(html) ?: return null
            val payload = m.groupValues[1]
            val base = m.groupValues[2].toIntOrNull() ?: return null
            val k = m.groupValues[4].split('|')

            // decode: packed JS payload'unu çöz
            val sb = StringBuilder()
            for (ch in payload) {
                val idx = digitOf(ch, base)
                sb.append(if (idx >= 0 && idx < k.size && k[idx].isNotEmpty()) k[idx] else ch)
            }
            sb.toString()
        } catch (e: Exception) {
            Log.e(name, "unpackPacker hatası: ${e.message}")
            null
        }
    }

    private fun digitOf(c: Char, base: Int): Int {
        val code = c.code
        return when {
            code >= 48 && code <= 57 -> code - 48          // 0-9
            code >= 97 && code <= 122 -> code - 97 + 10    // a-z
            code >= 65 && code <= 90 -> code - 65 + 36     // A-Z
            else -> -1
        }.takeIf { it in 0 until base } ?: -1
    }
}
