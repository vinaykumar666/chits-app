#!/usr/bin/env bash
###############################################################################
# Free disk space on EC2 before pulling/starting containers.
# Safe: does NOT remove named volumes (postgres pgdata is preserved).
#
# Usage: ./scripts/docker-cleanup.sh
###############################################################################
set -euo pipefail

c_blue(){  printf "\033[0;34m%s\033[0m\n" "$1"; }
c_green(){ printf "\033[0;32m%s\033[0m\n" "$1"; }
c_yellow(){ printf "\033[0;33m%s\033[0m\n" "$1"; }

show_disk(){
  c_blue "▶ Disk usage:"
  df -h / /var/lib/docker 2>/dev/null || df -h /
  echo ""
  c_blue "▶ Docker disk usage (before cleanup):"
  docker system df 2>/dev/null || true
  echo ""
}

cleanup_docker(){
  c_blue "▶ Stopping unused containers..."
  docker container prune -f 2>/dev/null || true

  c_blue "▶ Removing dangling unused images (keeps tagged app images)..."
  docker image prune -f 2>/dev/null || true

  c_blue "▶ Removing build cache..."
  docker builder prune -af 2>/dev/null || true

  c_blue "▶ Removing unused networks..."
  docker network prune -f 2>/dev/null || true

  # Do NOT run: docker volume prune — would risk postgres data on small EC2 disks
  c_yellow "  (Named volumes such as pgdata are kept intentionally)"
}

main(){
  c_blue "╔══════════════════════════════════════════════════════════╗"
  c_blue "║  Docker disk cleanup (pre-deploy)                       ║"
  c_blue "╚══════════════════════════════════════════════════════════╝"

  show_disk
  cleanup_docker

  c_green "✓ Docker cleanup complete"
  echo ""
  c_blue "▶ Docker disk usage (after cleanup):"
  docker system df 2>/dev/null || true
  echo ""
  c_blue "▶ Disk usage after cleanup:"
  df -h / /var/lib/docker 2>/dev/null || df -h /
}

main "$@"
