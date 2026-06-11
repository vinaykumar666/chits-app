#!/usr/bin/env bash
# Resolve Docker Compose command (plugin v2 or standalone binary).

ygc_compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    echo "docker compose"
    return 0
  fi
  if command -v docker-compose >/dev/null 2>&1 && docker-compose version >/dev/null 2>&1; then
    echo "docker-compose"
    return 0
  fi
  return 1
}

ygc_compose_prod() {
  local base
  base="$(ygc_compose_cmd)" || return 1
  echo "${base} -f docker-compose.prod.yml"
}

ygc_compose_default() {
  ygc_compose_cmd || return 1
}

ygc_install_compose() {
  if ygc_compose_cmd >/dev/null 2>&1; then
    return 0
  fi

  if command -v yum >/dev/null 2>&1; then
    sudo yum install -y docker-compose-plugin 2>/dev/null || true
  fi
  if docker compose version >/dev/null 2>&1; then
    return 0
  fi

  local plugin_dir="/usr/local/lib/docker/cli-plugins"
  sudo mkdir -p "${plugin_dir}"
  sudo curl -fsSL \
    "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
    -o "${plugin_dir}/docker-compose"
  sudo chmod +x "${plugin_dir}/docker-compose"

  if docker compose version >/dev/null 2>&1; then
    return 0
  fi

  sudo curl -fsSL \
    "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
    -o /usr/local/bin/docker-compose
  sudo chmod +x /usr/local/bin/docker-compose

  ygc_compose_cmd >/dev/null 2>&1
}
