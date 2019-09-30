Status
======

.. _platform-label:

Supported Platforms
-------------------

Maxine is being developed and tested on the following configurations:

+----------------+----------------------+--------------------------+--------------------+
| Architecture   | OS                   | Java                     | MaxineVM Version   |
+================+======================+==========================+====================+
| X86_64         | Ubuntu 18.04         | OpenJDK 8 (u222)         | 2.9.0              |
+----------------+----------------------+--------------------------+--------------------+
| x86_64         | macOS Mojave 10.14   | OpenJDK 8 (u222)         | 2.9.0              |
+----------------+----------------------+--------------------------+--------------------+
| Aarch64        | Ubuntu 18.04         | OpenJDK 8 (u222)         | 2.9.0              |
+----------------+----------------------+--------------------------+--------------------+
| RISC-V64       | Fedora 31 (on QEMU)  | OpenJDK 8 (u222)         | 2.9.0              |
+----------------+----------------------+--------------------------+--------------------+
| ARMv7          | Ubuntu 16.04         | OpenJDK 7 u151           | 2.4.0              |
+----------------+----------------------+--------------------------+--------------------+

MaxineVM - JDK version compatibility table
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The table below shows the JDK version required to build each version of
MaxineVM.

+--------------------+------------------------+
| MaxineVM Version   | Java Version           |
+====================+========================+
| >= 2.9.0           | Open JDK 8 u222        |
+--------------------+------------------------+
| 2.8.0              | Open JDK 8 u212        |
+--------------------+------------------------+
| 2.7.0              | Open JDK 8 u191        |
+--------------------+------------------------+
| 2.5.1 - 2.6.0      | Open JDK 8 u181        |
+--------------------+------------------------+
| 2.4.0 - 2.5.0      | Open JDK 7 or 8 u151   |
+--------------------+------------------------+
| 2.2 - 2.3.0        | Open JDK 7 or 8 u151   |
+--------------------+------------------------+
| 2.1.1              | Open JDK 7 u131        |
+--------------------+------------------------+
| 2.0 - 2.1.0        | Oracle JDK 7 u25       |
+--------------------+------------------------+
| < 2.0              | Oracle JDK 7 u6        |
+--------------------+------------------------+

To get OpenJDK 7 u151 in Ubuntu 16.04 on x86 you can use the following
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

Maturity
--------

Maxine VM is being tested against the `SPECjvm2008 <https://www.spec.org/jvm2008/>`__ and `DaCapo-9.12-bach-MR1 <http://dacapobench.org/>`__ benchmark suites.
The following tables show the status of each benchmark on each supported platform.

SpecJVM2008
~~~~~~~~~~~

+-----------------------------+---------+---------+---------+---------+
| Benchmark                   | X86     | AArch64 | ARMv7   | RISC-V64|
+=============================+=========+=========+=========+=========+
| startup.helloworld          | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| startup.compiler.compiler   | PASS    | PASS    |         | FAIL    |
+-----------------------------+---------+---------+---------+---------+
| startup.compiler.sunflow    | NA      | NA      | NA      | FAIL    |
+-----------------------------+---------+---------+---------+---------+
| startup.compress            | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| startup.crypto.aes          | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| startup.crypto.rsa          | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| startup.crypto.signverify   | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| startup.mpegaudio           | PASS    | PASS    |         | FAIL    |
+-----------------------------+---------+---------+---------+---------+
| startup.scimark.fft         | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| startup.scimark.lu          | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| startup.scimark.monte_carlo | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| startup.scimark.sor         | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| startup.scimark.sparse      | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| startup.serial              | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| startup.sunflow             | PASS    | PASS    |         | FAIL    |
+-----------------------------+---------+---------+---------+---------+
| startup.xml.transform       | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| startup.xml.validation      | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| scimark.fft.large           | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| scimark.lu.large            | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| scimark.sor.large           | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| scimark.sparse.large        | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| scimark.fft.small           | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| scimark.lu.small            | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| scimark.sor.small           | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| scimark.sparse.small        | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| scimark.monte_carlo         | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| compiler.compiler           | PASS    | PASS    |         | FAIL    |
+-----------------------------+---------+---------+---------+---------+
| compiler.sunflow            | PASS    | PASS    |         | FAIL    |
+-----------------------------+---------+---------+---------+---------+
| compress                    | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| crypto.aes                  | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| crypto.rsa                  | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| crypto.signverify           | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| derby                       | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| mpegaudio                   | PASS    | FAIL    |         | FAIL    |
+-----------------------------+---------+---------+---------+---------+
| serial                      | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| sunflow                     | PASS    | PASS    |         | FAIL    |
+-----------------------------+---------+---------+---------+---------+
| xml.transform               | PASS    | FAIL    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| xml.validation              | PASS    | PASS    |         | PASS    |
+-----------------------------+---------+---------+---------+---------+
| pass-rate                   | 100%    | 94%     |         | 79%     |
+-----------------------------+---------+---------+---------+---------+

