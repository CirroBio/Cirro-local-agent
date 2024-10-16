#!/bin/bash
set -euo pipefail

# Required Environment Variables:
# - CIRRO_AGENT_WORK_DIRECTORY: The root directory for all working files used by the agent
# - PROJECT_ID: The unique identifier for the project
# - DATASET_ID: The unique identifier for the dataset
# - HEADNODE_IMAGE: The image to pull

# If there is a headnode setup script provided, run it
if [[ -f "${CIRRO_AGENT_WORK_DIRECTORY}/helpers/setup_headnode.sh" ]]; then
    echo "$(date) Running headnode setup script"
    export "${CIRRO_AGENT_WORK_DIRECTORY}/helpers/setup_headnode.sh"
fi

# Source the environment variables for this analysis
source env.list

# Internal environment variables
# - PROJECT_DIR: The directory used for all local files (scoped to this particular project)
PROJECT_DIR="${CIRRO_AGENT_WORK_DIRECTORY}/projects/${PROJECT_ID}"
# - DATASET_DIR: The directory used for executing this particular dataset
DATASET_DIR="${PROJECT_DIR}/datasets/${DATASET_ID}"

# Format the path to the local image, replacing special characters
# e.g. HEADNODE_IMAGE=730335334008.dkr.ecr.us-west-2.amazonaws.com/cirro-headnode:slurm-agent
LOCAL_IMAGE="${CIRRO_AGENT_WORK_DIRECTORY}/headnode_images/$(echo ${HEADNODE_IMAGE} | sed '.' '_' | sed ':' '_' | sed '/' '_').sif"

# Pull the headnode image using apptainer
bash apptainer_pull.sh "${HEADNODE_IMAGE}" "${LOCAL_IMAGE}"

echo "$(date) Running headnode image: ${LOCAL_IMAGE}"
echo "$(date) Project directory: ${PROJECT_DIR}"
echo "$(date) Dataset directory: ${DATASET_DIR}"
echo "$(date) Temporary directory: ${TMPDIR}"

# Run the headnode image
apptainer run \
    --containall \
    --bind "${PROJECT_DIR}" \
    --env-file env.list \
    --pwd "${DATASET_DIR}" \
    --workdir "${TMPDIR}" \
    "${LOCAL_IMAGE}" \
    bash /opt/bin/entrypoint.sh
