#!/bin/bash
# https://docs.aws.amazon.com/cli/v1/userguide/cli-configure-sourcing-external.html

# Check if environment variables are set
if [ -z "$AGENT_ENDPOINT" ]; then
  echo "AGENT_ENDPOINT is not set"
  exit 1
fi

if [ -z "$AGENT_EXECUTION_ID" ]; then
  echo "AGENT_EXECUTION_ID is not set"
  exit 1
fi

if [ -z "$AGENT_TOKEN" ]; then
  echo "AGENT_TOKEN is not set"
  exit 1
fi

curl "${AGENT_ENDPOINT}/executions/${AGENT_EXECUTION_ID}/s3-token" \
  -X POST \
  -H "Authorization: Bearer ${AGENT_TOKEN}"
