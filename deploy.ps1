param(
    [string]$Message = "",
    [switch]$NoCache
)

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot

function Step($text) {
    Write-Host "`n===> $text" -ForegroundColor Cyan
}

function Ok($text) {
    Write-Host $text -ForegroundColor Green
}

function Fail($text) {
    Write-Host "ERROR: $text" -ForegroundColor Red
    exit 1
}

# ── 1. 프론트엔드 빌드 ──────────────────────────────────────────
Step "프론트엔드 빌드 (subway-web)"
Set-Location "$Root\subway-web"
npm install --silent
if ($LASTEXITCODE -ne 0) { Fail "npm install 실패" }
npm run build
if ($LASTEXITCODE -ne 0) { Fail "npm run build 실패" }
Ok "프론트엔드 빌드 완료"

# ── 2. Docker 이미지 재빌드 ─────────────────────────────────────
Step "Docker 이미지 빌드"
Set-Location $Root
if ($NoCache) {
    docker compose build --no-cache
} else {
    docker compose build
}
if ($LASTEXITCODE -ne 0) { Fail "docker compose build 실패" }
Ok "이미지 빌드 완료"

# ── 3. 컨테이너 재시작 ──────────────────────────────────────────
Step "컨테이너 재시작"
docker compose up -d
if ($LASTEXITCODE -ne 0) { Fail "docker compose up 실패" }
Ok "컨테이너 재시작 완료"

# ── 4. Git push ─────────────────────────────────────────────────
Step "Git push"
Set-Location $Root
git add .

$status = git status --porcelain
if ($status) {
    if (-not $Message) {
        $Message = "deploy: $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
    }
    git commit -m $Message
    if ($LASTEXITCODE -ne 0) { Fail "git commit 실패" }
    git push
    if ($LASTEXITCODE -ne 0) { Fail "git push 실패" }
    Ok "Git push 완료"
} else {
    Ok "변경사항 없음 - Git push 생략"
}

Write-Host "`n배포 완료!" -ForegroundColor Green
