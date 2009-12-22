/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm;

import com.sun.max.annotate.*;
import com.sun.max.asm.InstructionSet.*;
import com.sun.max.platform.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.trampoline.template.Package;

/**
 * A class to capture standard arguments to the {@link VMConfiguration} constructor
 * to avoid repetition.
 *
 * @author Mick Jordan
 */
@HOSTED_ONLY
public final class VMConfigurations {

    private VMConfigurations() {
    }

    public static VMPackage defaultCompilerScheme(Platform platform) {
        switch (platform.processorKind.instructionSet) {
            case AMD64:
                return new com.sun.max.vm.compiler.cps.b.c.d.e.amd64.target.Package();
            case SPARC:
                return new com.sun.max.vm.compiler.cps.b.c.d.e.sparc.target.Package();
            default:
                throw FatalError.unimplemented();
        }
    }

    public static VMPackage defaultJitCompilerScheme(Platform platform) {
        switch (platform.processorKind.instructionSet) {
            case AMD64:
                return new com.sun.max.vm.jit.amd64.Package();
            case SPARC:
                return new com.sun.max.vm.jit.sparc.Package();
            default:
                throw FatalError.unimplemented();
        }
    }

    public static VMPackage defaultTargetABIsScheme(Platform platform) {
        switch (platform.processorKind.instructionSet) {
            case AMD64:
                return new com.sun.max.vm.compiler.target.amd64.Package();
            case SPARC:
                return new com.sun.max.vm.compiler.target.sparc.systemV.Package();
            default:
                throw FatalError.unimplemented();
        }
    }

    public static VMPackage defaultHeapScheme() {
        return new com.sun.max.vm.heap.sequential.semiSpace.Package();
    }

    public static VMPackage defaultReferenceScheme() {
        return new com.sun.max.vm.reference.heap.Package();
    }

    public static VMPackage defaultLayoutScheme(Platform platform) {
        if (platform.instructionSet().category == Category.RISC) {
            // On SPARC, the HOM layout enables more optimized code for accessing array elements
            // smaller than a word as there is no need to perform address arithmetic to skip
            // over the header; the origin is pointing at array element 0.
            // A disadvantage of HOM is that the cell and origin addresses
            // of an object are not one and the same (which they are for OHM)
            // and converting between them requires reading memory.
            return new com.sun.max.vm.layout.hom.Package();
        }
        return new com.sun.max.vm.layout.ohm.Package();
    }

    public static com.sun.max.vm.run.java.Package defaultRunScheme() {
        return new com.sun.max.vm.run.java.Package();
    }

    public static com.sun.max.vm.grip.direct.Package defaultGripScheme() {
        return new com.sun.max.vm.grip.direct.Package();
    }

    public static com.sun.max.vm.monitor.modal.schemes.thin_inflated.Package defaultMonitorScheme() {
        return new com.sun.max.vm.monitor.modal.schemes.thin_inflated.Package();
    }

    public static Package defaultTrampolineScheme() {
        return new com.sun.max.vm.trampoline.template.Package();
    }

    public static VMConfiguration createStandardJit(BuildLevel buildLevel, Platform platform) {
        return new VMConfiguration(buildLevel, platform,
            defaultGripScheme(),
            defaultReferenceScheme(),
            defaultLayoutScheme(platform),
            defaultHeapScheme(),
            defaultMonitorScheme(),
            defaultCompilerScheme(platform),
            defaultJitCompilerScheme(platform),
                null, defaultTrampolineScheme(),
            defaultTargetABIsScheme(platform),
            defaultRunScheme());
    }

    public static VMConfiguration createStandardJit(BuildLevel buildLevel, Platform platform, VMPackage jitPackage) {
        return new VMConfiguration(buildLevel, platform,
            defaultGripScheme(),
            defaultReferenceScheme(),
            defaultLayoutScheme(platform),
            defaultHeapScheme(),
            defaultMonitorScheme(),
            defaultCompilerScheme(platform),
            jitPackage,
                null, defaultTrampolineScheme(),
            defaultTargetABIsScheme(platform),
            defaultRunScheme());
    }

    public static VMPackage defaultMonitorPackage() {
        return new com.sun.max.vm.monitor.modal.schemes.thin_inflated.Package();
    }

    public static VMConfiguration createStandardInterpreter(BuildLevel buildLevel, Platform platform) {
        return new VMConfiguration(buildLevel, platform,
            defaultGripScheme(),
            defaultReferenceScheme(),
            defaultLayoutScheme(platform),
            defaultHeapScheme(),
            defaultMonitorScheme(),
            defaultCompilerScheme(platform),
            defaultJitCompilerScheme(platform),
                null, defaultTrampolineScheme(),
            defaultTargetABIsScheme(platform),
            defaultRunScheme());
    }

    public static VMConfiguration createStandard(BuildLevel buildLevel, Platform platform) {
        return createStandard(buildLevel, platform,
            defaultCompilerScheme(platform));
    }

    public static VMConfiguration createStandard(BuildLevel buildLevel, Platform platform, VMPackage compilerPackage) {
        return new VMConfiguration(buildLevel, platform,
            defaultGripScheme(),
            defaultReferenceScheme(),
            defaultLayoutScheme(platform),
            defaultHeapScheme(),
            defaultMonitorScheme(),
            compilerPackage,
            null,
                null, defaultTrampolineScheme(),
            defaultTargetABIsScheme(platform),
            defaultRunScheme());
    }

    public static VMConfiguration createPrototype(BuildLevel buildLevel, Platform platform) {
        return new VMConfiguration(buildLevel, platform,
            new com.sun.max.vm.grip.prototype.Package(),
            new com.sun.max.vm.reference.prototype.Package(),
            defaultLayoutScheme(platform),
            defaultHeapScheme(),
            new com.sun.max.vm.monitor.prototype.Package(),
            new com.sun.max.vm.compiler.prototype.Package(),
            null,
                null, defaultTrampolineScheme(),
            defaultTargetABIsScheme(platform),
            defaultRunScheme());
    }
}
