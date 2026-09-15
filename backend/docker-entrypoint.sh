#!/bin/sh
# docker-entrypoint.sh — generates RSA keys for JWT if not present, then starts Quarkus

KEY_DIR="/app"

# Generate RSA private key (PKCS8 format required by SmallRye JWT)
if [ ! -f "$KEY_DIR/privateKey.pem" ]; then
  echo "[startup] Generating RSA key pair for JWT signing..."
  openssl genrsa -out "$KEY_DIR/privateKey_raw.pem" 2048 2>/dev/null
  openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
    -in "$KEY_DIR/privateKey_raw.pem" \
    -out "$KEY_DIR/privateKey.pem" 2>/dev/null
  openssl rsa -in "$KEY_DIR/privateKey_raw.pem" -pubout \
    -out "$KEY_DIR/publicKey.pem" 2>/dev/null
  rm -f "$KEY_DIR/privateKey_raw.pem"
  echo "[startup] RSA keys generated successfully."
fi

exec java \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -Djava.util.logging.manager=org.jboss.logmanager.LogManager \
  -Dsmallrye.jwt.sign.key.location="$KEY_DIR/privateKey.pem" \
  -Dmp.jwt.verify.publickey.location="$KEY_DIR/publicKey.pem" \
  -jar /app/quarkus-run.jar
