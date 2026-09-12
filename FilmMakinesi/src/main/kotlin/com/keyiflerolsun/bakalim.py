# ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

from Kekik.cli    import konsol
from cloudscraper import CloudScraper
from parsel       import Selector
from re           import findall
from base64       import b64decode

oturum = CloudScraper()
oturum.headers.update({
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Referer": "https://filmmakinesi.to/"
})

film_link = "https://filmmakinesi.to/film/band-of-brothers-legacy-2026/"

istek = oturum.get(film_link)
secici = Selector(istek.text)

# ! DÜZELTME: iframe, div.after-player içinde ve data-src attribute'unda
iframe = secici.css("div.after-player iframe::attr(data-src)").get()
konsol.print(f"Iframe: {iframe}")

if not iframe:
    konsol.print("[red]Iframe bulunamadı![/red]")
    exit(1)

i_source = oturum.get(iframe, headers={"Referer": film_link})
i_selector = Selector(i_source.text)

# ! DÜZELTME: dc_hello fonksiyonunu ara
m3u_link = None
for script in i_selector.css("script[type=text/javascript]").getall():
    if "dc_hello" in script:
        match = findall(r'dc_hello\("([^"]+)"\)', script)
        if match:
            b64_str = match[0]
            konsol.print(f"Base64: {b64_str}")
            # Base64 decode işlemi
            if padding_needed := len(b64_str) % 4:
                b64_str += "=" * (4 - padding_needed)
            first_decode = b64decode(b64_str).decode("utf-8")
            konsol.print(f"First decode: {first_decode}")
            reversed_str = first_decode[::-1]
            konsol.print(f"Reversed: {reversed_str}")
            if padding_needed := len(reversed_str) % 4:
                reversed_str += "=" * (4 - padding_needed)
            second_decode = b64decode(reversed_str).decode("utf-8")
            konsol.print(f"Second decode: {second_decode}")
            if "|" in second_decode:
                m3u_link = second_decode.split("|")[1]
            elif "+" in second_decode:
                m3u_link = second_decode.split("+")[-1]
            else:
                m3u_link = second_decode
            break

konsol.print(f"[green]M3U Link: {m3u_link}[/green]")

# Altyazıları çek
for track in i_selector.css("track"):
    label = track.css("::attr(label)").get()
    src   = track.css("::attr(src)").get()
    konsol.print(f"Altyazı: {label} | {src}")
