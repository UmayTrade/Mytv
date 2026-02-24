package com.umaytrade

import com.fasterxml.jackson.annotation.JsonProperty

data class BelgeselIzleseneMainResponse(
    @JsonProperty("data") val data: List<BelgeselIzleseneItem>? = null
)

data class BelgeselXItem(
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("url") val url: String? = null,
    @JsonProperty("poster") val poster: String? = null
)