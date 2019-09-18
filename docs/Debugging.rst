Debugging
=========

Inspector
---------

The Inspector is the tool co-developed with the VM for debugging.
Launching the Inspector is as simple as using the ``mx inspect`` command.

::

    mx inspect -cp com.oracle.max.tests/bin test.output.GCTest2

The Inspector window should appear in a few moments.
Go :doc:`here <./Inspector>` for other ways of launching the
Inspector.

Testing Maxine
--------------

Benchmarks
~~~~~~~~~~

The most useful way to test Maxine is to execute some of the standard
benchmarks on the image previously built with the ``mx image`` command.
In this example, we will use the DaCapo benchmarks.
After downloading the benchmark suite from https://sourceforge.net/projects/dacapobench/files/9.12-bach-MR1/dacapo-9.12-MR1-bach.jar, set the following environment
variable:

::

    export DACAPOBACH_JAR=/Users/acme/benchmarks/dacapo-9.12-MR1-bach.jar

Then execute the following command:

::

    mx testme -insitu -tests=dacapo
    ----------------------------------------------------------------------------------------
    Running reference       DaCapo-bach avrora:                    3960 ms
    Running maxvm (std)     DaCapo-bach avrora:                   13815 ms           3.488x
    ----------------------------------------------------------------------------------------
    ...

Note that the harness used to run the benchmarks against a reference VM
is a little brittle.
In particular, it compares the output written to ``stdout`` by the two
executions and if they don't match, it determines the Maxine VM
execution to have failed.
We've noticed that for some benchmarks on some platforms, the execution
output is not such a reliable fingerprint.
To see the actual output of the benchmark, and the exact command used to
run it, look in the ``maxine-tester/insitu`` directory.

Regression testing
~~~~~~~~~~~~~~~~~~

Maxine includes a set of tests that are useful in catching regressions
and measuring progress when making changes.
The three types of tests included in the distribution are described below.

JUnit tests
^^^^^^^^^^^

Tests the Maxine code base prior to building and running the VM.
For example, there are JUnit tests for the general purpose utility
classes in the Base project.
There are also JUnit tests that use the various IR interpreters to
ensure that each level of IR in Maxine's compiler produces the correct
output.

VM tests
^^^^^^^^

These are tests that are executed on the VM.
The first subcategory of VM tests are very simple unit tests that test a
specific VM feature and/or Java bytecode instruction in isolation (i.e.
avoiding as many other VM features as possible).
To ensure strong isolation, these tests are built into the boot image
and executed in such a way that precludes using a more general testing
framework such as JUnit.
The second subcategory of VM tests are called output tests.
These tests are comprised of standard Java programs (i.e.
they have a class containing a main method) that produce some
deterministic output via System.out.
These tests compare the output of these programs when run under Maxine
VM and another trustworthy JVM (such as HotSpot).

Cross-ISA tests
^^^^^^^^^^^^^^^

When porting Maxine VM's compilers to a new Instruction Set Architecture `QEMU <https://www.qemu.org/>`__ is utilized to virtually run unit tests and regress the correctness of the generated code.
To be able to run cross-ISA tests Maxine VM relies on gcc cross-compilers, gdb-multilib and qemu.
The easiest way to run the cross-ISA tests is by using the ``beehivelab/maxine-dev`` `docker image <https://hub.docker.com/r/beehivelab/maxine-dev>`__.

If you insist on running natively, assuming an Ubuntu 18.04 LTS installation, the following will install the required packages.

