Build and Usage Instructions
============================

.. _platform-label:

Platform
--------

Maxine is being developed and tested on the following configurations:

+----------------+----------------------+--------------------------+--------------------+
| Architecture   | OS                   | Java                     | MaxineVM Version   |
+================+======================+==========================+====================+
| X86            | Ubuntu 16.04/18.04   | OpenJDK 7 and 8 (u151)   | 2.4.0              |
+----------------+----------------------+--------------------------+--------------------+
| Aarch64        | Ubuntu 16.04         | OpenJDK 7 and 8 (u151)   | 2.4.0              |
+----------------+----------------------+--------------------------+--------------------+
| ARMv7          | Ubuntu 16.04         | OpenJDK 7 u151           | 2.4.0              |
+----------------+----------------------+--------------------------+--------------------+

To get OpenJDK 8 u151 in Ubuntu 16.04 on x86 you can use the following
debian packages:

.. code-block:: shell

    cd /tmp

    export ARCH=amd64                      # or arm64
    export JAVA_VERSION=7u151-2.6.11-3     # or 8u151-b12-1
    export JAVA=openjdk-7                  # or openjdk-8
    export FCONFIG_VERSION=2.12.3-0.2
    export BASE_URL=http://snapshot.debian.org/archive/debian/20171124T100538Z

    for package in jre jre-headless jdk dbg; do
    wget ${BASE_URL}/pool/main/o/${JAVA}/${JAVA}-${package}_${JAVA_VERSION}_${ARCH}.deb
    done

    for package in fontconfig-config libfontconfig1; do
    wget ${BASE_URL}/pool/main/f/fontconfig/${package}_${FCONFIG_VERSION}_all.deb
    done

    wget http://ftp.uk.debian.org/debian/pool/main/libj/libjpeg-turbo/libjpeg62-turbo_1.5.1-2_${ARCH}.deb

    sudo dpkg -i ${JAVA}-jdk_${JAVA_VERSION}_${ARCH}.deb ${JAVA}-jre_${JAVA_VERSION}_${ARCH}.deb ${JAVA}-jre-headless_${JAVA_VERSION}_${ARCH}.deb ${JAVA}-dbg_${JAVA_VERSION}_${ARCH}.deb libjpeg62-turbo_1.5.1-2_${ARCH}.deb fontconfig-config_${FCONFIG_VERSION}_all.deb libfontconfig1_${FCONFIG_VERSION}_all.deb
    sudo apt-get install -f

MaxineVM - JDK version compatibility table
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The table below shows the JDK version required to build each version of
MaxineVM.

+--------------------+------------------------+
| MaxineVM Version   | Java Version           |
+====================+========================+
| >= 2.4.0           | Open JDK 7 or 8 u151   |
+--------------------+------------------------+
| 2.2 - 2.3.0        | Open JDK 7 or 8 u151   |
+--------------------+------------------------+
| 2.1.1              | Open JDK 7 u131        |
+--------------------+------------------------+
| 2.0 - 2.1.0        | Oracle JDK 7 u25       |
+--------------------+------------------------+
| < 2.0              | Oracle JDK 7 u6        |
+--------------------+------------------------+

Structure of the Source Code
----------------------------

Maxine depends on the Graal compiler which, although originally started in the Maxine project, is now independent and hosted in the OpenJDK project on java.net.
Since Graal evolves independently from Maxine, it is necessary to use a specific version of Graal that is known to be compatible with Maxine.
In addition, currently Maxine requires some patches to Graal, so we provide the patched version as `a separate git
repository <https://github.com/beehive-lab/Maxine-Graal>`__.

When downloaded, the Maxine and Graal repositories should be placed in
the file system in sibling directories, for example
/Users/acme/ws/maxine and /Users/acme/ws/graal.

Environment variables
---------------------

#. Define the directory you want to work in:

   ::

       export WORKDIR=/path/to/workdir

#. Define the JDK to be used:

   ::

       export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-$ARCH

#. Define ``MAXINE_HOME``:

   ::

       export MAXINE_HOME=$WORKDIR/maxine

#. Define ``GRALL_HOME``:

   ::

       export GRAAL_HOME=$WORKDIR/graal

#. Extend ``PATH`` to include the ``mx`` tool and the *to be generated*
   maxvm:

   ::

       export PATH=$PATH:$GRAAL_HOME/mxtool/:$MAXINE_HOME/com.oracle.max.vm.native/generated/Linux/

#. Define ``LD_LIBRARY_PATH``:

   ::

      $ export LD_LIBRARY_PATH=$MAXINE_HOME/com.oracle.max.vm.native/generated/linux/

Building Maxine
---------------

Maxine can be developed, built and run either from the command line in a shell or within an IDE.
This flexibility should suit a wide range of common workflows and personal preferences.
For instance, developing in an IDE is productive for browsing, modifying, and refactoring source code, while other tasks such as running the VM with different command line options are better suited to the shell.

