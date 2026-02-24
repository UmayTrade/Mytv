package com.umaytrade

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class BelgeselXPlugin: Plugin() {
    override fun load() {
        registerMainAPI(BelgeselX())
    }
}
