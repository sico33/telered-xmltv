# Script de PowerShell para emular el bypass de tokens de Flow (Ultra TV)
# Realiza la sonda, la firma de la URL y descarga el manifiesto del canal premium

$DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36"
$FALLBACK_SEEDS = @(
    "https://chromecast.cvattv.com.ar/live/c6eds/Viajar/SA_Live_dash_cenc/Viajar.mpd",
    "https://cdn-py.cvattv.com.ar/live/c6eds/EWTN/SA_Live_dash_enc/EWTN.mpd",
    "https://cdn-py.cvattv.com.ar/live/c4eds/UNICANAL_C4/SA_Live_dash_enc/UNICANAL_C4.mpd",
    "https://cdn-py.cvattv.com.ar/live/c4eds/TELEFUTURO_C4/SA_Live_dash_enc/TELEFUTURO_C4.mpd"
)

$TEST_CHANNELS = @{
    "1" = @{ name = "Telefe HD"; url = "https://flow-cdn-gcp.app.flow.com.ar/live/c6eds/TelefeHD/SA_Live_dash_enc/TelefeHD.mpd" }
    "2" = @{ name = "El Trece HD"; url = "https://flow-cdn-gcp.app.flow.com.ar/live/c3eds/ArtearHD/SA_Live_dash_enc/ArtearHD.mpd" }
    "3" = @{ name = "America TV"; url = "https://flow-cdn-gcp.app.flow.com.ar/live/c3eds/AmericaTV/SA_Live_dash_enc/AmericaTV.mpd" }
    "4" = @{ name = "El Nueve"; url = "https://flow-cdn-gcp.app.flow.com.ar/live/c3eds/Canal9/SA_Live_dash_enc/Canal9.mpd" }
}

Write-Host "=== Emulador de Firma de Canales Flow (PowerShell) ===" -ForegroundColor Cyan

# 1. Obtener Token dinámico desde los canales semilla
$tokenInfo = $null

foreach ($seed in $FALLBACK_SEEDS) {
    Write-Host "[*] Sondeando canal semilla: $seed" -ForegroundColor Gray
    try {
        # Configurar petición HTTP sin seguir redirecciones (followRedirects = false)
        $request = [System.Net.HttpWebRequest]::Create($seed)
        $request.UserAgent = $DEFAULT_USER_AGENT
        $request.AllowAutoRedirect = $false
        $request.Timeout = 10000

        $response = $request.GetResponse()
        $location = $response.Headers["Location"]
        $response.Close()

        Write-Host "   [->] Respuesta HTTP $([int]$response.StatusCode). Redirección (Location): $location"

        if ($location) {
            # Extraer host y token usando regex
            if ($location -match "/(tok_[^/]+)") {
                $token = $Matches[1]
                $uri = [System.Uri]::new($location)
                $hostName = $uri.Host
                $tokenInfo = @{ host = $hostName; token = $token }
                break
            }
        }
    }
    catch {
        Write-Host "   [-] Error en semilla: $_" -ForegroundColor Red
    }
}

if (-not $tokenInfo) {
    Write-Host "[-] Error: Ningún canal semilla respondió correctamente. Abortando." -ForegroundColor Red
    exit
}

Write-Host "`n[+] ¡Token Obtenido Exitosamente!" -ForegroundColor Green
Write-Host "    CDN Host: $($tokenInfo.host)" -ForegroundColor Green
Write-Host "    Token JWT: $($tokenInfo.token)" -ForegroundColor Green

# 2. Elegir canal premium a probar
Write-Host "`nElige un canal premium para firmar y descargar:" -ForegroundColor Yellow
Write-Host "  1. Telefe HD"
Write-Host "  2. El Trece HD"
Write-Host "  3. America TV"
Write-Host "  4. El Nueve"

$choice = Read-Host "`nSelección (1-4)"
$choice = $choice.Trim()

if (-not $TEST_CHANNELS.ContainsKey($choice)) {
    Write-Host "[-] Selección inválida." -ForegroundColor Red
    exit
}

$selectedCh = $TEST_CHANNELS[$choice]

# 3. Firmar la URL
$relPath = $null
if ($selectedCh.url -match "(live/c\d+eds/[^?#]*)") {
    $relPath = $Matches[1]
}
elseif ($selectedCh.url -match "(c\d+eds/[^?#]*)") {
    $relPath = "live/" + $Matches[1]
}

if (-not $relPath) {
    Write-Host "[-] Error al procesar la ruta relativa del canal." -ForegroundColor Red
    exit
}

$cleanPath = $relPath.TrimStart('/')
$signedUrl = "https://$($tokenInfo.host)/$($tokenInfo.token)/$cleanPath"

Write-Host "`n[*] Descargando manifiesto firmado de Flow..." -ForegroundColor Gray
Write-Host "    URL Firmada: $signedUrl" -ForegroundColor Gray

# 4. Descargar el manifiesto real (demostración de bypass en vivo)
try {
    $webClient = New-Object System.Net.WebClient
    $webClient.Headers.Add("User-Agent", $DEFAULT_USER_AGENT)
    $webClient.Headers.Add("referer", "https://portal.app.flow.com.ar/")
    $webClient.Headers.Add("Origin", "https://portal.app.flow.com.ar")
    
    $manifestContent = $webClient.DownloadString($signedUrl)
    Write-Host "[+] Conexión Exitosa (HTTP 200 OK)" -ForegroundColor Green

    # Guardar en archivo XML/TXT
    $outputFile = Join-Path $PSScriptRoot "flow_manifest.xml"
    $manifestContent | Out-File -FilePath $outputFile -Encoding utf8
    
    Write-Host "`n[+] Manifiesto guardado con éxito en: $outputFile" -ForegroundColor Green
    Write-Host "    ¡El bypass ha funcionado de forma idéntica a la app!" -ForegroundColor Green
}
catch {
    Write-Host "[-] Error al descargar manifiesto de Flow: $_" -ForegroundColor Red
}
