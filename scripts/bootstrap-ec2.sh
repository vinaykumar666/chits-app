#!/usr/bin/env bash
###############################################################################
# Bootstrap EC2 without git — download via curl, then full HTTPS deploy.
#
# Run on a fresh Amazon Linux 2023 EC2 (no git required):
#
#   curl -fsSL https://raw.githubusercontent.com/vinaykumar666/chits-app/feature/extreme-features/scripts/bootstrap-ec2.sh | bash
#
# Or with custom Let's Encrypt email:
#   curl -fsSL .../bootstrap-ec2.sh | YGC_SSL_EMAIL=you@gmail.com bash
###############################################################################
set -euo pipefail

REPO="https://github.com/vinaykumar666/chits-app.git"
BRANCH="feature/extreme-features"
INSTALL_DIR="${HOME}/chits-app"

c_blue(){  printf "\033[0;34m%s\033[0m\n" "$1"; }
c_green(){ printf "\033[0;32m%s\033[0m\n" "$1"; }
c_red(){   printf "\033[0;31m%s\033[0m\n" "$1"; }

install_git(){
  if command -v git >/dev/null 2>&1; then
    return 0
  fi
  c_blue "▶ Installing git..."
  if command -v yum >/dev/null 2>&1; then
    sudo yum install -y git
  elif command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update -y
    sudo apt-get install -y git
  else
    c_red "Cannot install git automatically. Run: sudo yum install -y git"
    exit 1
  fi
  c_green "✓ git installed"
}

clone_or_update(){
  install_git
  if [[ -d "${INSTALL_DIR}/.git" ]]; then
    c_blue "▶ Updating existing repo..."
    cd "${INSTALL_DIR}"
    git fetch origin
    git reset --hard "origin/${BRANCH}"
  else
    c_blue "▶ Cloning repository..."
    git clone --branch "${BRANCH}" --depth 1 "${REPO}" "${INSTALL_DIR}"
    cd "${INSTALL_DIR}"
  fi
}

main(){
  c_blue "╔══════════════════════════════════════════════════════════╗"
  c_blue "║  YGC Chits — EC2 bootstrap (no git needed to start)       ║"
  c_blue "╚══════════════════════════════════════════════════════════╝"

  clone_or_update
  chmod +x start.sh scripts/*.sh deploy.sh 2>/dev/null || true

  if [[ -n "${YGC_SSL_EMAIL:-}" ]]; then
    export YGC_SSL_EMAIL
  fi

  c_blue "▶ Running start.sh..."
  exec ./start.sh
}

main "$@"
