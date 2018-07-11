Status
======

Benchmarks
----------

Maxine VM is tested against the `SPECjvm2008 <https://www.spec.org/jvm2008/>`__ and `DaCapo-9.12-bach <http://dacapobench.org/>`__ benchmark suites.
The following tables show the status of each benchmark on each supported platform.

SpecJVM2008
~~~~~~~~~~~

+--------------+---------+-----------+-----------------+
| Benchmark    | ARMv7   | X86 C1X   | X86 C1X-Graal   |
+==============+=========+===========+=================+
| startup      | PASS    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| compiler     | PASS    | PASS      | FAIL            |
+--------------+---------+-----------+-----------------+
| compress     | PASS    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| crypto       | PASS    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| derby        | FAIL    | PASS      | FAIL            |
+--------------+---------+-----------+-----------------+
| scimark      | PASS    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| serial       | PASS    | PASS      |                 |
+--------------+---------+-----------+-----------------+
| sunflow      | FAIL    | PASS      | FAIL            |
+--------------+---------+-----------+-----------------+
| xml          | FAIL    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| pass-rate    | 90%     | 100%      | 55%             |
+--------------+---------+-----------+-----------------+

**Note:** The pass-rate is calculated based on the individual tests of
each group, e.g., compiler contains 2 tests while serial only 1. As a
result, groups have different weights.

DaCapo-9.12-bach
~~~~~~~~~~~~~~~~

+--------------+---------+-----------+-----------------+
| Benchmark    | ARMv7   | X86 C1X   | X86 C1X-Graal   |
+==============+=========+===========+=================+
| avrora       | PASS    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| batik        | FAIL    | FAIL      | FAIL            |
+--------------+---------+-----------+-----------------+
| eclipse      | FAIL    | PASS      | FAIL            |
+--------------+---------+-----------+-----------------+
| fop          | PASS    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| h2           | PASS    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| jython       | PASS    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| luindex      | PASS    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| lusearch     | PASS    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| pmd          | FAIL    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| sunflow      | PASS    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| tomcat       | FAIL    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| tradebeans   | FAIL    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| tradesoap    | FAIL    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| xalan        | PASS    | PASS      | PASS            |
+--------------+---------+-----------+-----------------+
| pass-rate    | 62%     | 100%      | 92%             |
+--------------+---------+-----------+-----------------+

**Note:** batik fails due to a library that is not available on openJDK,
it is thus omitted from the pass-rate.

Features
--------

Some of the features of Maxine that make it a compelling platform for (J)VM research include:

-  Nearly all of the code base is written in Java and exploits advanced language features appearing in JDK 5 and beyond: for example annotations, static imports, and generics.
-  The VM integrates with openJDK.
   There's no need to download (and build) other implementations of the standard Java classes.
-  The source code supports development in Eclipse, Netbeans or IntelliJ all of which provide excellent support for cross-referencing and browsing the code.
   It also means that refactoring can be confidently employed to continuously improve the structure of the code.
-  `The Maxine Inspector <./Inspector>`__ produces visualizations of nearly every aspect of the VM runtime state, and provides advanced, VM-specific debugging.
-  The workspace includes ``mx``, a powerful command line tool for building the VM as well as launching other programs in the code base, such as the `Inspector <./Inspector>`__.
-  The source code is hosted on GitHub making downloading and collaboration easier.

Issues
------

Any known issues are reported in the `issue tracker <https://github.com/beehive-lab/Maxine-VM/issues>`__.

For reporting new issues please see `Reporting Bugs <./intro#reporting-bugs>`__.
