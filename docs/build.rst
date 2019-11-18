.. highlight:: shell

Build and Usage Instructions
============================

The easiest way to build and use Maxine is through the ``beehivelab/maxine-dev`` `docker image <https://hub.docker.com/r/beehivelab/maxine-dev>`__.
This image comes with all dependencies installed and configured.
You only need to get the latest maxine sources and build them through the container.

.. note::
    If for some reason using docker is not desirable/possible please refer to the end of this page for instructions on building maxine without docker.

Getting the source code
-----------------------

The docker image expects the source code to be in the layout shown below::

    $ tree -L 1 maxine-src
    maxine-src
    ├── maxine
    └── mx

To get the source code in this layout execute::

    $ mkdir maxine-src
    $ git clone https://github.com/graalvm/mx.git maxine-src/mx
    $ git clone https://github.com/beehive-lab/Maxine-VM.git maxine-src/maxine

Developing using a docker container
-----------------------------------

On macOS (and Windows) unfortunately simply mounting host directories on docker containers, although functional, is slow.
To work arround this issue we use `docker-sync <https://docker-sync.readthedocs.io/en/latest/index.html>`__.
Additionally we run an rsync daemon on the docker container, this way users that don't want to use docker-sync can manualy rsync files from the host to the container and vice versa (through ``rsync://localhost:9873/root`` which maps to ``/root/`` on the container).

To create a docker container named ``maxine-dev`` from the ``beehivelab/maxine-dev`` docker image enter ``maxine-src/maxine/docker`` and run::

    docker-compose up --no-start

Note that the container is now created but does not contain the source code.

Keeping your data on the host in sync with your data on the docker container
''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

Install `docker-rsync <https://docker-sync.readthedocs.io/en/latest/index.html>`__::

    gem install docker-sync

To start it enter ``maxine-src/maxine/docker/`` and run::

    docker-sync start

To stop it::

    docker-sync stop

