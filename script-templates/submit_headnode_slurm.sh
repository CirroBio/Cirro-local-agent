#!/bin/bash
set -euo pipefail

# This script is used to submit the headnode job to the cluster.
# It will be run by the agent process itself, and should take very little time to execute.

# Environment variables required:

# - PROCESS_DIR: The directory which will be used for the headnode execution (absolute path)
# - PROCESS_NAME: The unique identifier for the process
# - HEADNODE_ACCOUNTING: The accounting string to use for the headnode job
# - HEADNODE_LABEL: A more readable label to use for the headnode job
# - HEADNODE_PARTITION: The partition to use for the headnode job
# - HEADNODE_CPUS: The number of CPUs to use for the headnode job
# - HEADNODE_MEM: The amount of memory to use for the headnode job (e.g. 4G)
# - HEADNODE_PRIORITY: The priority to use for the headnode job

sbatch \
    run_headnode.sh \
    --chdir="${PROCESS_DIR}" \
    --error="${PROCESS_DIR}/process.err" \
    --output="${PROCESS_DIR}/process.out" \
    --job-name="${PROCESS_NAME}" \
    --account="${HEADNODE_ACCOUNTING}" \
    --extra="${HEADNODE_LABEL}" \
    --partition="${HEADNODE_PARTITION}" \
    --cpus-per-task="${HEADNODE_CPUS:-4}" \
    --mem="${HEADNODE_MEM:-4G}" \
    --priority="${HEADNODE_PRIORITY:-10}" \
    --parsable
