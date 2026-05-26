# Kubernetes 배포 가이드

## 사전 요구사항
- kubectl 설치 및 클러스터 연결
- 각 서비스의 Docker 이미지가 레지스트리에 push된 상태

## 배포 순서

```bash
# 1. 네임스페이스 생성
kubectl apply -f namespace.yaml

# 2. 시크릿 생성 (실제 값으로 교체 후 적용)
kubectl apply -f secrets.yaml

# 3. MySQL 배포
kubectl apply -f mysql/pvc.yaml
kubectl apply -f mysql/deployment.yaml
kubectl apply -f mysql/service.yaml

# 4. Kafka 배포 (Zookeeper 먼저)
kubectl apply -f kafka/zookeeper.yaml
kubectl apply -f kafka/kafka.yaml

# 5. 애플리케이션 배포
kubectl apply -f subway-api/deployment.yaml
kubectl apply -f subway-api/service.yaml
kubectl apply -f subway-notification/deployment.yaml
kubectl apply -f subway-notification/service.yaml
kubectl apply -f subway-collector/deployment.yaml
```

## 이미지 빌드 및 푸시

```bash
# 빌드
docker build -t metrohub/subway-api:latest ./subway-api
docker build -t metrohub/subway-notification:latest ./subway-notification
docker build -t metrohub/subway-collector:latest ./subway-collector

# 레지스트리에 푸시 (Docker Hub 기준)
docker push metrohub/subway-api:latest
docker push metrohub/subway-notification:latest
docker push metrohub/subway-collector:latest
```

## 시크릿 값 생성

```bash
echo -n 'your-password' | base64
```

## 상태 확인

```bash
kubectl get all -n metrohub
kubectl logs -n metrohub deployment/subway-api
```
