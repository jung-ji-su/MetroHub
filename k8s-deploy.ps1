# MetroHub — Docker Desktop Kubernetes 배포 스크립트
# 사용법:
#   .\k8s-deploy.ps1 -SeoulApiKey "your-key"   # 최초 배포
#   .\k8s-deploy.ps1 -SkipBuild                # 이미지 빌드 생략 (이미 빌드된 경우)
#   .\k8s-deploy.ps1 -CleanInstall             # 네임스페이스 삭제 후 재설치

param(
    [string]$SeoulApiKey  = $env:SEOUL_API_KEY,
    [switch]$SkipBuild,
    [switch]$CleanInstall
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

# ── 색상 헬퍼 ────────────────────────────────────────────────────────────────
function Write-Step  { param($msg) Write-Host "`n▶  $msg" -ForegroundColor Cyan }
function Write-Ok    { param($msg) Write-Host "  ✓  $msg" -ForegroundColor Green }
function Write-Warn  { param($msg) Write-Host "  !  $msg" -ForegroundColor Yellow }
function Write-Info  { param($msg) Write-Host "     $msg" -ForegroundColor Gray }
function Write-Fail  { param($msg) Write-Host "  ✗  $msg" -ForegroundColor Red; exit 1 }

Write-Host ""
Write-Host "╔══════════════════════════════════════╗" -ForegroundColor Magenta
Write-Host "║   MetroHub  Kubernetes  배포          ║" -ForegroundColor Magenta
Write-Host "╚══════════════════════════════════════╝" -ForegroundColor Magenta
Write-Host "  시작: $(Get-Date -Format 'HH:mm:ss')" -ForegroundColor DarkGray

# ── 1. 사전 조건 확인 ────────────────────────────────────────────────────────
Write-Step "사전 조건 확인"

# Docker 실행 확인
docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) { Write-Fail "Docker Desktop이 실행 중이지 않습니다." }
Write-Ok "Docker Desktop 실행 중"

# kubectl 컨텍스트 확인
$ctx = kubectl config current-context 2>&1
if ($ctx -ne "docker-desktop") {
    Write-Warn "현재 컨텍스트: $ctx  →  docker-desktop 으로 전환합니다."
    kubectl config use-context docker-desktop
    if ($LASTEXITCODE -ne 0) {
        Write-Fail "docker-desktop 컨텍스트 전환 실패.`nDocker Desktop → Settings → Kubernetes → Enable Kubernetes 후 재시도하세요."
    }
}
Write-Ok "kubectl 컨텍스트: docker-desktop"

# Seoul API Key 확인
if (-not $SeoulApiKey) {
    Write-Warn "SeoulApiKey가 없습니다. collector가 실행되어도 데이터 수집이 안 됩니다."
    Write-Info "다음 명령으로 재실행하세요:"
    Write-Info "  .\k8s-deploy.ps1 -SeoulApiKey `"your-key`""
    $SeoulApiKey = "dummy-key"
}
Write-Ok "Seoul API Key 설정됨"

# ── 2. Nginx Ingress Controller 설치 ────────────────────────────────────────
Write-Step "Nginx Ingress Controller 확인"

$ingressNs = kubectl get namespace ingress-nginx --ignore-not-found 2>&1
if ($ingressNs -notmatch "ingress-nginx") {
    Write-Info "Nginx Ingress Controller 설치 중..."
    kubectl apply -f "https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.1/deploy/static/provider/cloud/deploy.yaml"
    Write-Info "Ingress Controller 기동 대기 중 (최대 90초)..."
    kubectl wait --namespace ingress-nginx `
        --for=condition=ready pod `
        --selector=app.kubernetes.io/component=controller `
        --timeout=90s
    Write-Ok "Nginx Ingress Controller 설치 완료"
} else {
    Write-Ok "Nginx Ingress Controller 이미 설치됨"
}

# ── 3. Docker 이미지 로컬 빌드 ──────────────────────────────────────────────
if (-not $SkipBuild) {
    Write-Step "Docker 이미지 빌드 (처음 빌드 시 10~15분 소요)"

    $images = @(
        @{ name = "subway-api";          tag = "ghcr.io/jung-ji-su/metrohub-api:latest" },
        @{ name = "subway-notification"; tag = "ghcr.io/jung-ji-su/metrohub-notification:latest" },
        @{ name = "subway-collector";    tag = "ghcr.io/jung-ji-su/metrohub-collector:latest" },
        @{ name = "subway-web";          tag = "ghcr.io/jung-ji-su/metrohub-web:latest" }
    )

    foreach ($img in $images) {
        Write-Info "$($img.name) 빌드 중..."
        $start = Get-Date
        docker build -t $img.tag "./$($img.name)"
        if ($LASTEXITCODE -ne 0) { Write-Fail "$($img.name) 빌드 실패" }
        $sec = [int](New-TimeSpan -Start $start -End (Get-Date)).TotalSeconds
        Write-Ok "$($img.name) 빌드 완료 (${sec}초)"
    }
} else {
    Write-Ok "이미지 빌드 생략 (-SkipBuild)"
}

# ── 4. 네임스페이스 처리 ────────────────────────────────────────────────────
Write-Step "네임스페이스 설정"

if ($CleanInstall) {
    Write-Warn "기존 metrohub 네임스페이스 삭제 중..."
    kubectl delete namespace metrohub --ignore-not-found
    kubectl wait --for=delete namespace/metrohub --timeout=60s 2>&1 | Out-Null
    Write-Ok "기존 네임스페이스 삭제 완료"
}

kubectl apply -f k8s/namespace.yaml
Write-Ok "네임스페이스 준비"

# ── 5. ConfigMap / Secret 적용 ───────────────────────────────────────────────
Write-Step "ConfigMap / Secret 적용"

