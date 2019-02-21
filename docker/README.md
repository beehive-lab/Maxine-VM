# Running maxineVM in docker

## Prerequisites

The docker image expects the source code to be in the layout shown below:

```
$ tree -L 1 maxine-src
maxine-src
├── maxine
└── mx
```

To get the source code in this layout execute:

```
$ mkdir maxine-src
$ git clone https://github.com/graalvm/mx.git maxine-src/mx
$ git clone https://github.com/beehive-lab/Maxine-VM.git maxine-src/maxine
```

## Creating the docker container

From the directory `maxine-src` (created in [Prerequisites](#prerequisites)) run:

```
docker create -u=$(id -u):$(id -g) \
    --mount src="$(pwd)",target="/maxine-src",type=bind \
    --mount src="$HOME/.mx",target="/.mx",type=bind \
    --mount src="/tmp/.X11-unix",target="/tmp/.X11-unix",type=bind \
    -e DISPLAY=unix$DISPLAY --cap-add=SYS_PTRACE \
    --name maxine-dev -ti beehivelab/maxine-dev
```

This will create a container named `maxine-dev`.

- `-u=$(id -u):$(id -g)` instructs docker to write and read files as the current user instead of root which is the default.
- `--mount src="$(pwd)",target="/maxine-src",type=bind` essentially mounts the current directory to the docker container under the `/maxine-src` directory.
  Similarly, `--mount src="$HOME/.mx",target="/.mx",type=bind` does the same for the `~/.mx` directory.
  Any changes performed to mounted folders outside the docker container are visible in the container and vice versa.
- `--mount src="/tmp/.X11-unix",target="/tmp/.X11-unix",type=bind` mounts the host X11 socket to the container socket.
- `-e DISPLAY=unix$DISPLAY` passes in the `DISPLAY` environment variable.
- `--cap-add=SYS_PTRACE` enables `ptrace` capability for the container.
- `--name maxine-dev` names the new image so that it can later be referenced (to start it, stop it, attach to it etc.).
- `-ti` instructs docker to create an interactive session with a pseudo-tty, to allow us to interact with the container.

## Using the docker container

To use the container issue:

```
docker start -i maxine-dev
```

This will start the container and open a `bash` shell in it.
From this shell you can build and run maxine.

To exit the shell and stop the container type `Ctrl-D`.

## Build the docker image

The image can be found at https://hub.docker.com/r/beehivelab/maxine-dev, however if you want to build it yourself,
enter the directory with the Dockerfile and run:

```
docker build ./ -t beehivelab/maxine-dev
```
