#!/usr/bin/env bash
###############################################################################
# YGC Chits — Production Deployment Script
# Usage: ./deploy.sh [build|deploy|rollback|logs|status|ssl-renew]
# Requires: docker, docker-compose, (certbot for SSL)
###############################################################################
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/lib-compose.sh
source "${ROOT}/scripts/lib-compose.sh"

APP_NAME="ygc-chits"
IMAGE="${DOCKER_USERNAME:-ygc}/${APP_NAME}"
COMPOSE="$(ygc_compose_prod)"
HEALTH_URL="http://localhost:8080/login"
DOMAIN="${YGC_DOMAIN:-chits.example.com}"
EMAIL="${YGC_SSL_EMAIL:-admin@example.com}"

c_blue(){ printf "\033[0;34m%s\033[0m\n" "$1"; }
c_red(){ printf "\033[0;31m%s\033[0m\n" "$1"; }
c_green(){ printf "\033[0;32m%s\033[0m\n" "$1"; }

docker_cleanup(){
  c_blue "▶ Freeing disk space before deploy..."
  if [[ -x "${ROOT}/scripts/docker-cleanup.sh" ]]; then
    bash "${ROOT}/scripts/docker-cleanup.sh"
  else
    docker container prune -f || true
    docker image prune -f || true
    docker builder prune -af || true
    docker network prune -f || true
  fi
}

build(){
  docker_cleanup
  c_blue "▶ Building React frontend..."
  docker run --rm -v "$(pwd)/ygc-web:/frontend" -w /frontend node:22-alpine \
    sh -c "npm ci && npm run build"
  c_blue "▶ Building Docker image..."
  TAG="$(git rev-parse --short HEAD 2>/dev/null || echo latest)"
  docker build -t "${IMAGE}:latest" -t "${IMAGE}:${TAG}" .
  c_green "✓ Build complete"
}

deploy(){
  c_blue "▶ Tagging current image for rollback..."
  docker tag "${IMAGE}:latest" "${IMAGE}:previous" 2>/dev/null || true
  c_blue "▶ Stopping existing stack..."
  ${COMPOSE} down --remove-orphans 2>/dev/null || true
  c_blue "▶ Pulling postgres + nginx..."
  docker pull postgres:16-alpine
  docker pull nginx:1.27-alpine
  c_blue "▶ Starting services..."
  ${COMPOSE} up -d --remove-orphans --pull missing
  c_blue "▶ Waiting for health check..."
  for i in $(seq 1 30); do
    if curl -fsS "${HEALTH_URL}" >/dev/null 2>&1; then
      c_green "✓ Application healthy after ${i}0s"
      ${COMPOSE} ps
      return 0
    fi
    sleep 10
  done
  c_red "✗ Health check failed — rolling back"
  rollback
  exit 1
}

rollback(){
  c_red "▶ Rolling back to previous image..."
  docker tag "${IMAGE}:previous" "${IMAGE}:latest"
  ${COMPOSE} up -d
  c_green "✓ Rolled back"
}

ssl_init(){
  c_blue "▶ Rendering nginx config for ${DOMAIN}..."
  sed "s/__YGC_DOMAIN__/${DOMAIN}/g" nginx/nginx.conf.template > nginx/nginx.conf
  c_blue "▶ Obtaining Let's Encrypt certificate for ${DOMAIN}..."
  ${COMPOSE} down 2>/dev/null || true
  docker run --rm -p 80:80 \
    -v "$(pwd)/certbot/conf:/etc/letsencrypt" \
    -v "$(pwd)/certbot/www:/var/www/certbot" \
    certbot/certbot certonly --standalone \
    -d "${DOMAIN}" --email "${EMAIL}" --agree-tos --non-interactive --no-eff-email
  c_green "✓ Certificate obtained"
  c_green "  Login URL: https://${DOMAIN}/login"
}

ssl_renew(){
  c_blue "▶ Renewing SSL certificate..."
  docker run --rm \
    -v "$(pwd)/certbot/conf:/etc/letsencrypt" \
    -v "$(pwd)/certbot/www:/var/www/certbot" \
    certbot/certbot renew
  ${COMPOSE} exec nginx nginx -s reload
  c_green "✓ SSL renewed and nginx reloaded"
}

case "${1:-deploy}" in
  build)     build ;;
  deploy)    build && deploy ;;
  rollback)  rollback ;;
  logs)      ${COMPOSE} logs -f --tail=100 ;;
  status)    ${COMPOSE} ps && curl -fsS "${HEALTH_URL}" | head -1 ;;
  ssl-init)  ssl_init ;;
  ssl-renew) ssl_renew ;;
  *) echo "Usage: $0 [build|deploy|rollback|logs|status|ssl-init|ssl-renew]"; exit 1 ;;
esac