kubectl apply -f k8s/configmap.yaml

# Seoul API Key를 base64 인코딩해서 secret 생성
$seoulB64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($SeoulApiKey))

# secrets.yaml의 seoul-api-key만 실제 값으로 교체해서 적용
$secretYaml = Get-Content k8s/secrets.yaml -Raw
$secretYaml = $secretYaml -replace "eW91ci1zZW91bC1hcGkta2V5", $seoulB64
$secretYaml | kubectl apply -f -

Write-Ok "ConfigMap / Secret 적용 완료"

# ── 6. MySQL 배포 ────────────────────────────────────────────────────────────
Write-Step "MySQL 배포"

kubectl apply -f k8s/mysql/configmap.yaml
kubectl apply -f k8s/mysql/pvc.yaml
kubectl apply -f k8s/mysql/deployment.yaml
kubectl apply -f k8s/mysql/service.yaml

Write-Info "MySQL 기동 대기 중 (최대 120초)..."
kubectl wait --for=condition=ready pod -l app=mysql -n metrohub --timeout=120s
Write-Ok "MySQL 준비 완료"

# ── 7. Kafka / Zookeeper 배포 ────────────────────────────────────────────────
Write-Step "Kafka / Zookeeper 배포"

kubectl apply -f k8s/kafka/kafka-pvc.yaml
kubectl apply -f k8s/kafka/zookeeper.yaml
kubectl apply -f k8s/kafka/kafka.yaml

Write-Info "Zookeeper 기동 대기 중..."
kubectl wait --for=condition=ready pod -l app=zookeeper -n metrohub --timeout=90s
Write-Ok "Zookeeper 준비 완료"

Write-Info "Kafka 기동 대기 중 (최대 90초)..."
kubectl wait --for=condition=ready pod -l app=kafka -n metrohub --timeout=90s
Write-Ok "Kafka 준비 완료"

# ── 8. 애플리케이션 배포 ────────────────────────────────────────────────────
Write-Step "애플리케이션 배포"

kubectl apply -f k8s/subway-api/deployment.yaml
kubectl apply -f k8s/subway-api/service.yaml
kubectl apply -f k8s/subway-notification/deployment.yaml
kubectl apply -f k8s/subway-notification/service.yaml
kubectl apply -f k8s/subway-collector/deployment.yaml
kubectl apply -f k8s/subway-web/deployment.yaml
kubectl apply -f k8s/subway-web/service.yaml

Write-Info "subway-api 기동 대기 중 (최대 120초)..."
kubectl wait --for=condition=ready pod -l app=subway-api -n metrohub --timeout=120s
Write-Ok "subway-api 준비 완료"

Write-Info "subway-notification 기동 대기 중..."
kubectl wait --for=condition=ready pod -l app=subway-notification -n metrohub --timeout=120s
Write-Ok "subway-notification 준비 완료"

Write-Info "subway-web 기동 대기 중..."
kubectl wait --for=condition=ready pod -l app=subway-web -n metrohub --timeout=60s
Write-Ok "subway-web 준비 완료"

# ── 9. Ingress / HPA 적용 ───────────────────────────────────────────────────
Write-Step "Ingress / HPA 적용"

kubectl apply -f k8s/ingress.yaml
kubectl apply -f k8s/hpa.yaml
Write-Ok "Ingress / HPA 적용 완료"

# ── 10. hosts 파일 안내 ─────────────────────────────────────────────────────
Write-Step "hosts 파일 설정 안내"

$hostsEntry = "127.0.0.1  metrohub.local"
$hostsFile  = "C:\Windows\System32\drivers\etc\hosts"
$hosts      = Get-Content $hostsFile -Raw

if ($hosts -notmatch "metrohub\.local") {
    Write-Warn "hosts 파일에 아래 줄을 추가해야 브라우저에서 접속 가능합니다."
    Write-Warn "(관리자 권한 메모장으로 $hostsFile 열어서 맨 아래에 추가)"
    Write-Host ""
    Write-Host "    $hostsEntry" -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Ok "hosts 파일에 metrohub.local 이미 등록됨"
}

# Ingress host를 metrohub.local로 패치
kubectl patch ingress metrohub-ingress -n metrohub `
    --type=json `
    -p='[{"op":"replace","path":"/spec/rules/0/host","value":"metrohub.local"}]' 2>&1 | Out-Null

# ── 11. 최종 상태 출력 ──────────────────────────────────────────────────────
Write-Host ""
Write-Host "╔══════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║   배포 완료!                          ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""

kubectl get pods -n metrohub
Write-Host ""

Write-Host "  접속 주소 (hosts 파일 등록 후):" -ForegroundColor Cyan
Write-Host "    프론트엔드  →  http://metrohub.local" -ForegroundColor White
Write-Host "    API         →  http://metrohub.local/api/..." -ForegroundColor White
Write-Host "    직접 접속   →  http://localhost:30080  (subway-api NodePort)" -ForegroundColor DarkGray
Write-Host "                   http://localhost:30081  (subway-notification NodePort)" -ForegroundColor DarkGray
Write-Host ""
Write-Host "  유용한 명령어:" -ForegroundColor Cyan
Write-Host "    kubectl get pods -n metrohub           # pod 상태" -ForegroundColor DarkGray
Write-Host "    kubectl get hpa -n metrohub            # HPA 스케일링 상태" -ForegroundColor DarkGray
Write-Host "    kubectl logs -n metrohub -l app=subway-api -f   # API 로그" -ForegroundColor DarkGray
Write-Host "    kubectl rollout undo deployment/subway-api -n metrohub  # 롤백" -ForegroundColor DarkGray
Write-Host ""
