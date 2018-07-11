Debugging
=========

Inspector
---------

The Inspector is the tool co-developed with the VM for debugging.
Launching the Inspector is as simple as using the ``mx inspect`` command.

::

    mx inspect -cp com.oracle.max.tests/bin test.output.GCTest2

The Inspector window should appear in a few moments.
Go `here <./Inspector>`__ for other ways of launching the
Inspector.

Testing Maxine
--------------

Benchmarks
~~~~~~~~~~

The most useful way to test Maxine is to execute some of the standard
benchmarks on the image previously built with the ``mx image`` command.
In this example, we will use the SpecJVM98 and DaCapo benchmarks.
After downloading the benchmarks, set the following environment
variables:

::

    export SPECJVM98_ZIP=/Users/acme/benchmarks/specjvm98.zip
    export DACAPOBACH_JAR=/Users/acme/benchmarks/dacapo-9.12-bach.jar

Then execute the following command:

::

    mx test -insitu -tests=specjvm98,dacapobach
    ----------------------------------------------------------------------------------------
    Running reference       SpecJVM98 _201_compress:               1607 ms
    Running maxvm (std)     SpecJVM98 _201_compress:               3309 ms           2.059x
    ----------------------------------------------------------------------------------------
    ...
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
To be able to run cross-ISA tests Maxine VM relies on the gcc linaro toolchain and qemu.
Assuming an Ubuntu 16.04 LTS installation, the following will install the required packages.

ARMv7
'''''

::

    sudo apt-get install qemu-system-arm gcc-arm-none-eabi gdb-arm-none-eabi

Aarch64
'''''''

Unfortunately for aarch64 the packages provided by ubuntu are not
suitable. Qemu version is 2.5 while we need 2.10 and
``gcc-aarch64-linux-gnu`` although available in the Ubuntu repositories
it does not include ``aarch64-linux-gnu-gdb``, so we need to manually
download both Qemu and the linaro toolchain.

For qemu:

::

    wget https://download.qemu.org/qemu-2.10.1.tar.bz2
    bunzip2 qemu-2.10.1.tar.bz2
    tar xvf qemu-2.10.1.tar
    cd qemu-2.10.1
    mkdir build
    cd build
    ../configure --target-list=aarch64-linux-user,aarch64-softmmu
    make -j
    sudo make install

For gcc toolchain:

::

    wget https://releases.linaro.org/components/toolchain/binaries/7.1-2017.08/aarch64-linux-gnu/gcc-linaro-7.1.1-2017.08-x86_64_aarch64-linux-gnu.tar.xz
    tar xf gcc-linaro-7.1.1-2017.08-x86_64_aarch64-linux-gnu.tar.xz
    export PATH=$PATH:$(pwd)/gcc-linaro-7.1.1-2017.08-x86_64_aarch64-linux-gnu/bin

RISC-V
''''''

QEMU:

::

    git clone https://github.com/riscv/riscv-qemu
    mkdir build
    cd build
    ../configure --target-list=riscv32-softmmu,riscv64-softmmu,riscv32-linux-user,riscv64-linux-user --prefix=/opt/riscv
    make -j
    sudo make install
    export
    PATH=$PATH:/opt/riscv/bin

For the GCC toolchain please follow the instructions from
https://github.com/riscv/riscv-gnu-toolchain

**NOTE**: When debugging RISC-V to make breakpoints work run the
following in gdb

::

    #set riscv use_compressed_breakpoint off

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
