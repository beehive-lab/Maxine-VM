Status
======

.. _platform-label:

Supported Platforms
-------------------

Maxine is being developed and tested on the following configurations:

+----------------+----------------------+--------------------------+--------------------+
| Architecture   | OS                   | Java                     | MaxineVM Version   |
+================+======================+==========================+====================+
| X86_64         | Ubuntu 18.04         | OpenJDK 8 (u191)         | 2.7.0              |
+----------------+----------------------+--------------------------+--------------------+
| x86_64         | macOS Mojave 10.14.3 | OpenJDK 8 (u181)         | 2.7.0              |
+----------------+----------------------+--------------------------+--------------------+
| Aarch64        | Ubuntu 16.04/18.04   | OpenJDK 8 (u181)         | 2.6.0              |
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
| >= 2.7.0           | Open JDK 8 u191        |
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

Maxine VM is being tested against the `SPECjvm2008 <https://www.spec.org/jvm2008/>`__ and `DaCapo-9.12-bach <http://dacapobench.org/>`__ benchmark suites.
The following tables show the status of each benchmark on each supported platform.

SpecJVM2008
~~~~~~~~~~~

+--------------+---------+---------+-----------+-----------------+
| Benchmark    | ARMv7   | AArch64 | X86 C1X   | X86 C1X-Graal   |
+==============+=========+=========+===========+=================+
| startup      | PASS    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| compiler     | PASS    | PASS    | PASS      | FAIL            |
+--------------+---------+---------+-----------+-----------------+
| compress     | PASS    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| crypto       | PASS    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| derby        | FAIL    | FAIL    | PASS      | FAIL            |
+--------------+---------+---------+-----------+-----------------+
| scimark      | PASS    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| serial       | PASS    | PASS    | PASS      |                 |
+--------------+---------+---------+-----------+-----------------+
| sunflow      | FAIL    | FAIL    | PASS      | FAIL            |
+--------------+---------+---------+-----------+-----------------+
| xml          | FAIL    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| pass-rate    | 90%     | 92%     | 100%      | 55%             |
+--------------+---------+---------+-----------+-----------------+

**Note:** The pass-rate is calculated based on the individual tests of
each group, e.g., compiler contains 2 tests while serial only 1. As a
result, groups have different weights.

DaCapo-9.12-bach
~~~~~~~~~~~~~~~~

+--------------+---------+---------+-----------+-----------------+
| Benchmark    | ARMv7   | AArch64 | X86 C1X   | X86 C1X-Graal   |
+==============+=========+=========+===========+=================+
| avrora       | PASS    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| batik        | FAIL    | FAIL    | FAIL      | FAIL            |
+--------------+---------+---------+-----------+-----------------+
| eclipse      | FAIL    | FAIL    | PASS      | FAIL            |
+--------------+---------+---------+-----------+-----------------+
| fop          | PASS    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| h2           | PASS    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| jython       | PASS    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| luindex      | PASS    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| lusearch     | PASS    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| pmd          | FAIL    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| sunflow      | PASS    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| tomcat       | FAIL    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| tradebeans   | FAIL    | FAIL    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| tradesoap    | FAIL    | FAIL    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| xalan        | PASS    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| pass-rate    | 62%     | 77%     | 100%      | 92%             |
+--------------+---------+---------+-----------+-----------------+

**Note:** batik fails due to a library that is not available on openJDK,
it is thus omitted from the pass-rate.

Issues
------

Any issues are reported in the `issue tracker <https://github.com/beehive-lab/Maxine-VM/issues>`__.
