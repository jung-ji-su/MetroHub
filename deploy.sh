#!/bin/bash
set -e

echo "=============================="
echo "  MetroHub 배포 스크립트"
echo "=============================="

# Docker 설치 확인
if ! command -v docker &> /dev/null; then
  echo "[1/5] Docker 설치 중..."
  curl -fsSL https://get.docker.com | sh
  sudo usermod -aG docker "$USER"
  echo ""
  echo "Docker 설치 완료."
  echo "로그아웃 후 다시 로그인하고, deploy.sh를 다시 실행하세요."
  exit 0
fi

# Node.js 설치 확인 (프론트엔드 빌드용)
if ! command -v node &> /dev/null; then
  echo "[1/5] Node.js 20 설치 중..."
  curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
  sudo apt-get install -y nodejs
fi

# .env 파일 확인
if [ ! -f .env ]; then
  echo ""
  echo "[!] .env 파일이 없습니다."
  cp .env.prod.example .env
  echo ".env.prod.example을 .env로 복사했습니다."
  echo "아래 파일을 열어 값을 채운 후 다시 실행하세요:"
  echo "  nano .env"
  exit 1
fi

# 필수 env 값 확인
for key in SEOUL_API_KEY MYSQL_ROOT_PASSWORD MYSQL_PASSWORD JWT_SECRET; do
  val=$(grep "^${key}=" .env | cut -d= -f2-)
  if [ -z "$val" ] || [[ "$val" == *"입력"* ]]; then
    echo "[!] .env에서 ${key} 값을 설정하세요."
    exit 1
  fi
done

echo "[2/5] SvelteKit 프론트엔드 빌드 중..."
cd subway-web
npm ci --silent
npm run build
cd ..

echo "[3/5] Docker 이미지 빌드 중..."
docker compose -f docker-compose.prod.yml build

echo "[4/5] 기존 서비스 중지 후 재시작..."
docker compose -f docker-compose.prod.yml down --remove-orphans
docker compose -f docker-compose.prod.yml up -d

echo "[5/5] 완료!"
echo ""
echo "서비스 상태:"
docker compose -f docker-compose.prod.yml ps
echo ""
PUBLIC_IP=$(curl -s --max-time 5 ifconfig.me 2>/dev/null || echo "IP 확인 실패")
echo "접속 주소: http://${PUBLIC_IP}"
