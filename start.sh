#!/usr/bin/env bash
###############################################################################
# YGC Chits — One-command EC2 bootstrap + HTTPS
#
# Default domain: yg-chits.duckdns.org
#
# Usage (Amazon Linux 2023 / Ubuntu, from repo root):
#   chmod +x start.sh
#   ./start.sh
#
# Optional:
#   export YGC_SSL_EMAIL=you@gmail.com
#   export YGC_DOMAIN=yg-chits.duckdns.org
#   export CONTINUE_ON_DNS_MISMATCH=1   # skip DNS IP mismatch prompt
###############################################################################
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

export YGC_DOMAIN="${YGC_DOMAIN:-yg-chits.duckdns.org}"
export YGC_SSL_EMAIL="${YGC_SSL_EMAIL:-admin@${YGC_DOMAIN}}"
export DOCKER_USERNAME="${DOCKER_USERNAME:-ygc}"

c_blue(){  printf "\033[0;34m%s\033[0m\n" "$1"; }
c_green(){ printf "\033[0;32m%s\033[0m\n" "$1"; }
c_red(){   printf "\033[0;31m%s\033[0m\n" "$1"; }
c_yellow(){ printf "\033[0;33m%s\033[0m\n" "$1"; }

install_yum_pkg(){
  local pkg="$1"
  if rpm -q "$pkg" >/dev/null 2>&1; then
    return 0
  fi
  sudo yum install -y "$pkg"
}

install_dependencies(){
  c_blue "▶ Installing system dependencies..."

  if command -v yum >/dev/null 2>&1; then
    sudo yum update -y || true
    # AL2023 ships curl-minimal — do NOT install full curl (package conflict)
    for pkg in docker git openssl bind-utils; do
      install_yum_pkg "$pkg"
    done
    if ! command -v curl >/dev/null 2>&1; then
      install_yum_pkg curl-minimal || install_yum_pkg curl
    fi
  elif command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update -y
    sudo apt-get install -y docker.io docker-compose-plugin git curl openssl dnsutils
  else
    c_red "Unsupported OS — install Docker manually, then re-run."
    exit 1
  fi

  sudo systemctl start docker
  sudo systemctl enable docker
  sudo usermod -aG docker "${USER}" 2>/dev/null || true

  # shellcheck source=scripts/lib-compose.sh
  source "${ROOT}/scripts/lib-compose.sh"
  if ! ygc_compose_cmd >/dev/null 2>&1; then
    c_blue "▶ Installing Docker Compose..."
    ygc_install_compose || { c_red "Failed to install Docker Compose"; exit 1; }
  fi

  if ! docker info >/dev/null 2>&1; then
    if [[ "${YGC_DOCKER_REEXEC:-}" != "1" ]] && getent group docker 2>/dev/null | grep -q "${USER}"; then
      c_blue "▶ Activating docker group for this session..."
      exec sg docker -c "cd '${ROOT}' && YGC_DOCKER_REEXEC=1 YGC_DOMAIN='${YGC_DOMAIN}' YGC_SSL_EMAIL='${YGC_SSL_EMAIL}' DOCKER_USERNAME='${DOCKER_USERNAME}' bash ./start.sh"
    fi
    c_yellow "Docker not accessible in this shell."
    c_yellow "Run:  newgrp docker   then   ./start.sh"
    exit 1
  fi

  c_green "✓ Docker ready: $($(ygc_compose_cmd) version 2>/dev/null | head -1)"
}

set_env_var(){
  local key="$1"
  local value="$2"
  local envfile="${ROOT}/.env"
  if grep -q "^${key}=" "$envfile" 2>/dev/null; then
    sed -i "s|^${key}=.*|${key}=${value}|" "$envfile"
  else
    echo "${key}=${value}" >> "$envfile"
  fi
}

write_default_env(){
  cat > "${ROOT}/.env" <<EOF
YGC_DOMAIN=${YGC_DOMAIN}
YGC_SSL_EMAIL=${YGC_SSL_EMAIL}
DB_NAME=ygcdb
DB_USERNAME=ygcuser
DB_PASSWORD=$(openssl rand -base64 24 | tr -d '/+=' | head -c 32)
JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
JWT_EXPIRATION=86400000
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=noreply@localhost
MAIL_PASSWORD=unused
DUCKDNS_TOKEN=
EOF
}

ensure_env_file(){
  c_blue "▶ Preparing .env..."

  if [[ ! -f "${ROOT}/.env" ]]; then
    if [[ -f "${ROOT}/.env.example" ]]; then
      cp "${ROOT}/.env.example" "${ROOT}/.env"
    else
      write_default_env
    fi
  fi

  if [[ ! -f "${ROOT}/.env" ]]; then
    c_red "ERROR: Could not create ${ROOT}/.env"
    exit 1
  fi

  if grep -q 'change_me_strong_password' "${ROOT}/.env" 2>/dev/null; then
    set_env_var DB_PASSWORD "$(openssl rand -base64 24 | tr -d '/+=' | head -c 32)"
  fi
  if grep -q 'replace_with_64char_random_secret' "${ROOT}/.env" 2>/dev/null; then
    set_env_var JWT_SECRET "$(openssl rand -base64 48 | tr -d '\n')"
  fi

  set_env_var YGC_DOMAIN "${YGC_DOMAIN}"
  set_env_var YGC_SSL_EMAIL "${YGC_SSL_EMAIL}"

  grep -q '^MAIL_USERNAME=' "${ROOT}/.env" || echo 'MAIL_USERNAME=noreply@localhost' >> "${ROOT}/.env"
  grep -q '^MAIL_PASSWORD=' "${ROOT}/.env" || echo 'MAIL_PASSWORD=unused' >> "${ROOT}/.env"

  # shellcheck disable=SC1091
  set -a; source "${ROOT}/.env"; set +a

  c_green "✓ .env ready (domain: ${YGC_DOMAIN})"
}

