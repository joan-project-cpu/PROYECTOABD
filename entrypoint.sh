#!/bin/sh
# Prepara el wallet de Oracle Cloud (entregado como variables de entorno) y arranca la aplicacion.
set -e

WALLET_DIR=/tmp/wallet
mkdir -p "$WALLET_DIR"

set -- -Xmx280m -XX:+UseSerialGC -XX:MaxMetaspaceSize=120m -Xss512k -Duser.timezone=America/Lima

if [ -n "$WALLET_TRUSTSTORE_B64" ] && [ -n "$WALLET_KEYSTORE_B64" ]; then
    printf '%s' "$WALLET_TRUSTSTORE_B64" | base64 -d > "$WALLET_DIR/truststore.jks"
    printf '%s' "$WALLET_KEYSTORE_B64"   | base64 -d > "$WALLET_DIR/keystore.jks"
    set -- "$@" \
        "-Djavax.net.ssl.trustStore=$WALLET_DIR/truststore.jks" \
        "-Djavax.net.ssl.trustStoreType=JKS" \
        "-Djavax.net.ssl.trustStorePassword=$WALLET_PASSWORD" \
        "-Djavax.net.ssl.keyStore=$WALLET_DIR/keystore.jks" \
        "-Djavax.net.ssl.keyStoreType=JKS" \
        "-Djavax.net.ssl.keyStorePassword=$WALLET_PASSWORD"
    echo ">>> Wallet de Oracle cargado desde las variables de entorno"
else
    echo ">>> AVISO: faltan WALLET_TRUSTSTORE_B64 / WALLET_KEYSTORE_B64; se usara DB_URL sin wallet"
fi

exec java "$@" -jar /app/app.jar