The mx script
~~~~~~~~~~~~~

To simplify working in the shell, the Maxine code base includes a Python script that provides a command line interface to the Maxine system.
This script is modeled after the ``hg`` executable that comes with Mercurial.
The script is found in the ``$GRAAL_HOME/mxtool`` directory and is named ``mx.py``.
The prerequisites for using this script are:

::

    /usr/bin/python must be Python 2.7

In addition to a set of global commands, ``mx`` supports the concept of a "suite", which is a set of projects that comprise the suite, dependencies on other suites, and a set of suite-specific commands.
The current working directory in which ``mx`` is executed defines the "primary" suite.
When working with Maxine, Maxine is the primary suite and Graal is the dependent.
The Maxine suite-specific information is stored in the mxmaxine subdirectory.
Note, however, that in Graal it is stored in the mx subdirectory, not mxgraal.

Get the source code
~~~~~~~~~~~~~~~~~~~

Maxine is open source software, licensed under the GPL version 2.0 and is hosted on `GitHub <https://github.com/beehive-lab/Maxine-VM>`__.

This paragraph provides instructions on how to check out the Maxine source code using Git.
To use these instructions, first install Git so that the ``git`` command is available in your shell.
Once you can successfully execute the ``git`` command, you can checkout Maxine Source Code by using the ``git clone`` command.

#. Create a directory for the project and enter it:

   ::

       mkdir $WORKDIR
       cd $WORKDIR

#. Get the Maxine VM source code:

   ::

       git clone https://github.com/beehive-lab/Maxine-VM.git maxine

This command will create a directory named ``maxine`` with the contents checked out from the git repository.

#. Get the Graal compiler source code:

   ::

       git clone https://github.com/beehive-lab/Maxine-Graal.git graal

This command will create a directory named ``graal`` with the contents checked out from the git repository.

Updating your workspace with the latest changes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Later, when updates are available, you can use the standard git commands to request the changes:

::

    git pull

Whenever you pull new changes into your working directory, it's important to do a refresh.
If you are developing on the command line, then you should run mx clean before running mx build.
If you are developing in an IDE, then you need to perform the IDE-specific "refresh" action to inform it that the underlying source files may have changed.
For example, in Eclipse, this means selecting all the projects in the Package Explorer view and performing a refresh ``File -> Refresh``.

For more information on how to use Git, see the `Git site <https://git-scm.com/>`__.

Build
~~~~~

#. Enter the maxine source directory:

   ::

       cd $MAXINE_HOME

#. Compile the source code:

   ::

       mx build

Executing ``mx build`` in the ``$MAXINE_HOME`` directory compiles the Java source code of Maxine to class files using ``javac`` (or the Eclipse batch compiler if you use the ``-jdt`` option) and compiles the native code of Maxine to executable code using your platform's C compiler.

The build process attempts to download some necessary files from the internet.
If you are behind a firewall set the ``HTTP_PROXY`` environment variable appropriately before starting the build.

#. Generate the boot image:

   ::

       mx image

The ``mx image`` command is used to generate a boot image.
This command runs Maxine on a host JVM to configure a prototype, then compiles its own code and data to create an executable program for the target platform.

Choice of Optimizing Compiler
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Maxine provides two optimizing compilers, C1X and Graal.
The former, an evolution of the Hostpot client compiler, is very stable but no longer under development.
Graal is more akin to the Hotspot server compiler and is under active development and improvement.
The default image build still uses C1X as the optimizing compiler, but it is possible to select Graal, both for runtime compilations and for compiling the VM boot image (the latter is currently unstable).
To build a boot image with Graal as the runtime optimizing compiler, use the following command:

::

    mx image @c1xgraal

In this case the optimizing compiler is actually a hybrid of C1X and Graal, with C1X being used as a fallback option if the Graal compilation fails.
Note that the VM boot image is considerably larger (~100MB) with Graal included.

To compile the boot image itself with Graal, do:

::

    mx image @c1xgraal-boot

The Graal-compiled VM boot image will execute a few simple test programs but currently is not robust enough to be the default.

Running
-------

With the native substrate and a boot image built, the Maxine VM can now be executed.

The ``mx vm`` command handles the details of class and library paths and provides an interface similar to the standard java launcher command.

The ``mx`` script includes a command to run a simple HelloWorld program to verify that the VM is working.

::

    mx helloworld

Now let's use Maxine to run a more substantial program.

::

    mx vm -cp com.oracle.max.tests/bin test.output.GCTest2

To launch the VM (or any other command for that matter) without using ``mx``, the ``-v`` option echoes the commands issued by the mx script.

::

    mx -v helloworld

Debugging
---------

Please see :doc:`Debugging <./Debugging>`.

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
