/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
    public final RiXirGenerator xirGenerator;

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
        this(new MaxXirGenerator());
    }

    @HOSTED_ONLY
    protected C1XCompilerScheme(MaxXirGenerator xirGenerator) {
        VMOptions.addFieldOptions("-C1X:", C1XOptions.class, C1XOptions.getHelpMap());
        this.xirGenerator = xirGenerator;
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
        if (phase == Phase.TERMINATING) {
            if (C1XOptions.PrintMetrics) {
                C1XMetrics.print();
            }
            if (C1XOptions.PrintTimers) {
                C1XTimers.print();
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
            return c1xTargetMethod;
        }
        throw FatalError.unexpected("bailout"); // compilation failed
    }

    @Override
    public CallEntryPoint calleeEntryPoint() {
        return CallEntryPoint.OPTIMIZED_ENTRY_POINT;
    }
}
