#!/bin/bash
set -euo pipefail

# Required input arguments
HEADNODE_IMAGE="${1}"
LOCAL_IMAGE="${2}"

echo "$(date) Pulling image: ${HEADNODE_IMAGE}"
echo "$(date) Local image: ${LOCAL_IMAGE}"

# If the local image exists, exit
if [[ -f "${LOCAL_IMAGE}" ]]; then
    echo "$(date) Local image already exists: ${LOCAL_IMAGE}"
    exit 0
fi

# Use a timeout to prevent the script from running indefinitely
TIMEOUT=${TIMEOUT:-600}
TIMEOUT_INTERVAL=10

# Wait until the lock file is available
while [[ -e "${LOCAL_IMAGE}.lock" ]]; do
    echo "$(date) Waiting for lock file: ${LOCAL_IMAGE}.lock"
    sleep ${TIMEOUT_INTERVAL}
    TIMEOUT=$((TIMEOUT-TIMEOUT_INTERVAL))
    if [[ ${TIMEOUT} -eq 0 ]]; then
        echo "$(date) ERROR: Timeout waiting for lock file: ${LOCAL_IMAGE}.lock"
        exit 1
    fi
done

# If the local image exists, exit
if [[ -f "${LOCAL_IMAGE}" ]]; then
    echo "$(date) Local image already exists: ${LOCAL_IMAGE}"
    exit 0
fi

# Create the lock file
touch "${LOCAL_IMAGE}.lock"

# Remove the lock file when the script exits
trap "rm -f ${LOCAL_IMAGE}.lock" EXIT

# Parse the account ID and region from the image
ACCOUNT_ID=$(echo "${HEADNODE_IMAGE}" | cut -d'.' -f1)
REGION=$(echo "${HEADNODE_IMAGE}" | cut -d'.' -f4)

# Log in to the AWS ECR
echo "$(date) Logging in to AWS ECR registry for ${REGION} region and account ID ${ACCOUNT_ID}"
aws ecr get-login-password --region "${REGION}" | \
    apptainer remote login \
        --password-stdin \
        --username AWS \
        "docker://${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

echo "$(date) Pulling image: ${HEADNODE_IMAGE}"
apptainer pull "${LOCAL_IMAGE}" "docker://${HEADNODE_IMAGE}"
