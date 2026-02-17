package com.keyiflerolsun

import com.fasterxml.jackson.annotation.JsonProperty

data class SearchItem(
    @JsonProperty("id") val id: String?,
    @JsonProperty("title") val title: String?,
    @JsonProperty("poster") val poster: String?,
    @JsonProperty("type") val type: String?,
    @JsonProperty("url") val url: String?
)
