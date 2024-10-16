#!/bin/bash
set -euo pipefail

# This script is used to submit the headnode job to the default SLURM cluster.
# It will be run by the agent process itself, and should take very little time to execute.

# Required Environment Variables:

# - CIRRO_AGENT_WORK_DIRECTORY: The root directory for all working files used by the agent
# - PROJECT_ID: The unique identifier for the project
# - DATASET_ID: The unique identifier for the dataset within the project
# - HEADNODE_NAME: The string used to label the headnode job on the cluster (e.g. the dataset name)
# - HEADNODE_ACCOUNTING: The accounting string to use for the headnode job
# - HEADNODE_JOB_QUEUE: The partition to use for the headnode job

# Optional Environment Variables:

# - HEADNODE_CPUS: The number of CPUs to use for the headnode job
# - HEADNODE_MEM: The amount of memory to use for the headnode job (e.g. 8G)
# - HEADNODE_PRIORITY: The priority to use for the headnode job

# Raise an error if the DATASET_ID variable is empty or not set
[[ -z "${DATASET_ID}" ]] && exit 1

# Internal environment variables
DATASET_DIR="${CIRRO_AGENT_WORK_DIRECTORY}/projects/${PROJECT_ID}/datasets/${DATASET_ID}"

# Start the job
cd ${DATASET_DIR}
echo "Running analysis from ${DATASET_DIR}"
sbatch \
    ${DATASET_DIR}/run_headnode.sh \
    --error=process.err \
    --output=process.out \
    --job-name="${HEADNODE_NAME}" \
    --account="${HEADNODE_ACCOUNTING}" \
    --partition="${HEADNODE_JOB_QUEUE}" \
    --cpus-per-task=${HEADNODE_CPUS:-4} \
    --mem="${HEADNODE_MEM:-8G}" \
    --priority=${HEADNODE_PRIORITY:-10} \
    --parsable
