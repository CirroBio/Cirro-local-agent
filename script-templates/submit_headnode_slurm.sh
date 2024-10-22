#!/bin/bash
set -euo pipefail

# This script is used to submit the headnode job to the default SLURM cluster.
# It will be run by the agent process itself, and should take very little time to execute.

# Required Environment Variables:

# - PW_WORKING_DIR: The directory where this analysis is being run
# - PW_SHARED_DIR: The directory for all shared scripts used by the agent
# - PW_ENVIRONMENT_FILE: The path to the environment file for this analysis
# - HEADNODE_NAME: The string used to label the headnode job on the cluster (e.g. the dataset name)
# - HEADNODE_ACCOUNTING: The accounting string to use for the headnode job
# - HEADNODE_JOB_QUEUE: The partition to use for the headnode job

# Optional Environment Variables:

# - HEADNODE_CPUS: The number of CPUs to use for the headnode job
# - HEADNODE_MEM: The amount of memory to use for the headnode job (e.g. 8G)
# - HEADNODE_PRIORITY: The priority to use for the headnode job

# Source the environment variables for this analysis
source "${PW_ENVIRONMENT_FILE}"

# Start the job
echo "Running analysis from $(pwd)"
sbatch \
    --error=process.err \
    --output=process.out \
    --job-name="${HEADNODE_NAME}" \
    --account="${HEADNODE_ACCOUNTING}" \
    --partition="${HEADNODE_JOB_QUEUE}" \
    --cpus-per-task="${HEADNODE_CPUS:-4}" \
    --mem="${HEADNODE_MEM:-8G}" \
    --priority="${HEADNODE_PRIORITY:-10}" \
    --parsable \
    "${PW_SHARED_DIR}/run_headnode.sh"
