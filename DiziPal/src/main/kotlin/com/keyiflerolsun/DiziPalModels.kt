package com.keyiflerolsun

import com.fasterxml.jackson.annotation.JsonProperty

data class DiziPalMainResponse(
    @JsonProperty("data") val data: List<DiziPalItem>? = null
)

data class DiziPalItem(
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("url") val url: String? = null,
    @JsonProperty("poster") val poster: String? = null
)
