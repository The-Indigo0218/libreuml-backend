#!/usr/bin/env bash
# First-time Let's Encrypt certificate setup for production.
# Run this ONCE on the server before starting the full stack.
#
# Prerequisites: docker, docker compose, openssl, curl installed on the host.
# Usage: chmod +x init-letsencrypt.sh && ./init-letsencrypt.sh
set -euo pipefail

COMPOSE="docker compose -f docker-compose.yml -f docker-compose.prod.yml"

# ── Load .env ──────────────────────────────────────────────────────────────────
if [ ! -f .env ]; then
    echo "❌  .env not found. Copy .env.example to .env and fill in the values."
    exit 1
fi
# shellcheck disable=SC1091
set -a; source .env; set +a

if [ -z "${DOMAIN:-}" ] || [ -z "${CERTBOT_EMAIL:-}" ]; then
    echo "❌  DOMAIN and CERTBOT_EMAIL must be set in .env"
    exit 1
fi

CERT_PATH="./data/certbot/conf/live/$DOMAIN"

if [ -d "$CERT_PATH" ]; then
    echo "✅  Certificate already exists for $DOMAIN — nothing to do."
    echo "    Force renewal: $COMPOSE run --rm certbot certbot renew --force-renewal"
    exit 0
fi

# ── Download recommended TLS parameters ───────────────────────────────────────
echo "▶  Downloading recommended TLS parameters..."
mkdir -p "./data/certbot/conf" "./data/certbot/www"

curl -sSf \
    "https://raw.githubusercontent.com/certbot/certbot/master/certbot-nginx/certbot_nginx/_internal/tls_configs/options-ssl-nginx.conf" \
    -o "./data/certbot/conf/options-ssl-nginx.conf"

if [ ! -f "./data/certbot/conf/ssl-dhparams.pem" ]; then
    echo "▶  Generating DH params (this takes a minute)..."
    openssl dhparam -out "./data/certbot/conf/ssl-dhparams.pem" 2048
fi

# ── Create dummy self-signed cert so nginx can start ──────────────────────────
echo "▶  Creating temporary self-signed certificate..."
mkdir -p "$CERT_PATH"
openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout "$CERT_PATH/privkey.pem" \
    -out    "$CERT_PATH/fullchain.pem" \
    -subj   "/CN=$DOMAIN" 2>/dev/null

# ── Start the base stack + nginx (uses dummy cert) ────────────────────────────
echo "▶  Starting services (with temporary certificate)..."
$COMPOSE up -d db mail api
$COMPOSE up -d nginx

echo "▶  Waiting for nginx to be ready..."
until curl -sf --max-time 3 "http://$DOMAIN/.well-known/acme-challenge/" > /dev/null 2>&1 || \
      curl -sf --max-time 3 "http://localhost/" > /dev/null 2>&1; do
    sleep 2
done
sleep 3

# ── Request the real Let's Encrypt certificate ────────────────────────────────
echo "▶  Requesting Let's Encrypt certificate for $DOMAIN..."
$COMPOSE run --rm certbot \
    certbot certonly --webroot \
    --webroot-path=/var/www/certbot \
    --email "$CERTBOT_EMAIL" \
    --agree-tos \
    --no-eff-email \
    -d "$DOMAIN"

# ── Reload nginx with the real certificate ────────────────────────────────────
echo "▶  Reloading nginx with real certificate..."
$COMPOSE exec nginx nginx -s reload

# ── Start certbot renewal loop ────────────────────────────────────────────────
$COMPOSE up -d certbot

echo ""
echo "✅  Done! Your API is live at https://$DOMAIN"
echo ""
echo "    Next deploys: docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build"
