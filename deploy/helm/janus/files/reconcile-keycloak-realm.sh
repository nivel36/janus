#!/bin/sh
set -eu

# Startup realm imports are ignored when the realm already exists. Reconcile the
# settings that must also reach installations with a persistent Keycloak DB.
/opt/keycloak/bin/kc.sh "$@" &
keycloak_pid=$!
trap 'kill -TERM "$keycloak_pid" 2>/dev/null || true' INT TERM

attempt=0
until /opt/keycloak/bin/kcadm.sh config credentials \
  --config /tmp/janus-kcadm.config \
  --server http://127.0.0.1:8080/auth \
  --realm master \
  --user "$KC_BOOTSTRAP_ADMIN_USERNAME" \
  --password "$KC_BOOTSTRAP_ADMIN_PASSWORD" >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  if ! kill -0 "$keycloak_pid" 2>/dev/null; then
    wait "$keycloak_pid"
    exit $?
  fi
  if [ "$attempt" -ge 60 ]; then
    echo "Unable to authenticate to Keycloak to reconcile the Nivel36 realm" >&2
    kill -TERM "$keycloak_pid" 2>/dev/null || true
    wait "$keycloak_pid" || true
    exit 1
  fi
  sleep 2
done

/opt/keycloak/bin/kcadm.sh update realms/Nivel36 \
  --config /tmp/janus-kcadm.config \
  -s loginTheme=janus \
  -s internationalizationEnabled=true \
  -s 'supportedLocales=["es","ca","en"]' \
  -s defaultLocale=es

rm -f /tmp/janus-kcadm.config
wait "$keycloak_pid"
