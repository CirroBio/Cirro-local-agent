#!/bin/bash
# https://docs.aws.amazon.com/cli/v1/userguide/cli-configure-sourcing-external.html

# Check if environment variables are set
if [ -z "$CIRRO_AGENT_ENDPOINT" ]; then
  echo "CIRRO_AGENT_ENDPOINT is not set"
  exit 1
fi

if [ -z "$CIRRO_EXECUTION_ID" ]; then
  echo "CIRRO_EXECUTION_ID is not set"
  exit 1
fi

if [ -z "$CIRRO_AGENT_TOKEN" ]; then
  echo "CIRRO_AGENT_TOKEN is not set"
  exit 1
fi

curl "${CIRRO_AGENT_ENDPOINT}/executions/${CIRRO_EXECUTION_ID}/s3-token" \
  -X POST \
  -H "Authorization: Bearer ${CIRRO_AGENT_TOKEN}"
