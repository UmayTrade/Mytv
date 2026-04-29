// ! Bu araç @SAKLImavi tarafından | @UmayTrade için yazılmıştır.
version = 1

cloudstream {
    authors     = listOf("UmayTrade")
    language    = "en"
    description = "Cinema City."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie", "TvSeries", "Cartoon")
    iconUrl = "https://i.imgur.com/A87j6ue.png"
}
