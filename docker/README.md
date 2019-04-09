# Running maxineVM in docker

## Creating the docker container

From the directory `maxine-src` (created in [Prerequisites](#prerequisites)) run:

```
docker create \
    -p 9873:873 \
    --mount src="/tmp/.X11-unix",target="/tmp/.X11-unix",type=bind \
    -e DISPLAY=unix$DISPLAY --cap-add=SYS_PTRACE \
    --name maxine-dev -ti beehivelab/maxine-dev
```

This will create a container named `maxine-dev`.

- `-p 9873:873` maps port 9873 of the host to port 873 of the docker container.
- `--mount src="/tmp/.X11-unix",target="/tmp/.X11-unix",type=bind` mounts the host X11 socket to the container socket.
- `-e DISPLAY=unix$DISPLAY` passes in the `DISPLAY` environment variable.
- `--cap-add=SYS_PTRACE` enables `ptrace` capability for the container.
- `--name maxine-dev` names the new image so that it can later be referenced (to start it, stop it, attach to it etc.).
- `-ti` instructs docker to create an interactive session with a pseudo-tty, to allow us to interact with the container.

## Initializing the container

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

Then copy the `maxine-src` directory to the container using:

```
$ rsync -avP maxine-src --delete rsync://localhost:9873/maxine-src/
```

### Optionally copy over your `~/.mx` directory

If you use `mx` locally as well you can use the same cache to avoid fetching again large files:

```
$ rsync -avP ~/.mx --delete rsync://localhost:9873/mx/
```

## Using the docker container

To use the container issue:

```
docker start -i maxine-dev
```

This will start the container and open a `bash` shell in it.
From this shell you can build and run maxine.

To exit the shell and stop the container type `Ctrl-D`.

## Keeping your data on the host in sync with your data on the docker container

If you use the container for development purposes you most probably will be interested in editing the source code locally and building and running on the docker container.
To automatically synchronize your files from the host to the container use:

```
fswatch -0 maxine-src | xargs -0 -n 1 -I {} rsync -avP maxine-src --delete rsync://localhost:9873/maxine-src/
```

## Build the docker image

The image can be found at https://hub.docker.com/r/beehivelab/maxine-dev, however if you want to build it yourself,
enter the directory with the Dockerfile and run:

```
docker build ./ -t beehivelab/maxine-dev
```
