#!/bin/bash
set -euo pipefail

source "${PW_ENVIRONMENT_FILE}"

ACCOUNT_ID=$(echo "${HEADNODE_IMAGE}" | cut -d'.' -f1)
REGION=$(echo "${HEADNODE_IMAGE}" | cut -d'.' -f4)

aws ecr get-login-password --region "${REGION}" |
  docker login \
    --username AWS \
    --password-stdin \
    "${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

docker run \
  --env-file "${PW_ENVIRONMENT_FILE}" \
  --volume "${PW_PROJECT_DIR}:${PW_PROJECT_DIR}" \
  --volume "${PW_SHARED_DIR}:${PW_SHARED_DIR}:ro" \
  --workdir "${PW_WORKING_DIR}" \
  "${PW_HEADNODE_IMAGE}"
