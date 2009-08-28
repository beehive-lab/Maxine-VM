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


/**
 * A class to capture standard arguments to the {@link VMConfiguration} constructor
 * to avoid repetition.
 *
 * @author Mick Jordan
 */
@PROTOTYPE_ONLY
public final class VMConfigurations {

    private VMConfigurations() {
    }

    private static VMPackage defaultCompilerPackage(Platform platform) {
        switch (platform.processorKind.instructionSet) {
            case AMD64:
                return new com.sun.max.vm.compiler.b.c.d.e.amd64.target.Package();
            case SPARC:
                return new com.sun.max.vm.compiler.b.c.d.e.sparc.target.Package();
            default:
                throw FatalError.unimplemented();
        }
    }

    private static VMPackage defaultJitCompilerPackage(Platform platform) {
        switch (platform.processorKind.instructionSet) {
            case AMD64:
                return new com.sun.max.vm.jit.amd64.Package();
            case SPARC:
                return new com.sun.max.vm.jit.sparc.Package();
            default:
                throw FatalError.unimplemented();
        }
    }

    private static VMPackage defaultTargetABIsPackage(Platform platform) {
        switch (platform.processorKind.instructionSet) {
            case AMD64:
                return new com.sun.max.vm.compiler.target.amd64.Package();
            case SPARC:
                return new com.sun.max.vm.compiler.target.sparc.systemV.Package();
            default:
                throw FatalError.unimplemented();
        }
    }

    private static VMPackage defaultHeapPackage() {
        return new com.sun.max.vm.heap.sequential.semiSpace.Package();
    }

    private static VMPackage defaultReferenceScheme() {
        return new com.sun.max.vm.reference.heap.Package();
    }

    private static VMPackage defaultLayoutPackage(Platform platform) {
        if (platform.instructionSet().category == Category.RISC) {
            return new com.sun.max.vm.layout.hom.Package();
        }
        return new com.sun.max.vm.layout.ohm.Package();
    }

    public static VMConfiguration createStandardJit(BuildLevel buildLevel, Platform platform) {
        return new VMConfiguration(buildLevel, platform, new com.sun.max.vm.grip.direct.Package(), defaultReferenceScheme(), defaultLayoutPackage(platform),
                        defaultHeapPackage(), new com.sun.max.vm.monitor.modal.schemes.thin_inflated.Package(), defaultCompilerPackage(platform), defaultJitCompilerPackage(platform), new com.sun.max.vm.trampoline.template.Package(),
                        defaultTargetABIsPackage(platform), new com.sun.max.vm.run.java.Package());
    }

    public static VMConfiguration createStandardInterpreter(BuildLevel buildLevel, Platform platform) {
        return new VMConfiguration(buildLevel, platform, new com.sun.max.vm.grip.direct.Package(), defaultReferenceScheme(), defaultLayoutPackage(platform),
                        defaultHeapPackage(), new com.sun.max.vm.monitor.modal.schemes.thin_inflated.Package(), defaultCompilerPackage(platform), defaultJitCompilerPackage(platform), new com.sun.max.vm.trampoline.template.Package(),
                        defaultTargetABIsPackage(platform), new com.sun.max.vm.run.java.Package());
    }

    public static VMConfiguration createStandard(BuildLevel buildLevel, Platform platform) {
        return createStandard(buildLevel, platform, defaultCompilerPackage(platform));
    }

    public static VMConfiguration createStandard(BuildLevel buildLevel, Platform platform, VMPackage compilerPackage) {
        return new VMConfiguration(buildLevel, platform, new com.sun.max.vm.grip.direct.Package(), defaultReferenceScheme(), defaultLayoutPackage(platform),
                        defaultHeapPackage(), new com.sun.max.vm.monitor.modal.schemes.thin_inflated.Package(), compilerPackage, null, new com.sun.max.vm.trampoline.template.Package(), defaultTargetABIsPackage(platform),
                        new com.sun.max.vm.run.java.Package());
    }

    public static VMConfiguration createPrototype(BuildLevel buildLevel, Platform platform) {
        return new VMConfiguration(buildLevel, platform, new com.sun.max.vm.grip.prototype.Package(), new com.sun.max.vm.reference.prototype.Package(), defaultLayoutPackage(platform),
                        defaultHeapPackage(), new com.sun.max.vm.monitor.prototype.Package(), new com.sun.max.vm.compiler.prototype.Package(), null, new com.sun.max.vm.trampoline.template.Package(), defaultTargetABIsPackage(platform),
                        new com.sun.max.vm.run.java.Package());
    }
}
