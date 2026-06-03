# YGC Chits App — EC2 Docker Deployment Guide

## Prerequisites on EC2 (Amazon Linux 2 / Ubuntu)

```bash
# Install Docker
sudo yum update -y                          # Amazon Linux
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user            # allow ec2-user to run docker

# Install Docker Compose v2
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
docker-compose --version
```

## 1. Upload / Clone Project

```bash
# Option A: SCP from local
scp -i your-key.pem chits-app.zip ec2-user@<EC2-IP>:~/

# Option B: Git clone (recommended)
git clone https://github.com/your-org/chits-app.git
cd chits-app
```

## 2. Configure Environment

```bash
cp .env.example .env
nano .env                  # fill in DB_PASSWORD, JWT_SECRET, MAIL_* values
```

Generate a strong JWT secret:
```bash
openssl rand -base64 64
```

## 3. Open EC2 Security Group Ports

In AWS Console → EC2 → Security Groups, allow inbound:
- **8080** (TCP) — app (or use 80/443 via nginx reverse proxy)
- **22** (TCP) — SSH

## 4. Build & Start

```bash
docker-compose up -d --build
docker-compose logs -f app          # watch startup logs
```

## 5. Verify

```bash
# Check containers are healthy
docker-compose ps

# Test app
curl http://localhost:8080/login
```

## 6. Nginx Reverse Proxy (Optional — port 80/443)

```bash
sudo yum install -y nginx
sudo nano /etc/nginx/conf.d/ygc.conf
```

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass         http://localhost:8080;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_read_timeout 120s;
    }
}
```

```bash
sudo systemctl enable nginx && sudo systemctl start nginx
```

## 7. Useful Commands

```bash
# Stop everything
docker-compose down

# Stop and wipe DB data (destructive!)
docker-compose down -v

# Rebuild app only after code change
docker-compose up -d --build app

# Connect to PostgreSQL
docker exec -it ygc-postgres psql -U ygcuser -d ygcdb

# View app logs
docker-compose logs -f app

# View postgres logs
docker-compose logs -f postgres
```

## Environment Variables Reference

| Variable | Required | Description |
|---|---|---|
| `DB_NAME` | No (default: `ygcdb`) | PostgreSQL database name |
| `DB_USERNAME` | No (default: `ygcuser`) | DB user |
| `DB_PASSWORD` | **Yes** | DB password |
| `JWT_SECRET` | **Yes** | Random 64-char secret |
| `JWT_EXPIRATION` | No (default: `86400000`) | Token TTL in ms |
| `MAIL_HOST` | **Yes** | SMTP host |
| `MAIL_PORT` | No (default: `587`) | SMTP port |
| `MAIL_USERNAME` | **Yes** | SMTP user |
| `MAIL_PASSWORD` | **Yes** | SMTP password / app password |
