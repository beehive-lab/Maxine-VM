# Maxine VM: A Metacircular VM for Java in Java

#### News

* 2017-04-18: Maxine VM 2.1 Release
  * Enable profile-guided optimizations in Graal (T1X profiling info
    added)
  * Bug and stability fixes
  * ARMv7 execution (T1X, C1X)

#### Publications

For the original Maxine VM please cite:  
[C. Wimmer et al, “Maxine: An approachable virtual machine for, and in, java”, In ACM TACO 2013.](http://dl.acm.org/citation.cfm?id=2400689&dl=ACM&coll=DL&CFID=748733895&CFTOKEN=73017278)

For Maxine VM >= v2.1 please cite:  
[Christos Kotselidis, et al. Heterogeneous Managed Runtime Systems: A Computer Vision Case Study. In 13th ACM SIGPLAN/SIGOPS International Conference on Virtual Execution Environments (VEE), 2017.](http://dl.acm.org/citation.cfm?id=3050764)

#### Acknowledgments

This work in Maxine VM is partially supported by EPSRC grants Anyscale
EP/L000725/1, PAMELA EP/K008730/1, DOME EP/J016330/1, and EU Horizon
2020 ACTiCLOUD 732366 grant.

#### Users Mailing list

A mailing list is also available to discuss topics related to Maxine VM.

maxinevm@googlegroups.com

#### Collaborations

We welcome collaborations! Please
contact
[Christos Kotselidis](mailto:christos.kotselidis@manchester.ac.uk) for
external collaborations.

#### Relevant Projects

[MaxSim: A simulation platform for Managed Applications, Andrey Rodchenko](https://github.com/beehive-lab/MaxSim)

## Wiki

[Here](https://web.archive.org/web/20150516045940/https://wikis.oracle.com/display/MaxineVM/Home) and
[here](https://community.oracle.com/community/java/java_hotspot_virtual_machine/maxine-vm) you
can find the original wiki pages (by Oracle) describing the internals of Maxine
VM.

## Status

### Benchmarks

Maxine VM is tested against
the [SPECjvm2008](https://www.spec.org/jvm2008/)
and [DaCapo-9.12-bach](http://dacapobench.org/) benchmark suites.  The
following tables show the status of each benchmark on each supported
platform.

#### SpecJVM2008

| Benchmark  | ARMv7 | X86 C1X | X86 C1X-Graal |
| ---------- | ----- | ------- | ------------- |
| startup    | PASS  | PASS    | PASS          |
| compiler   | PASS  | PASS    | FAIL          |
| compress   | PASS  | PASS    | PASS          |
| crypto     | PASS  | PASS    | PASS          |
| derby      | FAIL  | PASS    | FAIL          |
| scimark    | PASS  | PASS    | PASS          |
| sunflow    | PASS  | PASS    | FAIL          |
| xml        | FAIL  | PASS    | PASS          |

#### DaCapo-9.12-bach

| Benchmark  | ARMv7 | X86 C1X | X86 C1X-Graal |
| ---------- | ----- | ------- | ------------- |
| avrora     | PASS  | PASS    | PASS          |
| batik      | FAIL  | FAIL    | FAIL          |
| eclipse    | FAIL  | PASS    | FAIL          |
| fop        | FAIL  | PASS    | PASS          |
| h2         | FAIL  | PASS    | PASS          |
| jython     | PASS  | PASS    | PASS          |
| luindex    | PASS  | PASS    | PASS          |
| lusearch   | PASS  | PASS    | PASS          |
| sunflow    | PASS  | PASS    | PASS          |
| pmd        | FAIL  | PASS    | PASS          |
| tomcat     | FAIL  | PASS    | PASS          |
| tradebeans | FAIL  | PASS    | PASS          |
| tradesoap  | FAIL  | PASS    | PASS          |
| xalan      | PASS  | PASS    | PASS          |

### Milestones

1. Upgrade to latest Graal, Implement JVMCI ([Foivos Zakkak](https://github.com/zakkak))
2. Run Truffle on top of Maxine VM/Graal ([Christos Kotselidis](https://github.com/kotselidis))
3. Port MMTk to Maxine VM ([Foivos Zakkak](https://github.com/zakkak), [Christos Kotselidis](https://github.com/kotselidis))
4. ARM AArch64 Port ([Tim Hartley]())

### Issues

Any known issues are reported in
the [issue tracker](https://github.com/beehive-lab/Maxine-VM/issues).

For reporting new issues please see [Reporting Bugs](#reporting-bugs).

## Build and Usage Instructions

### Platform

Maxine VM is being developed and tested on the following configurations:

| Architectures | OS           | Java          |
| ------------- | ------------ | ------------- |
| X86           | Ubuntu 14.04 | OpenJDK 7 u25 |
| ARMv7         | Ubuntu 14.04 | OpenJDK 7 u25 |

### Environment variables

1. Define the directory you want to work in:
   ```
   export WORKDIR=/path/to/workdir
   ```

2. Define the JDK to be used:
   ```
   export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64
   ```

3. Define `MAXINE_HOME`:
   ```
   export MAXINE_HOME=$WORKDIR/maxine
   ```

4. Extend `PATH` to include the `mx` tool and the *to be generated* maxvm:
   ```
   export PATH=$PATH:$WORKDIR/graal/mxtool/:$MAXINE_HOME/com.oracle.max.vm.native/generated/linux/
   ```

### Get the source code

1. Create a directory for the project and enter it:
   ```
   mkdir $WORKDIR
   cd $WORKDIR
   ```

2. Get the Maxine VM source code:
   ```
   git clone https://github.com/beehive-lab/Maxine-VM.git maxine
   ```

3. Get the Graal compiler source code:
   ```
   git clone https://github.com/beehive-lab/Maxine-Graal.git graal
   ```

### Build

1. Enter the maxine source directory:
   ```
   cd $MAXINE_HOME
   ```

2. Build the source code:
   ```
   mx build
   ```

3. Build the boot image:
   ```
   mx image
   ```

### Usage

Run Java programs using `maxvm` instead of `java`, e.g. `maxvm
HelloWorld`.

### Inspector

To run the Maxine Inspector issue `mx inspect`.

## Contributing

We welcome contributions! Use pull requests on Github. However, note
that we are doing most development in a private git tree and we are
working on a number of features which are not quite ready for public
release. Therefore, we would strongly encourage you to get in touch
before starting to work on anything large, to avoid duplication of
effort. We can probably expedite our release of any WIP features you
might be interested in, if you do that.

### Reporting Bugs

If you think you have found a bug which is not in the list of Known
issues, please open a new
issue [here](https://github.com/beehive-lab/Maxine-VM/issues), on
Github. However, note that we have limited time available to investigate
and fix bugs which are not affecting the workloads we are
using. Therefore, if you can't pinpoint the cause of the bug yourself,
we ask that you provide as many details on how to reproduce it, and
preferably provide a statically linked executable which triggers it.
