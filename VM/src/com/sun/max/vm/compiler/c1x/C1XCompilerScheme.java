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

import com.sun.c1x.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.xir.*;
import com.sun.max.asm.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.lang.Function;

/**
 * @author Ben L. Titzer
 */
public class C1XCompilerScheme extends AbstractVMScheme implements RuntimeCompilerScheme {

    private MaxRiRuntime c1xRuntime;
    private C1XCompiler compiler;
    private RiXirGenerator xirGenerator;

    public static final VMIntOption c1xOptLevel;

    static {
        if (MaxineVM.isHosted()) {
            c1xOptLevel = VMOptions.register(new VMIntOption("-C1X:OptLevel=", 1,
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
        } else {
            c1xOptLevel = null;
        }
    }

    public C1XCompilerScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.BOOTSTRAPPING) {
            if (MaxineVM.isHosted()) {
                VMOptions.addFieldOptions("-C1X:", C1XOptions.class);
            }
            // create the RiRuntime object passed to C1X
            c1xRuntime = MaxRiRuntime.globalRuntime;
            CiTarget c1xTarget = createTarget(vmConfiguration());
            xirGenerator = new MaxXirGenerator(vmConfiguration(), c1xTarget, c1xRuntime);
            compiler = new C1XCompiler(c1xRuntime, c1xTarget, xirGenerator);
            compiler.init();
        }
        if (phase == MaxineVM.Phase.COMPILING) {
            if (MaxineVM.isHosted()) {
                // can only refer to JavaPrototype while bootstrapping.
                JavaPrototype.javaPrototype().loadPackage("com.sun.c1x", true);
            }
        }
    }

    public static CiTarget createTarget(VMConfiguration configuration) {
        // create the Target object passed to C1X
        MaxRiRegisterConfig config = new MaxRiRegisterConfig(configuration);
        InstructionSet isa = configuration.platform().processorKind.instructionSet;
        CiArchitecture arch = CiArchitecture.findArchitecture(isa.name().toLowerCase());
        TargetABI targetABI = configuration.targetABIsScheme().optimizedJavaABI();

        CiTarget target = new CiTarget(arch, config, configuration.platform.pageSize, true);
        target.stackAlignment = targetABI.stackFrameAlignment();
        return target;
    }

    public final TargetMethod compile(final ClassMethodActor classMethodActor) {
        return MaxineVM.usingTarget(new Function<TargetMethod>() {
            public TargetMethod call() {
                RiMethod method = c1xRuntime.getRiMethod(classMethodActor);
                CiTargetMethod compiledMethod = compiler.compileMethod(method, xirGenerator).targetMethod();
                if (compiledMethod != null) {
                    C1XTargetMethod c1xTargetMethod = new C1XTargetMethod(C1XCompilerScheme.this, classMethodActor, compiledMethod);
                    CompilationScheme.Static.notifyCompilationComplete(c1xTargetMethod);
                    return c1xTargetMethod;
                }
                throw FatalError.unexpected("bailout"); // compilation failed
            }
        });
    }

    public abstract static class WalkFrameHelper {
        public static WalkFrameHelper instance;

        public abstract boolean walkFrame(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, StackFrameWalker.Purpose purpose, Object context);
    }


    public boolean walkFrame(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, StackFrameWalker.Purpose purpose, Object context) {
        return WalkFrameHelper.instance.walkFrame(current, callee, purpose, context);
    }
}
