# MetroHub - Docker Desktop Kubernetes Deploy Script
# Usage:
#   .\k8s-deploy.ps1 -SeoulApiKey "your-key"   # first deploy
#   .\k8s-deploy.ps1 -SkipBuild                # skip image build
#   .\k8s-deploy.ps1 -CleanInstall             # delete namespace and reinstall

param(
    [string]$SeoulApiKey  = $env:SEOUL_API_KEY,
    [switch]$SkipBuild,
    [switch]$CleanInstall
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

function Write-Step { param($msg) Write-Host "`n>> $msg" -ForegroundColor Cyan }
function Write-Ok   { param($msg) Write-Host "  OK  $msg" -ForegroundColor Green }
function Write-Warn { param($msg) Write-Host "  !!  $msg" -ForegroundColor Yellow }
function Write-Info { param($msg) Write-Host "      $msg" -ForegroundColor Gray }
function Write-Fail { param($msg) Write-Host "  ERR $msg" -ForegroundColor Red; exit 1 }

Write-Host ""
Write-Host "======================================" -ForegroundColor Magenta
Write-Host "  MetroHub Kubernetes Deploy" -ForegroundColor Magenta
Write-Host "======================================" -ForegroundColor Magenta
Write-Host "  Start: $(Get-Date -Format 'HH:mm:ss')" -ForegroundColor DarkGray

# ── 1. Prerequisites ─────────────────────────────────────────────────────────
Write-Step "1/9 Checking prerequisites"

docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) { Write-Fail "Docker Desktop is not running." }
Write-Ok "Docker Desktop is running"

$ctx = kubectl config current-context 2>&1
if ($ctx -ne "docker-desktop") {
    Write-Warn "Current context: $ctx -> switching to docker-desktop"
    kubectl config use-context docker-desktop
    if ($LASTEXITCODE -ne 0) {
        Write-Fail "Cannot switch to docker-desktop context.`nDocker Desktop -> Settings -> Kubernetes -> Enable Kubernetes"
    }
}
Write-Ok "kubectl context: docker-desktop"

if (-not $SeoulApiKey) {
    Write-Warn "SeoulApiKey not provided. Collector will run but won't fetch data."
    Write-Info "Re-run with: .\k8s-deploy.ps1 -SeoulApiKey `"your-key`""
    $SeoulApiKey = "dummy-key"
}
Write-Ok "Seoul API Key ready"

# ── 2. Nginx Ingress Controller ──────────────────────────────────────────────
Write-Step "2/9 Nginx Ingress Controller"

$ingressNs = kubectl get namespace ingress-nginx --ignore-not-found 2>&1
if ($ingressNs -notmatch "ingress-nginx") {
    Write-Info "Installing Nginx Ingress Controller..."
    kubectl apply -f "https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.1/deploy/static/provider/cloud/deploy.yaml"
    Write-Info "Waiting for ingress controller to be ready (up to 90s)..."
    kubectl wait --namespace ingress-nginx `
        --for=condition=ready pod `
        --selector=app.kubernetes.io/component=controller `
        --timeout=90s
    Write-Ok "Nginx Ingress Controller installed"
} else {
    Write-Ok "Nginx Ingress Controller already installed"
}

# ── 3. Docker image build ────────────────────────────────────────────────────
if (-not $SkipBuild) {
    Write-Step "3/9 Building Docker images (first build takes 10-15 min)"

    $images = @(
        @{ name = "subway-api";          tag = "ghcr.io/jung-ji-su/metrohub-api:latest" },
        @{ name = "subway-notification"; tag = "ghcr.io/jung-ji-su/metrohub-notification:latest" },
        @{ name = "subway-collector";    tag = "ghcr.io/jung-ji-su/metrohub-collector:latest" },
        @{ name = "subway-web";          tag = "ghcr.io/jung-ji-su/metrohub-web:latest" }
    )

    foreach ($img in $images) {
        Write-Info "Building $($img.name)..."
        $start = Get-Date
        docker build -t $img.tag "./$($img.name)"
        if ($LASTEXITCODE -ne 0) { Write-Fail "$($img.name) build failed" }
        $sec = [int](New-TimeSpan -Start $start -End (Get-Date)).TotalSeconds
        Write-Ok "$($img.name) built (${sec}s)"
    }
} else {
    Write-Ok "3/9 Image build skipped (-SkipBuild)"
}

# ── 4. Namespace ─────────────────────────────────────────────────────────────
Write-Step "4/9 Namespace"

if ($CleanInstall) {
    Write-Warn "Deleting existing metrohub namespace..."
    kubectl delete namespace metrohub --ignore-not-found
    kubectl wait --for=delete namespace/metrohub --timeout=60s 2>&1 | Out-Null
    Write-Ok "Old namespace deleted"
}

