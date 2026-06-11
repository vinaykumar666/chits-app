#!/usr/bin/env bash
###############################################################################
# YGC Chits — EC2 one-shot deploy with Let's Encrypt HTTPS
#
# Run on your EC2 instance (Amazon Linux / Ubuntu) from the project root:
#
#   export YGC_DOMAIN=chits.yourdomain.com
#   export YGC_SSL_EMAIL=you@yourdomain.com
#   export DB_PASSWORD='your-strong-db-password'
#   export JWT_SECRET='your-jwt-secret'
#   chmod +x scripts/ec2-ssl-deploy.sh
#   ./scripts/ec2-ssl-deploy.sh
#
# Prerequisites:
#   - Docker + Docker Compose v2 installed
#   - DNS A-record: YGC_DOMAIN → this EC2 public IP
#   - Security group inbound: 22, 80, 443
###############################################################################
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# shellcheck source=lib-compose.sh
source "${ROOT}/scripts/lib-compose.sh"

COMPOSE="$(ygc_compose_prod)"
IMAGE="${DOCKER_USERNAME:-ygc}/ygc-chits:latest"
DOMAIN="${YGC_DOMAIN:-}"
EMAIL="${YGC_SSL_EMAIL:-}"
LOGIN_PATH="/login"

c_green(){ printf "\033[0;32m%s\033[0m\n" "$1"; }
c_red(){   printf "\033[0;31m%s\033[0m\n" "$1"; }
c_blue(){  printf "\033[0;34m%s\033[0m\n" "$1"; }
c_yellow(){ printf "\033[0;33m%s\033[0m\n" "$1"; }

require_cmd(){
  command -v "$1" >/dev/null 2>&1 || { c_red "Missing required command: $1"; exit 1; }
}

render_nginx(){
  local template="$1"
  local output="$2"
  sed "s/__YGC_DOMAIN__/${DOMAIN}/g" "$template" > "$output"
}

ensure_env(){
  if [[ ! -f .env ]]; then
    c_blue "▶ Creating .env from .env.example..."
    cp .env.example .env
    c_yellow "  Edit .env and re-run if you need custom mail/JWT values."
  fi
  # shellcheck disable=SC1091
  set -a; source .env; set +a

  : "${DB_PASSWORD:?Set DB_PASSWORD in .env or export it before running}"
  : "${JWT_SECRET:?Set JWT_SECRET in .env or export it before running}"

  DOMAIN="${YGC_DOMAIN:-${DOMAIN:-}}"
  EMAIL="${YGC_SSL_EMAIL:-${EMAIL:-}}"

  if [[ -z "$DOMAIN" ]]; then
    c_red "ERROR: Set YGC_DOMAIN (e.g. export YGC_DOMAIN=chits.yourdomain.com)"
    exit 1
  fi
  if [[ -z "$EMAIL" ]]; then
    c_red "ERROR: Set YGC_SSL_EMAIL (e.g. export YGC_SSL_EMAIL=admin@yourdomain.com)"
    exit 1
  fi

  export YGC_DOMAIN="$DOMAIN"
  export YGC_SSL_EMAIL="$EMAIL"
}

preflight(){
  c_blue "▶ Preflight checks..."
  require_cmd docker
  if ! ygc_compose_cmd >/dev/null 2>&1; then
    c_blue "▶ Installing Docker Compose..."
    ygc_install_compose || { c_red "Docker Compose required — re-run ./start.sh"; exit 1; }
    COMPOSE="$(ygc_compose_prod)"
  fi
  if docker compose version >/dev/null 2>&1; then
    c_green "✓ Compose: $(docker compose version 2>/dev/null | head -1)"
  else
    c_green "✓ Compose: $(docker-compose version 2>/dev/null | head -1)"
  fi

  mkdir -p certbot/conf certbot/www uploads

  PUBLIC_IP="$(curl -fsS --max-time 5 http://checkip.amazonaws.com 2>/dev/null | tr -d '[:space:]' || true)"
  if [[ -n "$PUBLIC_IP" ]]; then
    c_blue "  EC2 public IP: ${PUBLIC_IP}"
    c_blue "  Ensure DNS A-record: ${DOMAIN} → ${PUBLIC_IP}"
  fi

  if command -v dig >/dev/null 2>&1; then
    RESOLVED="$(dig +short "$DOMAIN" 2>/dev/null | tail -1 || true)"
    if [[ -n "$RESOLVED" ]]; then
      c_blue "  DNS resolves ${DOMAIN} → ${RESOLVED}"
    else
      c_yellow "  WARNING: ${DOMAIN} did not resolve — fix DNS before continuing"
    fi
  fi
}

