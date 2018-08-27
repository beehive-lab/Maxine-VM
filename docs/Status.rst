Status
======

Maxine VM is being tested against the `SPECjvm2008 <https://www.spec.org/jvm2008/>`__ and `DaCapo-9.12-bach <http://dacapobench.org/>`__ benchmark suites.
The following tables show the status of each benchmark on each supported platform.

SpecJVM2008
-----------

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
| xml          | FAIL    | FAIL    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| pass-rate    | 90%     | 90%     | 100%      | 55%             |
+--------------+---------+---------+-----------+-----------------+

**Note:** The pass-rate is calculated based on the individual tests of
each group, e.g., compiler contains 2 tests while serial only 1. As a
result, groups have different weights.

DaCapo-9.12-bach
----------------

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
| pmd          | FAIL    | FAIL    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| sunflow      | PASS    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| tomcat       | FAIL    | FAIL    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| tradebeans   | FAIL    | FAIL    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| tradesoap    | FAIL    | FAIL    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| xalan        | PASS    | PASS    | PASS      | PASS            |
+--------------+---------+---------+-----------+-----------------+
| pass-rate    | 62%     | 62%     | 100%      | 92%             |
+--------------+---------+---------+-----------+-----------------+

**Note:** batik fails due to a library that is not available on openJDK,
it is thus omitted from the pass-rate.

Issues
------

Any known issues are reported in the `issue tracker <https://github.com/beehive-lab/Maxine-VM/issues>`__.

For reporting new issues please see :ref:`reporting-bugs-label`.
