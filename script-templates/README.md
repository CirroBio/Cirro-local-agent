# Scripts

The `submit_headnode.sh` script is responsible for starting the headnode container on the local compute resource.

At a minimum the script should start the headnode container with the following options:
- Bind the `PW_PROJECT_DIR` and `PW_SHARED_DIR` (read only) directories.
- Load the environment variables from the `${PW_ENVIRONMENT_FILE}` file.
- Set `PW_WORKING_DIR` as the working directory.

### Environment Variables

It is the agent's responsibility to set the following environment variables, either in the agent configuration on Cirro, or in the `submit_headnode.sh` script.

| Variable                | Description                                           |
|-------------------------|-------------------------------------------------------|
| `PW_ONDEMAND_JOB_QUEUE` | The name of the queue to submit non-restartable jobs. |
| `PW_SPOT_JOB_QUEUE`     | The name of the queue to submit restartable jobs.     |

#### Provided Environment Variables

The environment variables below are provided by Cirro.
You can use them in your scripts for various purposes, such as naming the Cluster job, or adding metadata to the job.

| Variable            | Description                                  |
|---------------------|----------------------------------------------|
| `PW_DATASET`        | The ID of the dataset being processed.       |
| `PW_PROJECT`        | The ID of the project being processed.       |
| `PW_HEADNODE_IMAGE` | URI to the headnode container image          |
| `PW_WORKFLOW`       | The name of the workflow being run.          |
| `PW_EXECUTOR`       | The executor being used (Nextflow/Cromwell). |

### Headnode Image

The headnode image is a Docker image that contains the necessary software and scripts to run the workflow.

If you have additional dependencies that are not included in the base image,
you can create your own image that extends the base image.

Inspect the `PW_HEADNODE_IMAGE` environment variable to determine the base image to use.

It is your responsibility to ensure that the image kept up-to-date with the latest software versions and security patches.

## Examples

### Slurm

An example using Slurm and Apptainer is provided in the [slurm](slurm) directory.

### Local (Docker)

An example using Docker is provided in the [local](local) directory.