check_dns(){
  c_blue "▶ Checking DuckDNS / DNS..."

  PUBLIC_IP="$(curl -fsS --max-time 5 http://checkip.amazonaws.com | tr -d '[:space:]')"

  # Auto-update DuckDNS when token is configured
  if [[ -n "${DUCKDNS_TOKEN:-}" ]] && [[ "${YGC_DOMAIN}" == *.duckdns.org ]]; then
    local sub="${YGC_DOMAIN%%.duckdns.org}"
    c_blue "▶ Updating DuckDNS (${sub}) → ${PUBLIC_IP}..."
    local resp
    resp="$(curl -fsS --max-time 10 "https://www.duckdns.org/update?domains=${sub}&token=${DUCKDNS_TOKEN}&ip=${PUBLIC_IP}" || true)"
    if [[ "$resp" == "OK" ]]; then
      c_green "✓ DuckDNS updated"
      sleep 15
    else
      c_yellow "  DuckDNS update returned: ${resp:-error} — check DUCKDNS_TOKEN in .env"
    fi
  fi

  local resolved_google resolved_cloudflare resolved_local
  resolved_google="$(dig +short "${YGC_DOMAIN}" A @8.8.8.8 2>/dev/null | tail -1 || true)"
  resolved_cloudflare="$(dig +short "${YGC_DOMAIN}" A @1.1.1.1 2>/dev/null | tail -1 || true)"
  resolved_local="$(dig +short "${YGC_DOMAIN}" A 2>/dev/null | tail -1 || true)"

  c_blue "  EC2 public IP:          ${PUBLIC_IP}"
  c_blue "  Google DNS (8.8.8.8):   ${resolved_google:-<none>}"
  c_blue "  Cloudflare (1.1.1.1):   ${resolved_cloudflare:-<none>}"
  c_blue "  Local resolver:         ${resolved_local:-<none>}"

  if [[ -z "$resolved_google" && -z "$resolved_cloudflare" ]]; then
    c_red "ERROR: ${YGC_DOMAIN} is not visible on public DNS yet."
    c_yellow "  1. Open https://www.duckdns.org — subdomain must be: ${YGC_DOMAIN%%.duckdns.org}"
    c_yellow "  2. Set DUCKDNS_TOKEN in .env and re-run ./start.sh"
    c_yellow "  3. Wait 5–10 minutes, then: dig +short ${YGC_DOMAIN} @8.8.8.8"
    c_yellow "  4. Global check: https://dnschecker.org/#A/${YGC_DOMAIN}"
    exit 1
  fi

  local resolved="${resolved_google:-$resolved_cloudflare}"
  if [[ "$resolved" != "$PUBLIC_IP" ]]; then
    c_yellow "  WARNING: Public DNS (${resolved}) ≠ EC2 IP (${PUBLIC_IP}). SSL may fail."
    c_yellow "  Update DuckDNS, wait 5 minutes, re-run ./start.sh"
    if [[ "${CONTINUE_ON_DNS_MISMATCH:-0}" == "1" ]]; then
      c_yellow "  CONTINUE_ON_DNS_MISMATCH=1 — proceeding anyway."
    elif [[ -t 0 ]]; then
      read -r -p "Continue anyway? [y/N] " ans
      [[ "${ans,,}" == "y" ]] || exit 1
    else
      exit 1
    fi
  else
    c_green "✓ Public DNS OK (Let's Encrypt should resolve this domain)"
  fi
}

main(){
  c_blue "╔══════════════════════════════════════════════════════════╗"
  c_blue "║  YGC Chits — start.sh                                    ║"
  c_blue "║  HTTPS: https://${YGC_DOMAIN}/login"
  c_blue "╚══════════════════════════════════════════════════════════╝"

  install_dependencies
  ensure_env_file
  check_dns

  chmod +x scripts/ec2-ssl-deploy.sh scripts/docker-cleanup.sh deploy.sh 2>/dev/null || true

  c_blue "▶ Running full HTTPS deploy (certificates + nginx + app)..."
  bash scripts/ec2-ssl-deploy.sh

  echo ""
  c_green "════════════════════════════════════════════════════════════"
  c_green "  Live: https://${YGC_DOMAIN}/login"
  c_green "  Admin:  admin@ygc.internal / Admin@123"
  c_green "  Member: aarav.sharma@example.com / Member@123"
  c_green "════════════════════════════════════════════════════════════"
}

main "$@"
