param(
    [switch]$SkipComposeUp,
    [switch]$KeepRunning
)

$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
$composeProject = "mamxanh-ai-e2e"
$envFile = Join-Path $root "app/mamxanh-backend/.env"
$isolatedEnv = Join-Path $root ".env.ai-e2e"
$composeFile = Join-Path $root "docker-compose.yml"

$outputDir = Join-Path $root ".agents/outputs/ai-acceptance/e2e-live"
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
}

$env:MAMXANX_E2E_FRONTEND_URL = "http://localhost:15174"
$env:MAMXANX_E2E_BACKEND_URL = "http://localhost:18084/api/v1"
$env:MAMXANX_E2E_COMPOSE_PROJECT = $composeProject
$env:MAMXANH_E2E_OUTPUT_DIR = $outputDir
$env:MAMXANH_AI_ORCHESTRATOR = "ADK"

$composeArgs = @("-p", $composeProject)
if (Test-Path $envFile) {
    $composeArgs += @("--env-file", $envFile)
}
if (Test-Path $isolatedEnv) {
    $composeArgs += @("--env-file", $isolatedEnv)
}
$composeArgs += @("-f", $composeFile)

function Wait-BackendHealth {
    param([int]$MaxAttempts = 40, [int]$DelaySeconds = 3)
    $url = "http://localhost:18084/v3/api-docs"
    Write-Host "Kiem tra backend tai $url..."
    for ($i = 1; $i -le $MaxAttempts; $i++) {
        try {
            $resp = Invoke-WebRequest -Uri $url -Method Get -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
            if ($resp.StatusCode -eq 200) {
                Write-Host "Backend san sang (HTTP 200)!"
                return $true
            }
        } catch {
            # cho khoi dong tiep
        }
        Start-Sleep -Seconds $DelaySeconds
    }
    Write-Warning "Backend health check timeout sau $($MaxAttempts * $DelaySeconds) giay."
    return $false
}

$origLocation = Get-Location
try {
    if (-not $SkipComposeUp) {
        Write-Host "Khoi dong Docker Compose stack cho project '$composeProject'..."
        & docker compose @composeArgs up -d --build
        if (-not (Wait-BackendHealth)) {
            throw "Backend khong the khoi dong thanh cong."
        }
    }

    Write-Host "Chay Playwright E2E AI Assistant Acceptance Tests..."
    Set-Location (Join-Path $root "app/mamxanh-frontend")
    & npx playwright test tests/integration/ai-assistant-acceptance.spec.ts --config playwright.integration.config.ts
} finally {
    Set-Location $origLocation
    if (-not $KeepRunning -and -not $SkipComposeUp) {
        Write-Host "Dung Docker Compose stack (bao toan volume)..."
        & docker compose @composeArgs down
    }
}
