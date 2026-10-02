$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repositoryRoot '.env.docker'

if (-not (Test-Path -LiteralPath $envFile)) {
    throw 'Không tìm thấy .env.docker. Hãy sao chép .env.docker.example thành .env.docker và đặt mật khẩu SQL Server trước.'
}

Write-Host 'CẢNH BÁO: thao tác này xóa vĩnh viễn database Docker và toàn bộ dữ liệu trên máy này.' -ForegroundColor Yellow
Write-Host 'Dữ liệu sẽ được tạo lại từ Flyway và database/sample-data.sql.'
$confirmation = Read-Host 'Nhập RESET để tiếp tục'
if ($confirmation -cne 'RESET') {
    Write-Host 'Đã hủy; dữ liệu không bị thay đổi.'
    exit 0
}

Push-Location $repositoryRoot
try {
    $composeConfig = docker compose --env-file $envFile config --format json | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0) { throw 'Không thể đọc cấu hình Docker Compose để xác định volume database.' }
    $volumeName = $composeConfig.volumes.'sqlserver-data'.name
    if ([string]::IsNullOrWhiteSpace($volumeName)) { throw 'Không xác định được volume SQL Server từ cấu hình Compose.' }

    docker compose --env-file $envFile down --remove-orphans
    if ($LASTEXITCODE -ne 0) { throw 'Không thể dừng Docker Compose.' }

    $existingVolume = docker volume ls --quiet --filter "name=^$volumeName$"
    if ($existingVolume -eq $volumeName) {
        docker volume rm $volumeName
        if ($LASTEXITCODE -ne 0) { throw 'Không thể xóa volume SQL Server.' }
    }

    docker compose --env-file $envFile up --build --detach
    if ($LASTEXITCODE -ne 0) { throw 'Không thể khởi động lại Docker Compose.' }
}
finally {
    Pop-Location
}
