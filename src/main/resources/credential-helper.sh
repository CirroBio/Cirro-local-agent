#!/bin/bash
# https://docs.aws.amazon.com/cli/v1/userguide/cli-configure-sourcing-external.html

for i in "$@"; do
  case $i in
    --session-id=*)
      SESSION_ID="${i#*=}"
      shift
      ;;
    --agent-endpoint=*)
      AGENT_ENDPOINT="${i#*=}"
      shift
      ;;
    -*)
      echo "Unknown option $i"
      exit 1
      ;;
    *)
      ;;
  esac
done

curl "${AGENT_ENDPOINT}/executions/${SESSION_ID}/s3-token"
