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
import static com.sun.max.vm.MaxineVM.*;

import java.lang.reflect.*;

import com.sun.c1x.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;
import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.c1x.MaxXirGenerator.RuntimeCalls;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Integration of the C1X compiler into Maxine's compilation framework.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public class C1XCompilerScheme extends AbstractVMScheme implements RuntimeCompilerScheme {

    /**
     * The Maxine specific implementation of the {@linkplain RiRuntime runtime interface} needed by C1X.
     */
    public final MaxRiRuntime runtime = vm().runtime;

    /**
     * The {@linkplain CiTarget target} environment derived from a Maxine {@linkplain Platform platform} description.
     */
    public final CiTarget target = platform().target;

    /**
     * The Maxine specific implementation of the {@linkplain RiXirGenerator interface} used by C1X
     * to incorporate runtime specific details when translating bytecode methods.
     */
    public final RiXirGenerator xirGenerator = new MaxXirGenerator();

    /**
     * The C1X compiler instance configured for the Maxine runtime.
     */
    private C1XCompiler compiler;

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
        VMOptions.addFieldOptions("-C1X:", C1XOptions.class, C1XOptions.helpMap);
    }

    @Override
    public <T extends TargetMethod> Class<T> compiledType() {
        Class<Class<T>> type = null;
        return Utils.cast(type, C1XTargetMethod.class);
    }

    @Override
    public void initialize(Phase phase) {
        if (isHosted() && phase == Phase.BOOTSTRAPPING) {
            C1XOptions.UseConstDirectCall = true; // Default
            compiler = new C1XCompiler(runtime, target, xirGenerator, vm().registerConfigs.globalStub);
            // search for the runtime call and register critical methods
            for (Method m : RuntimeCalls.class.getDeclaredMethods()) {
                int flags = m.getModifiers();
                if (Modifier.isStatic(flags) && Modifier.isPublic(flags)) {
                    // Log.out.println("Registered critical method: " + m.getName() + " / " + SignatureDescriptor.create(m.getReturnType(), m.getParameterTypes()).toString());
                    new CriticalMethod(RuntimeCalls.class, m.getName(), SignatureDescriptor.create(m.getReturnType(), m.getParameterTypes()));
                }
            }
        }
    }

    public C1XCompiler compiler() {
        if (isHosted() && compiler == null) {
            initialize(Phase.BOOTSTRAPPING);
        }
        return compiler;
    }

    public final TargetMethod compile(final ClassMethodActor classMethodActor) {
        RiMethod method = classMethodActor;
        CiTargetMethod compiledMethod = compiler().compileMethod(method, -1, xirGenerator).targetMethod();
        if (compiledMethod != null) {
            C1XTargetMethod c1xTargetMethod = new C1XTargetMethod(classMethodActor, compiledMethod);
            CompilationScheme.Inspect.notifyCompilationComplete(c1xTargetMethod);
            return c1xTargetMethod;
        }
        throw FatalError.unexpected("bailout"); // compilation failed
    }

    @Override
    public CallEntryPoint calleeEntryPoint() {
        return CallEntryPoint.OPTIMIZED_ENTRY_POINT;
    }
}
