Working with RISC-V on QEMU
===========================

In this section we describe the environment used by the Maxine VM team to develop and test Maxine VM on RISC-V.
Since the physical RISC-V processors are pretty limited, we rely on the QEMU processor emulator to run Maxine VM on.
Maxine VM also depends on an OS and an OpenJDK distribution to work.
For the OS we use Fedora Rawhide, which ships with OpenJDK zero (interpreter only) version 8u212.

QEMU
----

QEMU 4.1.1 is known to work well.
QEMU 4.2.0 has also been tested but it was causing some issues with ``ssh`` in Fedora.

::

    wget https://download.qemu.org/qemu-4.1.1.tar.xz
    tar -xf qemu-4.1.1.tar.xz
    cd qemu-4.1.1
    ./configure --target-list=riscv64-softmmu,riscv32-softmmu,riscv64-linux-user,riscv32-linux-user --prefix=/opt/riscv
    make -j
    sudo make install

Linux
-----

Pre-built Fedora images for RISV-V can be found at:
https://dl.fedoraproject.org/pub/alt/risc-v/repo/virt-builder-images/images/

The last known working image is the ``20200108`` build.

::

    wget https://dl.fedoraproject.org/pub/alt/risc-v/repo/virt-builder-images/images/Fedora-Minimal-Rawhide-20200108.n.0-fw_payload-uboot-qemu-virt-smode.elf
    wget https://dl.fedoraproject.org/pub/alt/risc-v/repo/virt-builder-images/images/Fedora-Minimal-Rawhide-20200108.n.0-sda.raw.xz
    xz -d Fedora-Minimal-Rawhide-20200108.n.0-sda.raw.xz

and to boot it (with 2 cores and 2G ram)

::

    /opt/riscv/bin/qemu-system-riscv64 \
        -nographic \
        -machine virt \
        -smp 2 \
        -m 2G \
        -kernel Fedora-Minimal-Rawhide-20200108.n.0-fw_payload-uboot-qemu-virt-smode.elf \
        -object rng-random,filename=/dev/urandom,id=rng0 \
        -device virtio-rng-device,rng=rng0 \
        -append "console=ttyS0 ro root=/dev/vda" \
        -device virtio-blk-device,drive=hd0 \
        -drive file=Fedora-Minimal-Rawhide-20200108.n.0-sda.raw,format=raw,id=hd0 \
        -device virtio-net-device,netdev=usernet \
        -netdev user,id=usernet,hostfwd=tcp::10000-:22

After the boot process is completed you can login using ``riscv`` as login and ``fedora_rocks!`` as the password.
SSH is also enabled and you can ssh in the VM through port ``10000`` using the following command.

::

    ssh -p 10000 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o PreferredAuthentications=password -o PubkeyAuthentication=no riscv@localhost

Building Maxine VM for RISC-V
-----------------------------

To build Maxine VM for RISC-V you will need ``riscv64-linux-gnu-gcc`` for the cross-compilation.
To build a RISC-V compatible Maxine VM image issue the following commands (assuming you have followed the instructions in :doc:`Build and Usage Instructions <./build>`).

::

    CC=riscv64-linux-gnu-gcc TARGETISA=riscv64 mx build
    mx image -platform linux-riscv64

Running Maxine VM on the Fedora RISC-V VM
-----------------------------------------

Now that everything is setup we need to copy the generated to image to the Fedora RISC-V VM that is running in QEMU.
To do this we can use the following command::

    rsync -e 'ssh -p 10000 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o PreferredAuthentications=password -o PubkeyAuthentication=no' --progress -av ./com.oracle.max.vm.native riscv@localhost:~/maxine-src/maxine

Note that this requires ``rsync`` to be installed in the Fedora VM.
You can install it using ``dnf install rsync``.

After ``rsync`` completes you can now run Maxine on the Fedora VM like this::

    export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk
    export LD_LIBRARY_PATH=/home/riscv/maxine-src/maxine/com.oracle.max.vm.native/generated/linux
    ~/maxine-src/maxine/com.oracle.max.vm.native/generated/linux/maxvm -jar ~/dacapo-9.12-MR1-bach.jar -t 1 xalan

``~/maxine-src/maxine/com.oracle.max.vm.native/generated/linux/maxvm`` is essentially a substitute for ``java``.
