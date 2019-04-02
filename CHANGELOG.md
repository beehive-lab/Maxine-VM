# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.7.0] - Unreleased

### Added

- Various new RISC-V instructions in assembler (mostly regarding floating points)
- C1X for RISC-V
- C1X test cases for RISC-V
- Allocation Profiler (-XX:+AllocationProfilerAll -XX:+AllocationProfilerPrintHistogram) gathers stats about object allocations
- Fine grain allocation profiling with -XX:AllocationProfilerEntryPoint -XX:AllocationProfilerExitPoint
- -XX:+LogCompiledMethods to print all runtime compiled methods in the application
- New Jenkinsfile for running benchmarks
- Dockerfile to create docker images for running and developing Maxine VM
- RISC-V partial image build through cross-compilation

### Fixed

- Reflection.getCallerClass()
- mx gen and jni code generation
- Reduced output of mx build
- Simplified conditional and boolean expressions (refactoring)
- Update Cross-ISA infrastructure to use newer tools
- Documentation refactoring and update

## [2.6.0] - 2018-12-05

### Added

- Various new RISC-V instructions in assembler
- T1X for RISC-V
- T1X test cases for RISC-V

### Changed

- Rename nul directory to nulll for MS windows compatibility
- Separated RISC-V 32 from RISC-V 64 ISAs

## [2.5.2] - 2018-10-16

### Fixed

- version in docs

## [2.5.1] - 2018-10-16

### Changed

- Build against OpenJDK8u181-b13
- Transition to upstream mx for building
- Transition changelog to keep-a-changelog format

### Fixed

- Part of the asmdis package for generating assemblers and disassemblers
- AArch46 callsite pathcing
- JDK8 and JSR292 bug and stability fixes

### Removed

- Obsolete methods from crossisa testing infrastructure

## [2.5.0] - 2018-08-27

### Added

- jsr292 support on C1X
- Parallel and asynchronous JIT compilation
- Deoptimization in AArch64
- New way to test cross-isa assemblers

### Fixed

- Inspector fix to work with Java 8
- Bug and stability fixes

## [2.4.2] - 2018-06-26

### Fixed

- Fix checkstyle url

## [2.4.1] - 2018-06-14

### Fixed

- Bug fix that enables the c1xgraal configuration

## [2.4.0] - 2018-05-31

### Added

- Initial port to AArch64 (runs Hello World)
- Implemented AArch64 C1X port
- Implemented first RISC-V instructions

### Fixed

- Bug and stability fixes
- Code refactoring and cleanup

## [2.3.0] - 2018-03-09

### Added

- Implemented AArch64 Assembler
- Implemented AArch64 T1X port, Adapters
- Ported Cross-ISA testing infrastructure to RISC-V

### Fixed

- Bug and stability fixes
- Code refactoring and cleanup

## [2.2.0] - 2017-11-23

### Added

- Support invokedynamic
- Support lambdas (Only on JDK8 builds)
- Support default methods (Only on JDK8 builds)

### Changed

- Build against OpenJDK8u151-b12

### Fixed

- Bug and stability fixes

### Removed

- End support for JDK6

## [2.1.2] - 2017-10-13

### Added

- Support method handles

### Fixed

- Bug and stability fixes

## [2.1.1] - 2017-05-31

### Changed

- Port to latest OpenJDK 7 (u131)

## [2.1.0] - 2017-04-18

### Added

- Enable profile-guided optimizations in Graal (T1X profiling info added)
- ARMv7 execution (T1X, C1X)

### Fixed

- Bug and stability fixes