kubectl apply -f k8s/namespace.yaml
Write-Ok "Namespace ready"

# ── 5. ConfigMap / Secret ────────────────────────────────────────────────────
Write-Step "5/9 ConfigMap / Secret"

kubectl apply -f k8s/configmap.yaml

$seoulB64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($SeoulApiKey))
$secretYaml = Get-Content k8s/secrets.yaml -Raw
$secretYaml = $secretYaml -replace "eW91ci1zZW91bC1hcGkta2V5", $seoulB64
$secretYaml | kubectl apply -f -

Write-Ok "ConfigMap / Secret applied"

# ── 6. MySQL ──────────────────────────────────────────────────────────────────
Write-Step "6/9 MySQL"

kubectl apply -f k8s/mysql/configmap.yaml
kubectl apply -f k8s/mysql/pvc.yaml
kubectl apply -f k8s/mysql/deployment.yaml
kubectl apply -f k8s/mysql/service.yaml

Write-Info "Waiting for MySQL to be ready (up to 120s)..."
kubectl wait --for=condition=ready pod -l app=mysql -n metrohub --timeout=120s
Write-Ok "MySQL ready"

# ── 7. Kafka / Zookeeper ─────────────────────────────────────────────────────
Write-Step "7/9 Kafka / Zookeeper"

kubectl apply -f k8s/kafka/kafka-pvc.yaml
kubectl apply -f k8s/kafka/zookeeper.yaml
kubectl apply -f k8s/kafka/kafka.yaml

Write-Info "Waiting for Zookeeper..."
kubectl wait --for=condition=ready pod -l app=zookeeper -n metrohub --timeout=90s
Write-Ok "Zookeeper ready"

Write-Info "Waiting for Kafka (up to 90s)..."
kubectl wait --for=condition=ready pod -l app=kafka -n metrohub --timeout=90s
Write-Ok "Kafka ready"

# ── 8. Application deployments ───────────────────────────────────────────────
Write-Step "8/9 Application deployments"

kubectl apply -f k8s/subway-api/deployment.yaml
kubectl apply -f k8s/subway-api/service.yaml
kubectl apply -f k8s/subway-notification/deployment.yaml
kubectl apply -f k8s/subway-notification/service.yaml
kubectl apply -f k8s/subway-collector/deployment.yaml
kubectl apply -f k8s/subway-web/deployment.yaml
kubectl apply -f k8s/subway-web/service.yaml

Write-Info "Waiting for subway-api (up to 120s)..."
kubectl wait --for=condition=ready pod -l app=subway-api -n metrohub --timeout=120s
Write-Ok "subway-api ready"

Write-Info "Waiting for subway-notification..."
kubectl wait --for=condition=ready pod -l app=subway-notification -n metrohub --timeout=120s
Write-Ok "subway-notification ready"

Write-Info "Waiting for subway-web..."
kubectl wait --for=condition=ready pod -l app=subway-web -n metrohub --timeout=60s
Write-Ok "subway-web ready"

# ── 9. Ingress / HPA ─────────────────────────────────────────────────────────
Write-Step "9/9 Ingress / HPA"

kubectl apply -f k8s/ingress.yaml
kubectl apply -f k8s/hpa.yaml
Write-Ok "Ingress / HPA applied"

# hosts file guide
$hostsFile = "C:\Windows\System32\drivers\etc\hosts"
$hosts = Get-Content $hostsFile -Raw
if ($hosts -notmatch "metrohub\.local") {
    Write-Warn "Add the following line to $hostsFile (open with admin Notepad):"
    Write-Host ""
    Write-Host "    127.0.0.1  metrohub.local" -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Ok "metrohub.local already in hosts file"
}

# ── Done ─────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "======================================" -ForegroundColor Green
Write-Host "  Deploy complete!" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Green
Write-Host ""

kubectl get pods -n metrohub
Write-Host ""

Write-Host "  Access URLs (after hosts file entry):" -ForegroundColor Cyan
Write-Host "    Frontend  ->  http://metrohub.local" -ForegroundColor White
Write-Host "    API       ->  http://metrohub.local/api/..." -ForegroundColor White
Write-Host "    Direct    ->  http://localhost:30080  (subway-api NodePort)" -ForegroundColor DarkGray
Write-Host "               ->  http://localhost:30081  (subway-notification NodePort)" -ForegroundColor DarkGray
Write-Host ""
Write-Host "  Useful commands:" -ForegroundColor Cyan
Write-Host "    kubectl get pods -n metrohub" -ForegroundColor DarkGray
Write-Host "    kubectl get hpa -n metrohub" -ForegroundColor DarkGray
Write-Host "    kubectl logs -n metrohub -l app=subway-api -f" -ForegroundColor DarkGray
Write-Host "    kubectl rollout undo deployment/subway-api -n metrohub" -ForegroundColor DarkGray
Write-Host ""
