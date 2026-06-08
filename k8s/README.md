# Kubernetes 배포 가이드

## 사전 요구사항
- kubectl 설치 및 클러스터 연결
- Nginx Ingress Controller 설치
- Metrics Server 설치 (HPA용)
- GitHub Container Registry에 이미지가 push된 상태 (ci.yml master push 시 자동 빌드)

## 배포 순서

```bash
# 1. 네임스페이스 생성
kubectl apply -f namespace.yaml

# 2. Secret 생성 (base64 값 교체 필수)
kubectl apply -f secrets.yaml

# 3. ConfigMap 생성 (도메인 값 교체 후 적용)
kubectl apply -f configmap.yaml

# 4. MySQL 배포
kubectl apply -f mysql/configmap.yaml      # init SQL
kubectl apply -f mysql/pvc.yaml
kubectl apply -f mysql/deployment.yaml
kubectl apply -f mysql/service.yaml

# MySQL 준비 대기
kubectl wait --for=condition=ready pod -l app=mysql -n metrohub --timeout=120s

# 5. Kafka / Zookeeper 배포
kubectl apply -f kafka/kafka-pvc.yaml
kubectl apply -f kafka/zookeeper.yaml
kubectl apply -f kafka/kafka.yaml

# 6. 애플리케이션 배포
kubectl apply -f subway-api/deployment.yaml
kubectl apply -f subway-api/service.yaml
kubectl apply -f subway-notification/deployment.yaml
kubectl apply -f subway-notification/service.yaml
kubectl apply -f subway-collector/deployment.yaml
kubectl apply -f subway-web/deployment.yaml
kubectl apply -f subway-web/service.yaml

# 7. Ingress 설정
kubectl apply -f ingress.yaml

# 8. HPA 설정
kubectl apply -f hpa.yaml
```

## Secret 값 생성

```bash
echo -n 'your-mysql-password' | base64
echo -n 'your-jwt-secret'     | base64
echo -n 'your-seoul-api-key'  | base64
```

## 도메인 / 이미지 교체

`k8s/configmap.yaml` — `CORS_ALLOWED_ORIGINS` 값을 실제 도메인으로 교체:
```yaml
CORS_ALLOWED_ORIGINS: "https://your-domain.com"
```

`k8s/ingress.yaml` — `host` 필드를 실제 도메인으로 교체:
```yaml
host: your-domain.com
```

이미지는 GitHub Actions ci.yml이 master 브랜치 push 시 자동으로
`ghcr.io/jung-ji-su/metrohub-{api|notification|collector|web}:latest` 로 빌드/푸시.

## 상태 확인

```bash
# 전체 리소스 확인
kubectl get all -n metrohub

# HPA 상태
kubectl get hpa -n metrohub

# 로그 확인
kubectl logs -n metrohub deployment/subway-api -f
kubectl logs -n metrohub deployment/subway-notification -f

# Ingress 확인
kubectl get ingress -n metrohub
```

## 롤링 업데이트 (무중단 배포)

```bash
# 이미지 태그 업데이트
kubectl set image deployment/subway-api \
  subway-api=ghcr.io/jung-ji-su/metrohub-api:<SHA> -n metrohub
kubectl rollout status deployment/subway-api -n metrohub

# 롤백
kubectl rollout undo deployment/subway-api -n metrohub
```

## 아키텍처

```
Internet
   │
   ▼
Nginx Ingress (metrohub.example.com)
   ├── /api/notifications/* → subway-notification (8081)
   ├── /api/*              → subway-api          (8080)
   └── /*                  → subway-web           (80, nginx)

내부 클러스터
   subway-api ──Kafka──► subway-notification
   subway-collector ──Kafka──► subway-api
   subway-api ──MySQL──► mysql
   subway-notification ──MySQL──► mysql

HPA (자동 스케일링)
   subway-api:          2~5 replicas (CPU 70%)
   subway-notification: 1~3 replicas (CPU 70%)
   subway-web:          2~4 replicas (CPU 70%)
```
