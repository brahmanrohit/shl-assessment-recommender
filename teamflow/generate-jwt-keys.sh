#!/usr/bin/env bash
# ============================================================
#  Generates the RSA keypair used to sign and verify JWTs.
#  Run once before local development:   bash generate-jwt-keys.sh
#  (The Docker build generates its own keys automatically,
#   and the keys are gitignored - secrets never go into git.)
# ============================================================
set -e
cd "$(dirname "$0")"

mkdir -p src/main/resources/jwt
openssl genrsa -out /tmp/teamflow-rsa.pem 2048
openssl rsa -pubout -in /tmp/teamflow-rsa.pem -out src/main/resources/jwt/publicKey.pem
openssl pkcs8 -topk8 -nocrypt -inform pem -in /tmp/teamflow-rsa.pem -outform pem -out src/main/resources/jwt/privateKey.pem
rm /tmp/teamflow-rsa.pem

echo "JWT keys written to src/main/resources/jwt/ (privateKey.pem + publicKey.pem)"
