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

import static com.sun.max.platform.Platform.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.InstructionSet.Category;
import com.sun.max.platform.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.run.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.trampoline.*;

/**
 * A class to list default scheme implementation and to capture standard {@link VMConfiguration}s.
 *
 * @author Mick Jordan
 */
@HOSTED_ONLY
public final class VMConfigurations {

    private VMConfigurations() {
    }

    /**
     * Gets the package providing the default {@link BootstrapCompilerScheme}.
     */
    public static VMPackage defaultCompilerScheme() {
        switch (platform().instructionSet()) {
            case AMD64:
                return (VMPackage) MaxPackage.fromName("com.sun.max.vm.cps.b.c.d.e.amd64.target");
            case SPARC:
                return (VMPackage) MaxPackage.fromName("com.sun.max.vm.cps.b.c.d.e.sparc.target");
            default:
                throw FatalError.unimplemented();
        }
    }

    /**
     * Gets the package providing the default {@link RuntimeCompilerScheme}.
     */
    public static VMPackage defaultJitCompilerScheme() {
        switch (platform().instructionSet()) {
            case AMD64:
                return (VMPackage) MaxPackage.fromName("com.sun.max.vm.cps.jit.amd64");
            case SPARC:
                return (VMPackage) MaxPackage.fromName("com.sun.max.vm.cps.jit.sparc");
            default:
                throw FatalError.unimplemented();
        }
    }

    /**
     * Gets the package providing the default {@link TargetABIsScheme}.
     */
    public static VMPackage defaultTargetABIsScheme() {
        switch (platform().instructionSet()) {
            case AMD64:
                return new com.sun.max.vm.compiler.target.amd64.Package();
            case SPARC:
                return new com.sun.max.vm.compiler.target.sparc.systemV.Package();
            default:
                throw FatalError.unimplemented();
        }
    }

    /**
     * Gets the package providing the default {@link HeapScheme}.
     */
    public static VMPackage defaultHeapScheme() {
        return new com.sun.max.vm.heap.sequential.semiSpace.Package();
    }

    /**
     * Gets the package providing the default {@link ReferenceScheme}.
     */
    public static VMPackage defaultReferenceScheme() {
        return new com.sun.max.vm.reference.heap.Package();
    }

    /**
     * Gets the package providing the default {@link LayoutScheme}.
     */
    public static VMPackage defaultLayoutScheme() {
        if (platform().instructionSet().category == Category.RISC) {
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

    /**
     * Gets the package providing the default {@link RunScheme}.
     */
    public static VMPackage defaultRunScheme() {
        return new com.sun.max.vm.run.java.Package();
    }

    /**
     * Gets the package providing the default {@link GripScheme}.
     */
    public static VMPackage defaultGripScheme() {
        return new com.sun.max.vm.grip.direct.Package();
    }

    /**
     * Gets the package providing the default {@link MonitorScheme}.
     */
    public static VMPackage defaultMonitorScheme() {
        return new com.sun.max.vm.monitor.modal.schemes.thin_inflated.Package();
    }

    /**
     * Gets the package providing the default {@link DynamicTrampolineScheme}.
     */
    public static VMPackage defaultTrampolineScheme() {
        return new com.sun.max.vm.trampoline.template.Package();
    }

    public static VMConfiguration createStandardJit(BuildLevel buildLevel, Platform platform) {
        return new VMConfiguration(buildLevel, platform,
            defaultGripScheme(),
            defaultReferenceScheme(),
            defaultLayoutScheme(),
            defaultHeapScheme(),
            defaultMonitorScheme(),
            defaultCompilerScheme(),
            defaultJitCompilerScheme(),
            null,
            defaultTrampolineScheme(),
            defaultTargetABIsScheme(),
            defaultRunScheme());
    }

    public static VMConfiguration createStandard(BuildLevel buildLevel, Platform platform) {
        return createStandard(buildLevel, platform,
            defaultCompilerScheme());
    }

    public static VMConfiguration createStandard(BuildLevel buildLevel, Platform platform, VMPackage compilerPackage) {
        return new VMConfiguration(buildLevel, platform,
            defaultGripScheme(),
            defaultReferenceScheme(),
            defaultLayoutScheme(),
            defaultHeapScheme(),
            defaultMonitorScheme(),
            compilerPackage,
            null,
            null,
            defaultTrampolineScheme(),
            defaultTargetABIsScheme(),
            defaultRunScheme());
    }
}