build_frontend(){
  c_blue "▶ Building React frontend (Docker)..."
  docker run --rm \
    -v "${ROOT}/ygc-web:/frontend" \
    -w /frontend node:22-alpine \
    sh -c "npm ci && npm run build"
  c_green "✓ Frontend built → ygc-web/dist/"
}

build_app_image(){
  c_blue "▶ Building Spring Boot Docker image..."
  docker build -t "${IMAGE}" .
  c_green "✓ Docker image: ${IMAGE}"
}

cert_exists(){
  [[ -f "certbot/conf/live/${DOMAIN}/fullchain.pem" && -f "certbot/conf/live/${DOMAIN}/privkey.pem" ]]
}

obtain_certificate(){
  if cert_exists; then
    c_green "✓ SSL certificate already exists for ${DOMAIN}"
    return 0
  fi

  c_blue "▶ Obtaining Let's Encrypt certificate for ${DOMAIN}..."

  # Free port 80 for certbot standalone
  ${COMPOSE} down 2>/dev/null || true
  sleep 2

  docker run --rm -p 80:80 \
    -v "${ROOT}/certbot/conf:/etc/letsencrypt" \
    -v "${ROOT}/certbot/www:/var/www/certbot" \
    certbot/certbot certonly --standalone \
    --preferred-challenges http \
    -d "${DOMAIN}" \
    --email "${EMAIL}" \
    --agree-tos \
    --non-interactive \
    --no-eff-email

  if ! cert_exists; then
    c_red "✗ Certificate issuance failed. Check DNS and security group (port 80 open)."
    exit 1
  fi
  c_green "✓ Certificate obtained"
}

render_https_nginx(){
  c_blue "▶ Rendering HTTPS nginx config..."
  render_nginx nginx/nginx.conf.template nginx/nginx.conf
  c_green "✓ nginx/nginx.conf updated for ${DOMAIN}"
}

start_stack(){
  c_blue "▶ Freeing disk space before start..."
  if [[ -x "${ROOT}/scripts/docker-cleanup.sh" ]]; then
    bash "${ROOT}/scripts/docker-cleanup.sh"
  fi

  c_blue "▶ Starting production stack (app + postgres + nginx)..."
  ${COMPOSE} up -d --remove-orphans

  c_blue "▶ Waiting for app to become healthy..."
  for i in $(seq 1 36); do
    if docker exec ygc-app wget -qO- http://localhost:8080/login >/dev/null 2>&1; then
      c_green "✓ Application healthy after ~${i}0s"
      return 0
    fi
    sleep 10
  done
  c_red "✗ App health check timed out — run: ${COMPOSE} logs app"
  exit 1
}

verify_https(){
  c_blue "▶ Verifying HTTPS..."
  for i in $(seq 1 12); do
    if curl -fsS "https://${DOMAIN}${LOGIN_PATH}" >/dev/null 2>&1; then
      c_green "✓ HTTPS login page reachable"
      return 0
    fi
    sleep 5
  done
  c_yellow "  HTTPS not reachable yet — nginx may still be starting. Try again in a minute."
}

install_renewal_cron(){
  CRON_LINE="0 3 * * * cd ${ROOT} && docker run --rm -v ${ROOT}/certbot/conf:/etc/letsencrypt -v ${ROOT}/certbot/www:/var/www/certbot certbot/certbot renew --quiet && ${COMPOSE} exec -T nginx nginx -s reload"
  c_blue "▶ Add this cron job for auto-renewal (optional):"
  echo "  ${CRON_LINE}"
}

print_summary(){
  echo ""
  echo "════════════════════════════════════════════════════════════"
  c_green "  YGC Chits is live with HTTPS"
  echo "════════════════════════════════════════════════════════════"
  echo ""
  c_green "  Login URL:  https://${DOMAIN}${LOGIN_PATH}"
  echo ""
  echo "  Demo admin:"
  echo "    Email:    admin@ygc.internal"
  echo "    Password: Admin@123"
  echo ""
  echo "  Demo member:"
  echo "    Email:    aarav.sharma@example.com"
  echo "    Password: Member@123"
  echo ""
  echo "  Useful commands:"
  echo "    ${COMPOSE} ps"
  echo "    ${COMPOSE} logs -f app"
  echo "    ${COMPOSE} logs -f nginx"
  echo "    ./deploy.sh ssl-renew"
  echo "════════════════════════════════════════════════════════════"
}

main(){
  c_blue "╔══════════════════════════════════════════════════════════╗"
  c_blue "║  YGC Chits — EC2 SSL / HTTPS Deployment                 ║"
  c_blue "╚══════════════════════════════════════════════════════════╝"

  ensure_env
  preflight
  build_frontend
  build_app_image
  obtain_certificate
  render_https_nginx
  start_stack
  verify_https
  install_renewal_cron
  print_summary
}

main "$@"
