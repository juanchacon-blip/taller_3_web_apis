#!/usr/bin/env bash
# Genera el par de llaves RSA (2048 bits) para firmar/validar los JWT.
# private.pem -> PKCS#8, public.pem -> X.509 (formato que espera auth-service).
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p keys
if [[ -f keys/private.pem ]]; then
  echo "keys/private.pem ya existe, no se regenera."
  exit 0
fi
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/private.pem
openssl rsa -in keys/private.pem -pubout -out keys/public.pem
echo "Llaves generadas en ./keys (no se suben a Git)."
