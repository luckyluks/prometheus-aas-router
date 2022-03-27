#!/bin/sh

# Abort on any error (including if wait-for-it fails).
set -e

# Wait for prometheus to be up, if we know where it is.
if [ -n "$PROMETHEUS_HOST" ]; then
  /app/wait-for-it.sh "$PROMETHEUS_HOST:${PROMETHEUS_PORT:-9090}"
fi

# Wait for the aas-server to be up, if we know where it is.
if [ -n "$AAS_SERVER_HOST" ]; then
  /app/wait-for-it.sh "$AAS_SERVER_HOST:${AAS_SERVER_PORT:-4001}"
fi

# Additional setup-time required before polling
sleep 5

# Run the main container command.
exec "$@"