Using the ``maxine-dev`` container
''''''''''''''''''''''''''''''''''

To use the container issue ``docker start -i maxine-dev``.
This will start the container and open a ``bash`` shell in it.
From this shell you can build and run maxine.
As long as docker-sync is running any changes you make on the host or the container will become visible to the counterpart as well.

To exit the shell and stop the container type ``Ctrl-D``.

Build
-----

- Enter the maxine source directory::

    cd $MAXINE_HOME

- Compile the source code::

    mx build

Executing ``mx build`` in the ``$MAXINE_HOME`` directory compiles the Java source code of Maxine to class files using ``javac`` (or the Eclipse batch compiler if you use the ``-jdt`` option) and compiles the native code of Maxine to executable code using your platform's C compiler.

The build process attempts to download some necessary files from the internet.
If you are behind a firewall set the ``HTTP_PROXY`` environment variable appropriately before starting the build.

- Generate the boot image::

    mx image

The ``mx image`` command is used to generate a boot image.
This command runs Maxine on a host JVM to configure a prototype, then compiles its own code and data to create an executable program for the target platform.

Running
-------

With the native substrate and a boot image built, the Maxine VM can now be executed.

The ``mx vm`` command handles the details of class and library paths and provides an interface similar to the standard java launcher command.

The ``mx`` script includes a command to run a simple HelloWorld program to verify that the VM is working::

    mx helloworld

Now let's use Maxine to run a more substantial program::

    mx vm -cp test/bin test.output.GCTest2

To launch the VM (or any other command for that matter) without using ``mx``, the ``-v`` option echoes the commands issued by the mx script::

    mx -v helloworld

Creating a Maxine-based JDK
---------------------------

To create a Maxine-based JDK that can serve as a replacement for OpenJDK or OracleJDK issue::

    mx makejdk

This will create the directory ``$MAXINE_HOME/maxjdk`` which you can now use as the ``JAVA_HOME`` for running java with Maxine.

Profiling
---------

Various profiling tools are available for the Java platform, with varying degrees of overhead.
Some tools require VM support and the Maxine VM includes two such tools.
The first is a simple sampling based profiler with minimal overhead that is provided in the standard VM image and enabled by the ``-Xprof`` command line option.
The second tool is the :doc:`Virtual Machine Level Analysis <./Virtual-Machine-Level-Analysis>` (VMA) system that works by instrumenting compiled code.
Using VMA requires a custom VM image to be built.

Sampling Profiler
~~~~~~~~~~~~~~~~~

Maxine includes a simple sampling-based profiler.
It is enabled with the ``-Xprof`` command line option.
The full syntax for the option is ``-Xprof:frequency=f,depth=d,dump=s,flat=t,sort=t,systhreads=t``, where everything after the ``-Xprof`` is optional.
The control arguments have the following interpretation:

-  **frequency=f**: Sets the frequency of the samples to ``f``
   milliseconds.
   The default is 10.
-  **depth=d**: Records the stacks of threads at sample points to a
   depth of ``d``.
   The default is 16.
-  **dump=s**: Dumps the accumulated stack traces every s seconds.
   The default is zero which results in the traces being output only at
   VM termination.
-  **sort=t**: Sorts the stack traces by thread and sample counts if t
   is true.
   The default value is true unless dump is non-zero, as the sorting
   incurs both CPU and allocation overhead.
   In unsorted mode the stack traces are output in an arbitrary order,
   each followed by the list of threads and sample counts for that
   trace.
   In sorted mode, the traces for each thread are output separately,
   with the traces ordered from highest to lowest sample count.
-  **flat=t**: If t is true, the output is sorted and, for each sample,
   only the method at the top of the stack is listed.
   Therefore, this option also implies ``depth=1``.
   The default value is ``true``.
-  **systhreads=t**: Include system (VM) threads in the analysis if
   ``t``
   is ``true``.
   The default is false.

If the ``=t`` in the truth-valued options is omitted, it is the same as ``t=true``.

The profiler is implemented as a separate thread that wakes up periodically, based on the given frequency (slightly randomized), stops all threads and records their stack traces.
Since threads only stop at safepoints there is some inevitable inaccuracy in the reported trace.
In particular, a hot method that contains no loops will not appear in the output.
However, the stack trace will likely show the closest caller that contains a loop (or a system call that will cause the thread to reach a safepoint).

The data is output using the Maxine log mechanism, so can be captured in a file by setting the ``MAXINE_LOG_FILE`` environment variable.

Choice of Optimizing Compiler
-----------------------------

Maxine provides two optimizing compilers, C1X and Graal.
The former, an evolution of the Hostpot client compiler, is very stable but no longer under development.
Graal is more akin to the Hotspot server compiler and is under active development and improvement.
The default image build still uses C1X as the optimizing compiler, but it is possible to select Graal, both for runtime compilations and for compiling the VM boot image (the latter is currently unstable).
To build a boot image with Graal as the runtime optimizing compiler, use the following command::

 mx image @c1xgraal

In this case the optimizing compiler is actually a hybrid of C1X and Graal, with C1X being used as a fallback option if the Graal compilation fails.
Note that the VM boot image is considerably larger (~100MB) with Graal included.

To compile the boot image itself with Graal, do::

 mx image @c1xgraal-boot

The Graal-compiled VM boot image will execute a few simple test programs but currently is not robust enough to be the default.

Building Maxine without docker
------------------------------

Dependencies
~~~~~~~~~~~~

Maxine depends on the `MX tool <https://github.com/graalvm/mx>`__ for its build process.
To get it and add it to your ``PATH`` execute::

 sudo apt-get install python           # MX depends on python
 mkdir -p $WORKDIR
 cd $WORKDIR
 git clone https://github.com/graalvm/mx
 export PATH=$PATH:$(pwd)/mx

Maxine also depends on openJDK 8.
To see the supported jdk versions please refer to the :doc:`Status <./Status>` page.
To get it from the ubuntu repositories run::

 sudo apt-get install openjdk-8-jdk

Maxine is open source software, licensed under the GPL version 2.0 and is hosted on `GitHub <https://github.com/beehive-lab/Maxine-VM>`__.
Since Maxine is hosted in a git repository we need to install ``git`` as well::

 sudo apt-get install git

Environment variables
~~~~~~~~~~~~~~~~~~~~~

To build maxine natively we first need to define a number of environment variables:

#. Define the directory you want to work in::

    export WORKDIR=/path/to/workdir

#. Define the JDK to be used::

    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

#. Define ``MAXINE_HOME``::

    export MAXINE_HOME=$WORKDIR/maxine

#. Optionally (needed to run ``maxvm`` binary directly):

  * Extend ``PATH`` to include the *to be generated* ``maxvm``::

     export PATH=$PATH:$MAXINE_HOME/com.oracle.max.vm.native/generated/linux/

  * Define ``LD_LIBRARY_PATH``::

     export LD_LIBRARY_PATH=$MAXINE_HOME/com.oracle.max.vm.native/generated/linux/

Get the source code
~~~~~~~~~~~~~~~~~~~

#. Make sure the project directory exists and enter it::

    mkdir -p $WORKDIR
    cd $WORKDIR

#. Get the Maxine VM source code::

    git clone --recursive https://github.com/beehive-lab/Maxine-VM.git maxine

This command will create a directory named ``maxine`` with the contents checked out from the git repository.