.. notes::
    - startup.compiler.sunflow runs infinitely both on Maxine and on HotSpot, thus it's not included in the pass-rate calculation
    - on RISC-V64, startup.compiler.sunflow fails before running infinitely (therefore, we include it in the pass rate).

DaCapo-9.12-bach-MR1
~~~~~~~~~~~~~~~~~~~~

+--------------+-----------+---------+---------+---------+
| Benchmark    | X86       | AArch64 | ARMv7   | RISC-V64|
+==============+===========+=========+=========+=========+
| avrora       | PASS      | PASS    | PASS    | PASS    |
+--------------+-----------+---------+---------+---------+
| batik        | NA        | NA      | NA      | FAIL    |
+--------------+-----------+---------+---------+---------+
| eclipse      | PASS      | FAIL    | FAIL    | FAIL    |
+--------------+-----------+---------+---------+---------+
| fop          | PASS      | PASS    | PASS    | PASS    |
+--------------+-----------+---------+---------+---------+
| h2           | PASS      | PASS    | PASS    | FAIL    |
+--------------+-----------+---------+---------+---------+
| jython       | PASS      | PASS    | PASS    | PASS    |
+--------------+-----------+---------+---------+---------+
| luindex      | PASS      | PASS    | PASS    | PASS    |
+--------------+-----------+---------+---------+---------+
| lusearch     | PASS      | PASS    | PASS    | PASS    |
+--------------+-----------+---------+---------+---------+
| lusearch-fix | PASS      | PASS    | PASS    | PASS    |
+--------------+-----------+---------+---------+---------+
| pmd          | PASS      | PASS    | FAIL    | PASS    |
+--------------+-----------+---------+---------+---------+
| sunflow      | PASS      | PASS    | PASS    | PASS    |
+--------------+-----------+---------+---------+---------+
| tomcat       | PASS      | PASS    | FAIL    | FAIL    |
+--------------+-----------+---------+---------+---------+
| tradebeans   | PASS      | FAIL    | FAIL    | FAIL    |
+--------------+-----------+---------+---------+---------+
| tradesoap    | PASS      | FAIL    | FAIL    | FAIL    |
+--------------+-----------+---------+---------+---------+
| xalan        | PASS      | PASS    | PASS    | PASS    |
+--------------+-----------+---------+---------+---------+
| pass-rate    | 100%      | 78%     | 62%     | 60%     |
+--------------+-----------+---------+---------+---------+

.. notes::
    - batik fails due to a library that is not available on openJDK, it is thus omitted from the pass-rate.
    - on RISC-V64, batik fails before trying to load the missing library from openJDK (therefore, we include it in the pass rate).


Issues
------

Any issues are reported in the `issue tracker <https://github.com/beehive-lab/Maxine-VM/issues>`__.
