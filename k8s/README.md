# Kubernetes 배포 가이드

## 사전 요구사항
- kubectl 설치 및 클러스터 연결
- Nginx Ingress Controller 설치
- Metrics Server 설치 (HPA용)
- 각 서비스의 Docker 이미지가 레지스트리에 push된 상태

## 배포 순서

```bash
# 1. 네임스페이스 생성
kubectl apply -f namespace.yaml

# 2. 시크릿 생성 (secrets.yaml의 base64 값을 실제 값으로 교체 후 적용)
kubectl apply -f secrets.yaml

# 3. ConfigMap 생성
kubectl apply -f configmap.yaml

# 4. MySQL 배포
kubectl apply -f mysql/pvc.yaml
kubectl apply -f mysql/deployment.yaml
kubectl apply -f mysql/service.yaml

# MySQL 준비 대기
kubectl wait --for=condition=ready pod -l app=mysql -n metrohub --timeout=120s

# 5. Kafka/Zookeeper 배포
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

## 시크릿 값 생성

```bash
# base64 인코딩 예시
echo -n 'your-mysql-password' | base64
echo -n 'your-jwt-secret'     | base64
echo -n 'your-seoul-api-key'  | base64
```

## 이미지 교체

`subway-api/deployment.yaml`, `subway-notification/deployment.yaml` 등에서  
`image:` 필드를 실제 레지스트리 경로로 변경하세요:

```
ghcr.io/YOUR_GITHUB_USERNAME/metrohub-api:latest
```

## 상태 확인

```bash
# 전체 리소스 확인
kubectl get all -n metrohub

# HPA 상태
kubectl get hpa -n metrohub

# 로그 확인
kubectl logs -n metrohub deployment/subway-api --follow
kubectl logs -n metrohub deployment/subway-notification --follow

# Ingress 확인
kubectl get ingress -n metrohub
```

## 롤링 업데이트

```bash
# 이미지 업데이트 (무중단 배포)
kubectl set image deployment/subway-api subway-api=ghcr.io/OWNER/metrohub-api:NEW_TAG -n metrohub
kubectl rollout status deployment/subway-api -n metrohub

# 롤백
kubectl rollout undo deployment/subway-api -n metrohub
```
