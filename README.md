# Maxine VM: A Metacircular VM for Java in Java

#### Updates

* 2018-08-27: Maxine VM 2.5.0 Release
  * jsr292 support on C1X
  * Parallel and asynchronous JIT compilation
  * Deoptimization in AArch64
  * New way to test cross-isa assemblers
  * Inspector fix to work with Java 8
  * Bug and stability fixes
* 2018-06-26: Maxine VM 2.4.2 Release
  * Fix checkstyle url
* 2018-06-14: Maxine VM 2.4.1 Release
  * Bug fix that enables the c1xgraal configuration
* 2018-05-31: Maxine VM 2.4.0 Release
  * Initial port to AArch64 (runs Hello World)
  * Implemented AArch64 C1X port
  * Implemented first RISC-V instructions
  * Bug and stability fixes
  * Code refactoring and cleanup
* 2018-03-09: Maxine VM 2.3.0 Release
  * Implemented AArch64 Assembler
  * Implemented AArch64 T1X port, Adapters
  * Ported Cross-ISA testing infrastructure to RISC-V
  * Bug and stability fixes
  * Code refactoring and cleanup
* 2017-11-23: Maxine VM 2.2.0 Release
  * Build against OpenJDK8u151-b12
  * Support invokedynamic
  * Support lambdas (Only on JDK8 builds)
  * Support default methods (Only on JDK8 builds)
  * End support for JDK6
  * Bug and stability fixes
* 2017-10-13: Maxine VM 2.1.2 Release
  * Support method handles
  * Bug and stability fixes
* 2017-05-31: Maxine VM 2.1.1 Release
  * Port to latest OpenJDK 7 (u131)
* 2017-04-18: Maxine VM 2.1.0 Release
  * Enable profile-guided optimizations in Graal (T1X profiling info
    added)
  * Bug and stability fixes
  * ARMv7 execution (T1X, C1X)

#### Roadmap

Maxine VM's roadmap can be found in the [wiki](https://github.com/beehive-lab/Maxine-VM/wiki#roadmap).

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

For more information please visit
the [Maxine VM's wiki](https://github.com/beehive-lab/Maxine-VM/wiki)
