param(
    [string] $BackendUrl = "http://localhost:8080",
    [string] $OutputPath = "docs/api/openapi.yaml"
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$resolvedOutputPath = if ([IO.Path]::IsPathRooted($OutputPath)) {
    $OutputPath
} else {
    Join-Path $repositoryRoot $OutputPath
}
$outputDirectory = Split-Path -Parent $resolvedOutputPath
$temporaryPath = Join-Path ([IO.Path]::GetTempPath()) ("mamxanh-openapi-{0}.yaml" -f [guid]::NewGuid())
$openApiUrl = "{0}/v3/api-docs.yaml" -f $BackendUrl.TrimEnd('/')

try {
    Invoke-WebRequest -Uri $openApiUrl -OutFile $temporaryPath -TimeoutSec 30

    $yaml = Get-Content -LiteralPath $temporaryPath -Raw
    if ([string]::IsNullOrWhiteSpace($yaml) -or $yaml -notmatch '(?m)^openapi:\s*') {
        throw "Springdoc response from '$openApiUrl' is empty or is not an OpenAPI YAML document."
    }

    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
    Move-Item -LiteralPath $temporaryPath -Destination $resolvedOutputPath -Force
    Write-Output "Generated $resolvedOutputPath from $openApiUrl"
}
finally {
    if (Test-Path -LiteralPath $temporaryPath) {
        Remove-Item -LiteralPath $temporaryPath -Force
    }
}
