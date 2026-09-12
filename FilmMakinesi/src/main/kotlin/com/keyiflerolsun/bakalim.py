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

film_link = "https://filmmakinesi.to/film/haydi-takim-2026/"

istek  = oturum.get(film_link)
secici = Selector(istek.text)

# Iframe URL'sini al — div.after-player içindeki iframe
iframe = secici.css("div.after-player iframe::attr(data-src)").get()
konsol.print(f"Iframe: {iframe}")

if not iframe:
    konsol.print("[red]Iframe bulunamadı![/red]")
    exit(1)

# Iframe sayfasını al
i_source   = oturum.get(iframe, headers={"Referer": film_link})
i_selector = Selector(i_source.text)

# dc_hello base64 string'ini ara
b64_str = None
for script in i_selector.css("script").getall():
    if "dc_hello" in script:
        match = findall(r'dc_hello\(\s*["\']([^"\']+)["\']\s*\)', script)
        if match:
            b64_str = match[0]
            konsol.print(f"Base64 bulundu: {b64_str}")
            break

if not b64_str:
    konsol.print("[red]dc_hello base64 bulunamadı![/red]")
    exit(1)

# Base64 decode (ters çevir → tekrar decode)
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

konsol.print(f"[green]M3U Link: {m3u_link}[/green]")

# Altyazıları çek
for track in i_selector.css("track"):
    label = track.css("::attr(label)").get()
    src   = track.css("::attr(src)").get()
    konsol.print(f"Altyazı: {label} | {src}")
