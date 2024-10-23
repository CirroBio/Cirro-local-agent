#!/bin/bash
set -euo pipefail

# Required Environment Variables:
# - PW_WORKING_DIR: The directory where this analysis is being run
# - PW_SHARED_DIR: The directory for all shared scripts used by the agent
# - PW_ENVIRONMENT_FILE: The path to the environment file for this analysis
# - PW_PROJECT_DIR: The unique identifier for the project
# - PW_HEADNODE_IMAGE: The image to pull

# Source the environment variables for this analysis
source "${PW_ENVIRONMENT_FILE}"

# Format the path to the local image, replacing special characters
# e.g. HEADNODE_IMAGE=730335334008.dkr.ecr.us-west-2.amazonaws.com/cirro-headnode:slurm-agent
IMAGE_NAME=$(echo "${PW_HEADNODE_IMAGE}" | tr '.' '_' | tr ':' '_' | tr '/' '_')
LOCAL_IMAGE="${PW_SHARED_DIR}/headnode_images/${IMAGE_NAME}.sif"

# Environment-specific setup to be passed into the headnode
# Append to the environment file
cat <<EOF >> "${PW_ENVIRONMENT_FILE}"
export APPTAINER_CACHEDIR="${PW_PROJECT_DIR}/apptainer"
export WORKER_PRIORITY=5
export PW_ONDEMAND_JOB_QUEUE="campus-new"
export PW_SPOT_JOB_QUEUE="campus-new"
export PW_DRAGEN_JOB_QUEUE="campus-new"
EOF

# Load the apptainer dependency
ml Apptainer/1.1.6

# Pull the headnode image using apptainer
mkdir -p "${PW_SHARED_DIR}/headnode_images"
mkdir -p "${PW_PROJECT_DIR}/apptainer"
bash apptainer_pull.sh "${PW_HEADNODE_IMAGE}" "${LOCAL_IMAGE}"

echo "$(date) Running headnode image: ${LOCAL_IMAGE}"
echo "$(date) Project directory: ${PW_PROJECT_DIR}"
echo "$(date) Dataset directory (working): ${PW_WORKING_DIR}"
echo "$(date) Temporary directory: ${TMPDIR}"

mkdir -p "${TMPDIR}/.nextflow"

# Run the headnode image
apptainer run \
    --containall \
    --bind "${PW_PROJECT_DIR}" \
    --env-file "${PW_ENVIRONMENT_FILE}" \
    --pwd "${PW_WORKING_DIR}" \
    --workdir "${TMPDIR}" \
    --bind "${TMPDIR}/.nextflow":"$HOME/.nextflow" \
    --bind "${PW_SHARED_DIR}:${PW_SHARED_DIR}:ro" \
    "${LOCAL_IMAGE}"
