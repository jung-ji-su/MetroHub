# MetroHub 로컬 개발 스크립트
# ─────────────────────────────────────────────────────────
# 인프라(MySQL, Kafka)는 Docker로 실행하고
# subway-api는 Gradle bootRun으로 직접 실행합니다.
#
# 장점: 코드 변경 후 재시작 ~5초 (Docker 빌드 2~3분 불필요)
#
# 사용법:
#   .\dev.ps1          → 인프라 시작 + API bootRun + 웹 dev 서버 안내
#   .\dev.ps1 -Api     → API만 재시작 (Ctrl+C 후 재실행)
#   .\dev.ps1 -Reset   → dev 계정 초기화 후 API 재시작
# ─────────────────────────────────────────────────────────

param(
    [switch]$Api,
    [switch]$Reset
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path

function Write-Header { param($msg) Write-Host "`n  $msg" -ForegroundColor Cyan }
function Write-Ok     { param($msg) Write-Host "  ✓ $msg" -ForegroundColor Green }
function Write-Info   { param($msg) Write-Host "    $msg" -ForegroundColor Gray }
function Write-Warn   { param($msg) Write-Host "  ! $msg" -ForegroundColor Yellow }

# ── Docker 확인 ────────────────────────────────────────────
docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ✗ Docker Desktop이 실행 중이지 않습니다." -ForegroundColor Red
    exit 1
}

# ── 인프라 시작 ────────────────────────────────────────────
if (-not $Api) {
    Write-Header "인프라 서비스 시작 (MySQL · Kafka · Zookeeper)"
    Set-Location $Root
    docker-compose up -d mysql kafka kafka-init zookeeper | Out-Null

    Write-Info "MySQL 준비 대기 중..."
    $retries = 0
    while ($retries -lt 30) {
        $ready = docker-compose exec -T mysql mysql -u metrohub -pmetrohub metrohub -e "SELECT 1" 2>&1
        if ($LASTEXITCODE -eq 0) { break }
        Start-Sleep -Seconds 3
        $retries++
    }
    Write-Ok "인프라 준비 완료"
}

# ── dev 계정 초기화 (--Reset 옵션) ─────────────────────────
if ($Reset) {
    Write-Header "dev 관리자 계정 초기화"
    $result = docker-compose exec -T mysql mysql -u root -proot metrohub -e "DELETE FROM users WHERE nickname='dev';" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Ok "dev 계정 삭제 완료 — API 재시작 시 dev/1234 로 자동 재생성됩니다"
    } else {
        Write-Warn "dev 계정 삭제 실패 (이미 없거나 DB 연결 오류): $result"
    }
}

# ── subway-api bootRun ─────────────────────────────────────
Write-Header "subway-api 시작 (Gradle bootRun)"
Write-Info "코드 변경 후: Ctrl+C → .\dev.ps1 -Api 로 재시작"
Write-Info ""
Write-Info "  subway-web:  cd subway-web && npm run dev  →  http://localhost:5173"
Write-Info "  subway-api:  http://localhost:8080"
Write-Info ""

Set-Location "$Root\subway-api"

# JAVA_OPTS에 빠른 시작 옵션 추가 (JVM 워밍업 생략)
$env:GRADLE_OPTS = "-Dorg.gradle.daemon=true -Dorg.gradle.parallel=true"

.\gradlew.bat bootRun
