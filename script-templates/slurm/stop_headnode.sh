#!/bin/bash
set -euo pipefail

scancel -f --signal=TERM "${PW_JOB_ID}"
