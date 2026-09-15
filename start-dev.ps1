#!/usr/bin/env pwsh
# =============================================================================
# start-dev.ps1 — Levanta el stack completo para desarrollo local
# Uso: .\start-dev.ps1 [-NgrokToken "tu_token_aqui"]
# =============================================================================

param(
    [string]$NgrokToken = ""
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  SaaS Platform — Arranque de desarrollo" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar Docker
Write-Host "[ 1/4 ] Verificando Docker..." -ForegroundColor Yellow
try {
    docker info 2>&1 | Out-Null
    Write-Host "        Docker OK" -ForegroundColor Green
} catch {
    Write-Host "" 
    Write-Host "  ERROR: Docker Desktop no está corriendo." -ForegroundColor Red
    Write-Host "  Abre Docker Desktop y vuelve a ejecutar este script." -ForegroundColor Red
    Write-Host ""
    exit 1
}

# 2. Levantar PostgreSQL (solo el contenedor de BD)
Write-Host "[ 2/4 ] Levantando base de datos (PostgreSQL)..." -ForegroundColor Yellow
Set-Location $ProjectRoot
docker compose -f docker-compose.dev.yml up -d postgres 2>&1 | Out-Null
Start-Sleep -Seconds 3

# Esperar a que Postgres esté listo
$retries = 0
do {
    Start-Sleep -Seconds 2
    $retries++
    $pgReady = docker compose -f docker-compose.dev.yml exec postgres pg_isready -U saasplatform 2>&1
} while ($pgReady -notmatch "accepting" -and $retries -lt 10)

if ($retries -ge 10) {
    Write-Host "  ERROR: PostgreSQL no respondió a tiempo" -ForegroundColor Red
    exit 1
}
Write-Host "        PostgreSQL listo" -ForegroundColor Green

# 3. Levantar el backend Quarkus en modo dev
Write-Host "[ 3/4 ] Iniciando backend Quarkus (puerto 8080)..." -ForegroundColor Yellow
$backendPath = Join-Path $ProjectRoot "backend"
$backendJob = Start-Job -ScriptBlock {
    param($path)
    Set-Location $path
    mvn quarkus:dev -q 2>&1
} -ArgumentList $backendPath

# Esperar a que el backend esté listo
Write-Host "        Esperando que Quarkus arranque..." -ForegroundColor DarkYellow
$maxWait = 60
$waited = 0
do {
    Start-Sleep -Seconds 2
    $waited += 2
    try {
        $health = Invoke-WebRequest -Uri "http://localhost:8080/q/health" -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($health.StatusCode -eq 200) { break }
    } catch {}
} while ($waited -lt $maxWait)

if ($waited -ge $maxWait) {
    Write-Host "  ADVERTENCIA: Backend tardando más de lo esperado..." -ForegroundColor Yellow
    Write-Host "  Verifica logs con: mvn quarkus:dev en .\backend\" -ForegroundColor Yellow
} else {
    Write-Host "        Backend listo en http://localhost:8080" -ForegroundColor Green
}

# 4. Levantar ngrok
Write-Host "[ 4/4 ] Iniciando ngrok tunnel (puerto 8080)..." -ForegroundColor Yellow
$env:PATH = [System.Environment]::GetEnvironmentVariable("PATH", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH", "User")

if ($NgrokToken -ne "") {
    ngrok config add-authtoken $NgrokToken 2>&1 | Out-Null
}

# Obtener URL de ngrok via API local
$ngrokJob = Start-Job -ScriptBlock {
    $env:PATH = [System.Environment]::GetEnvironmentVariable("PATH", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH", "User")
    ngrok http 8080 --log=stdout 2>&1
}

Start-Sleep -Seconds 4

$ngrokUrl = ""
try {
    $tunnels = Invoke-RestMethod -Uri "http://localhost:4040/api/tunnels" -ErrorAction SilentlyContinue
    $ngrokUrl = ($tunnels.tunnels | Where-Object { $_.proto -eq "https" } | Select-Object -First 1).public_url
} catch {}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "  Stack listo!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Dashboard:    http://localhost:3000" -ForegroundColor White
Write-Host "  Backend:      http://localhost:8080" -ForegroundColor White
Write-Host "  Swagger UI:   http://localhost:8080/q/swagger-ui" -ForegroundColor White
if ($ngrokUrl -ne "") {
    Write-Host ""
    Write-Host "  Webhook URL para Meta:" -ForegroundColor Cyan
    Write-Host "  $ngrokUrl/api/v1/whatsapp/webhook" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  Token de verificacion: dev_verify_token" -ForegroundColor Cyan
}
Write-Host ""
Write-Host "  Presiona Ctrl+C para detener" -ForegroundColor DarkGray
Write-Host ""

# Mantener vivo
try {
    Wait-Job $backendJob, $ngrokJob
} finally {
    Stop-Job $backendJob, $ngrokJob -ErrorAction SilentlyContinue
    Remove-Job $backendJob, $ngrokJob -ErrorAction SilentlyContinue
}
