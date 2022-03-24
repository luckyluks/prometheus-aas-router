#!/bin/sh

# Abort on any error (including if wait-for-it fails).
set -e

# Wait for the backend to be up, if we know where it is.
if [ -n "$PROMETHEUS_HOST" ]; then
  /app/wait-for-it.sh "$PROMETHEUS_HOST:${PROMETHEUS_PORT:-9090}"
fi

sleep 3

# Run the main container command.
exec "$@"
