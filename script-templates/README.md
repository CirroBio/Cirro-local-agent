## Script Examples

### Slurm

Write up on Slurm & Apptainer.

### Local (Docker)

Write up on using Docker locally.

### Environment Variables

It is the agent's responsibility to set the following environment variables, either in the agent configuration on Cirro, or in the `submit_headnode.sh` script.

| Variable                | Description                                           |
|-------------------------|-------------------------------------------------------|
| `PW_ONDEMAND_JOB_QUEUE` | The name of the queue to submit non-restartable jobs. |
| `PW_SPOT_JOB_QUEUE`     | The name of the queue to submit restartable jobs.     |
| `PW_DRAGEN_JOB_QUEUE`   | The name of the queue to submit DRAGEN jobs.          |