ARMv7 and AArch64
'''''''''''''''''

::

    sudo apt-get install \
        gdb-multilib qemu-system-arm \
        gcc-aarch64-linux-gnu gcc-arm-none-eabi

RISC-V
''''''

GCC:

::

    sudo apt-get install gcc-riscv64-linux-gnu

GDB:

::

    sudo apt-get install wget texinfo
    mkdir /tmp/riscv
    cd /tmp/riscv/
    wget https://ftp.gnu.org/gnu/gdb/gdb-8.2.1.tar.xz
    tar xf gdb-8.2.1.tar.xz
    cd /tmp/riscv/gdb-8.2.1
    ./configure --target=riscv32-elf,riscv64-elf --disable-multilib --prefix=/opt/riscv
    make -j
    sudo make install

QEMU:

::

    sudo apt-get install libglib2.0-dev libpixman-1-dev flex bison
    wget https://download.qemu.org/qemu-3.1.0.tar.xz
    tar xf qemu-3.1.0.tar.xz
    cd /tmp/riscv/qemu-3.1.0
    ./configure --target-list=riscv64-softmmu,riscv32-softmmu,riscv64-linux-user,riscv32-linux-user --prefix=/opt/riscv
    make -j
    sudo make install

Don't forget to add ``/opt/riscv`` to ``PATH``.

.. _logging-tracing-label:

Logging and Tracing
-------------------

Maxine provides two related mechanisms for logging and/or tracing the
behavior of the VM, manual string-based logging using the
``com.sun.max.vm.Log`` class, or more automated, type-based logging, that
is integrated with the :doc:`Inspector <./Inspector>`, using
``com.sun.max.vm.log.VMLogger``.
These are related in that ``VMLogger`` includes string based logging as an
option and so can replace the use of ``Log``.
Currently the VM uses a mixture of these two mechanisms, with conversion
being done opportunistically.
For simplicity, we will use the term tracing to describe string-based
logging in the following.
If you are adding logging to a VM component you are strongly encouraged
to use the ``VMLogger`` approach.

Manual Tracing
~~~~~~~~~~~~~~

Use the class ``com.sun.max.vm.Log`` to do manual tracing.
The class includes a variety of methods for printing objects of various
types.
By default the output goes to the standard output but can be re-directed
to a file by setting the environment variable ``MAXINE_LOG_FILE`` before
running the VM.
To selectively enable specific tracing in the VM, define a
``com.sun.max.vm.VMOption`` with the name ``-XX:+TraceXXX``, where ``XXX``
identifies the tracing.

You should avoid string concatenation (or any other code involving
allocation) in tracing code, especially inside a ``VmOperation``.
While this should not break the VM (allocation will fail fast with an
error message if a VM operation does not allow it), allocation can add
noise to your logs.
Lastly, if the logging sequence involves more than one logging
statement, you should bound the sequence with this pattern:

.. code:: java

    boolean lockDisabledSafepoints = Log.lock();
    // multiple calls to Log.print...() methods
    Log.unlock(lockDisabledSafepoints);

This will serialize logging performed by multiple threads.
Of course, it will also serialize the execution of the VM and may well
make the race you are trying to debug disappear!

Native Code Tracing
~~~~~~~~~~~~~~~~~~~

Maxine provides some tracing of the small amount of native code that
supports the VM.
By default this is conditionally compiled out of the VM image but can be
selectively enabled by editing ``com.oracle.max.vm.native/share/log.h``
and rebuilding with ``mx build`` and rebuilding the VM image.
This is particularly useful if the the VM crashes during startup.
For example to enable all tracing set ``log-ALL`` to 1.

Type-based Logging
~~~~~~~~~~~~~~~~~~

In type-based logging, the actual values that you want to log are passed
to an instance of the ``com.sun.max.vm.log.VMLogger`` class using methods
defined in the class.
Evidently, at the ``VMLogger`` level, type-based logging is something of a
misnomer, as it cannot know the types of the actual values.
In practice the values are logged as untyped ``Word`` values, but
extensive automated support is provided to handle the conversion to/from
``Word`` types.
The optional tracing support is driven from the values in the log.
For more details see :doc:`Type-based Logging <./Type-based-Logging>`.

Debugging Maxine Java Tasks
---------------------------

The Maxine project includes a number of Java programs that can be
launched as commands of the ``mx`` script.
For example, the ``mx image`` command described above runs the
``com.sun.max.vm.prototype.BootImageGenerator`` class on a host JVM.
This simplest way to debug such a command is to use the ``-d`` global
option of the mx script.
This will launch the Java program with extra options telling it to wait
and listen for a JDWP-capable debugger on port 8000.
You then configure a JDWP-enabled debugger to attach to this port.

The advantage of this approach is that you can easily launch the command
with different command line arguments without having to create/modify an
IDE launch configuration.

Core dump
---------

To get a core dump from a Maxine VM process, it is simplest to do ``gcore <pid>`` from another shell.
This forces a core dump but does not terminate the process, which
continues after the dump is taken.
An alternative is to use ``kill -s ABRT <pid>`` which does kill the
process after the dump is taken.
One other difference is that ``gcore`` allows the path to the core dump
file to be specified with the ``-c <corefile>`` option, whereas ``kill``
puts it in a default location, typically ``core`` in the current working
directory.

It is possible to force a core dump on a fatal VM error by setting the
option ``-XX:+CoreOnError`` when running the VM.

The following invocation:

::

    mx inspect --mode=attach --target=file --location=dumpfile

will then bring up the Inspector on the core dump.
If you omit the ``--location`` argument, it will put up a dialog box.

Unfortunately this will only work if the associated Maxine VM was run
with the ``-XX:+MakeInspectable`` option, otherwise some key data
structures needed by the Inspector will not have been created.
