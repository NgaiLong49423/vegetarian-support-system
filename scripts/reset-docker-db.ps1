param(
    [string]$ProjectName = 'mamxanh-dev'
)

$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repositoryRoot 'app/mamxanh-backend/.env'
$composeFile = Join-Path $repositoryRoot 'docker-compose.yml'
$composeArgs = @('compose', '-f', $composeFile, '-p', $ProjectName, '--env-file', $envFile)

if (-not (Test-Path -LiteralPath $envFile)) {
    throw 'Không tìm thấy app/mamxanh-backend/.env. Hãy sao chép .env.example thành .env tại cùng thư mục và đặt mật khẩu SQL Server trước.'
}

Write-Host "CẢNH BÁO: thao tác này xóa vĩnh viễn SQL Server volume của Compose project '$ProjectName' và toàn bộ database trong volume đó." -ForegroundColor Yellow
Write-Host 'Dữ liệu sẽ được tạo lại từ Flyway và database/sample-data.sql.'
$confirmation = Read-Host 'Nhập RESET để tiếp tục'
if ($confirmation -cne 'RESET') {
    Write-Host 'Đã hủy; dữ liệu không bị thay đổi.'
    exit 0
}

Push-Location $repositoryRoot
try {
    $configJson = & docker @composeArgs config --format json
    if ($LASTEXITCODE -ne 0) { throw 'Không thể đọc cấu hình Docker Compose để xác định volume database.' }
    $composeConfig = $configJson | ConvertFrom-Json
    $volumeName = $composeConfig.volumes.'sqlserver-data'.name
    if ([string]::IsNullOrWhiteSpace($volumeName)) { throw 'Không xác định được volume SQL Server từ cấu hình Compose.' }

    & docker @composeArgs down --remove-orphans
    if ($LASTEXITCODE -ne 0) { throw 'Không thể dừng Docker Compose.' }

    $existingVolume = docker volume ls --quiet --filter "name=^$volumeName$"
    if ($existingVolume -eq $volumeName) {
        docker volume rm $volumeName
        if ($LASTEXITCODE -ne 0) { throw 'Không thể xóa volume SQL Server.' }
    }

    & docker @composeArgs up --build --detach --wait --wait-timeout 600
    if ($LASTEXITCODE -ne 0) { throw 'Không thể khởi động lại Docker Compose.' }
}
finally {
    Pop-Location
}
