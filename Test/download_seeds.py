# Script de descarga y descifrado de semillas y tokens de Flow
# Emula de forma 100% real el comportamiento de la app Ultra TV

import re
import urllib.request
import urllib.parse
from sys import exit

# Configuración idéntica a PremiumConfig / worldtv6.json
DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36"

# URLs semilla oficiales y locales (fallbacks del código de la app)
FALLBACK_SEEDS = [
    "https://chromecast.cvattv.com.ar/live/c6eds/Viajar/SA_Live_dash_cenc/Viajar.mpd",
    "https://cdn-py.cvattv.com.ar/live/c6eds/EWTN/SA_Live_dash_enc/EWTN.mpd",
    "https://cdn-py.cvattv.com.ar/live/c4eds/UNICANAL_C4/SA_Live_dash_enc/UNICANAL_C4.mpd",
    "https://cdn-py.cvattv.com.ar/live/c4eds/TELEFUTURO_C4/SA_Live_dash_enc/TELEFUTURO_C4.mpd"
]

# Canales Premium de prueba (obtenidos del catálogo de la app)
TEST_CHANNELS = {
    "1": {
        "name": "Telefe HD",
        "url": "https://flow-cdn-gcp.app.flow.com.ar/live/c6eds/TelefeHD/SA_Live_dash_enc/TelefeHD.mpd"
    },
    "2": {
        "name": "El Trece HD",
        "url": "https://flow-cdn-gcp.app.flow.com.ar/live/c3eds/ArtearHD/SA_Live_dash_enc/ArtearHD.mpd"
    },
    "3": {
        "name": "America TV",
        "url": "https://flow-cdn-gcp.app.flow.com.ar/live/c3eds/AmericaTV/SA_Live_dash_enc/AmericaTV.mpd"
    },
    "4": {
        "name": "El Nueve",
        "url": "https://flow-cdn-gcp.app.flow.com.ar/live/c3eds/Canal9/SA_Live_dash_enc/Canal9.mpd"
    }
}

# Clase para no seguir redirecciones de forma manual (como followRedirects(false) en OkHttp)
class NoRedirectHandler(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # Evita la redirección automática devolviendo None
        return None

def probe_seed(seed_url):
    print(f"[*] Sondeando canal semilla: {seed_url}")
    opener = urllib.request.build_opener(NoRedirectHandler)
    urllib.request.install_opener(opener)
    
    req = urllib.request.Request(seed_url)
    req.add_header("User-Agent", DEFAULT_USER_AGENT)
    
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            # Obtener cabecera Location
            location = response.headers.get("Location")
            code = response.status
            print(f"   [->] Respuesta HTTP {code}. Redirección (Location): {location}")
            
            if location:
                # Extraer host y token usando expresiones regulares (igual que la app)
                match_tok = re.search(r"/(tok_[^/]+)", location)
                if match_tok:
                    token = match_tok.group(1)
                    parsed_url = urllib.parse.urlparse(location)
                    host = parsed_url.netloc
                    return {"host": host, "token": token}
    except Exception as e:
         print(f"   [-] Error en semilla: {e}")
    return None

def extract_relative_path(url):
    # Emula exactamente la regex PATH_PATTERN de la app
    match = re.search(r"(live/c\d+eds/[^?#]*)", url)
    if match:
        return match.group(1)
    
    # Emula PATH_PATTERN_ALT
    match_alt = re.search(r"(c\d+eds/[^?#]*)", url)
    if match_alt:
        return "live/" + match_alt.group(1)
    return None

def resolve_playable_url(url, token_info):
    rel_path = extract_relative_path(url)
    if not rel_path:
        return None
    clean_path = rel_path.lstrip('/')
    # Reconstruye la URL firmada usando el Host y Token de la semilla
    return f"https://{token_info['host']}/{token_info['token']}/{clean_path}"

def download_manifest(signed_url):
    print(f"\n[*] Descargando manifiesto firmado de Flow...")
    print(f"    URL Firmada: {signed_url}")
    
    # Para la descarga real sí seguimos redirecciones
    opener = urllib.request.build_opener()
    urllib.request.install_opener(opener)
    
    req = urllib.request.Request(signed_url)
    req.add_header("User-Agent", DEFAULT_USER_AGENT)
    req.add_header("referer", "https://portal.app.flow.com.ar/")
    req.add_header("Origin", "https://portal.app.flow.com.ar")
    
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            content = response.read().decode('utf-8')
            print(f"[+] Conexión Exitosa (HTTP {response.status})")
            return content
    except Exception as e:
        print(f"[-] Error al descargar manifiesto de Flow: {e}")
        return None

def main():
    print("=== Emulador de Firma de Canales Flow (Ultra TV) ===")
    
    # 1. Obtener Token dinámico desde los canales semilla
    token_info = None
    for seed in FALLBACK_SEEDS:
        token_info = probe_seed(seed)
        if token_info:
            print(f"\n[+] ¡Token Obtenido Exitosamente!")
            print(f"    CDN Host: {token_info['host']}")
            print(f"    Token JWT: {token_info['token']}")
            break
            
    if not token_info:
        print("[-] Error: Ningún canal semilla respondió correctamente. Abortando.")
        exit(1)
        
    # 2. Elegir canal premium a probar
    print("\nElige un canal premium para firmar y descargar:")
    for k, v in TEST_CHANNELS.items():
        print(f"  {k}. {v['name']}")
    
    choice = input("\nSelección (1-4): ").strip()
    if choice not in TEST_CHANNELS:
        print("[-] Selección inválida.")
        exit(1)
        
    selected_ch = TEST_CHANNELS[choice]
    
    # 3. Firmar la URL
    signed_url = resolve_playable_url(selected_ch["url"], token_info)
    if not signed_url:
        print("[-] Error al procesar la ruta relativa del canal.")
        exit(1)
        
    # 4. Descargar el manifiesto real (demostración de bypass en vivo)
    manifest_content = download_manifest(signed_url)
    
    if manifest_content:
        # Guardar en archivo TXT
        output_file = "flow_manifest.xml"
        try:
            with open(output_file, "w", encoding="utf-8") as f:
                f.write(manifest_content)
            print(f"\n[+] Manifiesto guardado con éxito en '{output_file}'")
            print("    ¡El bypass ha funcionado de forma idéntica a la app!")
        except Exception as e:
            print(f"[-] Error al guardar el archivo: {e}")

if __name__ == "__main__":
    main()
