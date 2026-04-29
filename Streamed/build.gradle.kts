// ! Bu araç @SAKLImavi tarafından | @UmayTrade için yazılmıştır.
version = 1

cloudstream {
    authors     = listOf("UmayTrade")
    language    = "en"
    description = "Streamed ile Canlı Spor Yayınlarını İzleyebilirsiniz."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Live")
    iconUrl = "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://streamed.pk/&size=128"
}
