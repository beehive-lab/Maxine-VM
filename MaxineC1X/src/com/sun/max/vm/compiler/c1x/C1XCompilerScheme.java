/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.VMConfiguration.*;

import com.sun.c1x.*;
import com.sun.c1x.target.amd64.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;
import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Ben L. Titzer
 */
public class C1XCompilerScheme extends AbstractVMScheme implements RuntimeCompilerScheme {

    private MaxRiRuntime c1xRuntime;
    private C1XCompiler c1xCompiler;
    private RiXirGenerator c1xXirGenerator;
    private CiTarget c1xTarget;

    public static MaxRiRuntime globalRuntime;
    public static final VMIntOption c1xOptLevel = VMOptions.register(new VMIntOption("-C1X:OptLevel=", 1,
        "Set the optimization level of C1X.") {
            @Override
            public boolean parseValue(com.sun.max.unsafe.Pointer optionValue) {
                boolean result = super.parseValue(optionValue);
                if (result) {
                    C1XOptions.setOptimizationLevel(getValue());
                    return true;
                }
                return false;
            }
        }, MaxineVM.Phase.STARTING);


    @HOSTED_ONLY
    public C1XCompilerScheme() {
        super();
        globalRuntime = new MaxRiRuntime(this);
    }

    @Override
    public <Type extends TargetMethod> Class<Type> compiledType() {
        Class<Class<Type>> type = null;
        return Utils.cast(type, C1XTargetMethod.class);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (MaxineVM.isHosted()) {
            if (phase == MaxineVM.Phase.BOOTSTRAPPING) {
                if (MaxineVM.isHosted()) {
                    VMOptions.addFieldOptions("-C1X:", C1XOptions.class, C1XOptions.helpMap);
                }
                // create the RiRuntime object passed to C1X
                c1xRuntime = new MaxRiRuntime(this);
                // create the CiTarget object passed to C1X
                MaxRiRegisterConfig config = new MaxRiRegisterConfig(vmConfig());
                InstructionSet isa = platform().instructionSet();
                CiArchitecture arch;
                switch (isa) {
                    case AMD64:
                        arch = new AMD64();
                        break;
                    default:
                        throw FatalError.unimplemented();
                }

                TargetABI<?, ?> targetABI = vmConfig().targetABIsScheme().optimizedJavaABI;

                int wordSize = arch.wordSize;

                c1xTarget = new CiTarget(arch, config, true, wordSize, wordSize, wordSize, targetABI.stackFrameAlignment, platform().pageSize, wordSize, wordSize, 16, false);
                c1xXirGenerator = new MaxXirGenerator(vmConfig(), c1xTarget, c1xRuntime);
                c1xCompiler = new C1XCompiler(c1xRuntime, c1xTarget, c1xXirGenerator);
            } else if (phase == MaxineVM.Phase.COMPILING) {
                // can only refer to JavaPrototype while bootstrapping.
                JavaPrototype.javaPrototype().loadPackage("com.sun.c1x", true);
            }
        }
    }

    public final TargetMethod compile(final ClassMethodActor classMethodActor) {
        RiMethod method = classMethodActor;
        CiTargetMethod compiledMethod = c1xCompiler.compileMethod(method, c1xXirGenerator).targetMethod();
        if (compiledMethod != null) {
            C1XTargetMethod c1xTargetMethod = new C1XTargetMethod(classMethodActor, compiledMethod);
            CompilationScheme.Inspect.notifyCompilationComplete(c1xTargetMethod);
            return c1xTargetMethod;
        }
        throw FatalError.unexpected("bailout"); // compilation failed
    }

    @HOSTED_ONLY
    public static C1XCompilerScheme create() {
        C1XCompilerScheme compilerScheme = new C1XCompilerScheme();
        compilerScheme.initialize(MaxineVM.Phase.BOOTSTRAPPING);
        return compilerScheme;
    }

    @Override
    public CallEntryPoint calleeEntryPoint() {
        return CallEntryPoint.OPTIMIZED_ENTRY_POINT;
    }

    public final MaxRiRuntime getRuntime() {
        return c1xRuntime;
    }

    public final C1XCompiler getCompiler() {
        return c1xCompiler;
    }

    public final RiXirGenerator getXirGenerator() {
        return c1xXirGenerator;
    }

    public final CiTarget getTarget() {
        return c1xTarget;
    }
}
