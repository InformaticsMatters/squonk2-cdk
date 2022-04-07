# squonk2-cdk

CDK jobs repository contains Java code running molecular property predictions using the [Chemistry Development Kit](https://cdk.github.io/).
These are made available as a "tool", typically represented as a Java class.
Those tools become available as:
- a command line application
- a command line application running in a Docker container
- a Squonk2 job that runs using the Docker container 

For more details on Squonk2 jobs look at our main [virtual-screening](https://github.com/InformaticsMatters/virtual-screening) repo that
contains more documentation and examples.

We anticipate a wider range of functions being incoporated as additional tools in the futures. Suggestions and contributions are welcome.

One purpose of this repo is as a prototype for creating Squonk2 jobs using Java (most current ones are Python based).

This repo illustrates:
- Creating a Java class that can be executed as CLI application. See [DescriptorsExec.java](app/src/main/java/squonk/jobs/cdk/DescriptorsExec.java).
  We use the [Apache Commons CLI](https://commons.apache.org/proper/commons-cli/) library for handling the command line options.
- Use the [Gradle](https://gradle.org/) build tool for building everything. See [app/build.gradle](app/build.gradle) for details.
- Create a Docker container image (details in the build.gradle) that is pushed to [DockerHub](https://hub.docker.com/r/informaticsmatters/squonk2-cdk).
- Write the Squonk2 job definition, including a [jote]() test. See [data-manager/cdk.yaml](https://github.com/InformaticsMatters/data-manager-job-tester)


## Running

**To run a tool through gradle**:
```shell
$ ./gradlew run --args="--all -i ../data/dhfr_3d.sdf -o foo.sdf"

> Task :app:run
2022-04-06T09:05:19+00:00 # INFO -EVENT- squonk.jobs.cdk.DescriptorsExec --all -i ../data/dhfr_3d.sdf -o foo.sdf
2022-04-06T09:05:19+00:00 # INFO -EVENT- Calculating 10 descriptors
2022-04-06T09:05:22+00:00 # INFO -EVENT- Processed 756 molecules, 0 errors.
2022-04-06T09:05:22+00:00 # INFO -COST- 7560.0 1

BUILD SUCCESSFUL in 3s
```
This runs the default class which reads a SDF, calculates all properties and writes out a new SDF with those 
calculations as additional properties. See the build.gradle `application` task for more details.
Note that this is effectively executed from the `app` directory, hence the path to the test SDF.

**Build the docker container image**:
```shell
$ ./gradlew dockerBuildImage
```
Builds an image named `informaticsmatters/squonk2-cdk:latest`.

**Running the docker container**:
```shell
$ docker run -it -v $PWD:$PWD -w $PWD -u 1000:1000 informaticsmatters/squonk2-cdk:latest java squonk.jobs.cdk.DescriptorsExec --all -i data/dhfr_3d.sdf -o foo.sdf
2022-04-06T09:14:40+00:00 # INFO -EVENT- squonk.jobs.cdk.DescriptorsExec --all -i data/dhfr_3d.sdf -o foo.sdf
2022-04-06T09:14:40+00:00 # INFO -EVENT- Calculating 10 descriptors
2022-04-06T09:14:44+00:00 # INFO -EVENT- Processed 756 molecules, 0 errors.
2022-04-06T09:14:44+00:00 # INFO -COST- 7560.0 1
```
