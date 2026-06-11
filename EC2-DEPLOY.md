# YGC Chits — EC2 Deploy with HTTPS (Let's Encrypt)

## Quick start (one command)

### Option A — No git on server yet (curl only)

On a fresh EC2 instance:

```bash
# 1. DuckDNS: point chits-live → EC2 public IP at https://www.duckdns.org
# 2. Security group: open 22, 80, 443

curl -fsSL https://raw.githubusercontent.com/vinaykumar666/chits-app/feature/extreme-features/scripts/bootstrap-ec2.sh | bash
```

With your email for Let's Encrypt:

```bash
curl -fsSL https://raw.githubusercontent.com/vinaykumar666/chits-app/feature/extreme-features/scripts/bootstrap-ec2.sh | YGC_SSL_EMAIL=you@gmail.com bash
```

### Option B — Git already installed

```bash
git clone https://github.com/vinaykumar666/chits-app.git
cd chits-app
git checkout feature/extreme-features
chmod +x start.sh
./start.sh
```

If `git: command not found`, install it first:

```bash
sudo yum install -y git    # Amazon Linux
# sudo apt-get install -y git   # Ubuntu
```

When finished:

```
https://chits-live.duckdns.org/login
```

### Manual / custom domain

```bash
# 1. Point DNS A-record → EC2 public IP (required before SSL)
#    e.g. chits.yourdomain.com → 54.x.x.x

# 2. Open security group ports: 22, 80, 443

# 3. Configure secrets
cp .env.example .env
nano .env   # set DB_PASSWORD, JWT_SECRET, YGC_DOMAIN, YGC_SSL_EMAIL, MAIL_*

# 4. Run SSL deploy script
chmod +x scripts/ec2-ssl-deploy.sh
export YGC_DOMAIN=chits.yourdomain.com
export YGC_SSL_EMAIL=you@yourdomain.com
./scripts/ec2-ssl-deploy.sh
```

---

## Prerequisites on EC2

```bash
# Amazon Linux 2023 / AL2
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user
# log out and back in for group change

# Docker Compose v2
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
docker compose version
```

## Security group (AWS Console)

| Port | Protocol | Source    | Purpose        |
|------|----------|-----------|----------------|
| 22   | TCP      | Your IP   | SSH            |
| 80   | TCP      | 0.0.0.0/0 | HTTP + ACME    |
| 443  | TCP      | 0.0.0.0/0 | HTTPS          |

## What the script does

1. Validates Docker, DNS, and `.env` secrets
2. Builds React frontend (`ygc-web/dist`) via Docker
3. Builds Spring Boot Docker image
4. Obtains Let's Encrypt certificate (certbot standalone on port 80)
5. Renders `nginx/nginx.conf` from template for your domain
6. Starts `docker-compose.prod.yml` (app + postgres + nginx)
7. Prints **https://YOUR_DOMAIN/login**

## Demo credentials

```
Admin:  admin@ygc.internal / Admin@123
Member: aarav.sharma@example.com / Member@123
```

## SSL renewal

```bash
./deploy.sh ssl-renew
```

Optional cron (auto-renew nightly):

```bash
crontab -e
# add:
0 3 * * * cd /home/ec2-user/chits-app && ./deploy.sh ssl-renew >> /var/log/ygc-ssl-renew.log 2>&1
```

## Manual steps (alternative)

```bash
export YGC_DOMAIN=chits.yourdomain.com
export YGC_SSL_EMAIL=you@yourdomain.com
./deploy.sh ssl-init    # obtain certificate
./deploy.sh deploy      # build + start stack
```

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Certbot fails | DNS must point to EC2 **before** running script; port 80 must be open |
| nginx won't start | Check cert exists: `ls certbot/conf/live/$YGC_DOMAIN/` |
| 502 on login | Wait 60s for app startup: `docker compose -f docker-compose.prod.yml logs app` |
| CORS errors | Set `YGC_DOMAIN` in `.env` and redeploy |

## Useful commands

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f nginx
docker compose -f docker-compose.prod.yml logs -f app
curl -I https://YOUR_DOMAIN/login
```
