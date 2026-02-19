package com.nikyokki

import com.fasterxml.jackson.annotation.JsonProperty

data class Cipher(
    @JsonProperty("ct") val ct: String,
    @JsonProperty("iv") val iv: String,
    @JsonProperty("s") val s: String
)

data class JsonData(
    @JsonProperty("data") val data: String
)
