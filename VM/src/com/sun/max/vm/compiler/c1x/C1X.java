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
import java.util.*;

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
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * Integration of the C1X compiler into Maxine's compilation framework.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public class C1X implements RuntimeCompiler {

    /**
     * The Maxine specific implementation of the {@linkplain RiRuntime runtime interface} needed by C1X.
     */
    public final MaxRiRuntime runtime = vm().runtime;

    /**
     * The {@linkplain CiTarget target} environment derived from a Maxine {@linkplain Platform platform} description.
     */
    public final CiTarget target;

    /**
     * The Maxine specific implementation of the {@linkplain RiXirGenerator interface} used by C1X
     * to incorporate runtime specific details when translating bytecode methods.
     */
    public final RiXirGenerator xirGenerator;

    /**
     * The C1X compiler instance configured for the Maxine runtime.
     */
    private C1XCompiler compiler;

    /**
     * Set to true once the C1X options are set (to allow subclasses of this scheme to coexist in the same image).
     */
    @HOSTED_ONLY
    private static boolean optionsRegistered;

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

    /**
     * A map from option field names to some text describing the meaning and
     * usage of the corresponding C1X option.
     */
    private static Map<String, String> helpMap;

    public static Map<String, String> getHelpMap() {
        if (helpMap == null) {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("PrintFilter",
                    "Filter compiler tracing to methods whose fully qualified name " +
                    "matches <arg>. If <arg> starts with \"~\", then <arg> (without " +
                    "the \"~\") is interpreted as a regular expression. Otherwise, " +
                    "<arg> is interpreted as a simple substring.");

            map.put("TraceBytecodeParserLevel",
                    "Trace frontend bytecode parser at level <n> where 0 means no " +
                    "tracing, 1 means instruction tracing and 2 means instruction " +
                    "plus frame state tracing.");

            map.put("DetailedAsserts",
                    "Turn on detailed error checking that has a noticeable performance impact.");

            map.put("GenSpecialDivChecks",
                    "Generate code to check for (Integer.MIN_VALUE / -1) or (Long.MIN_VALUE / -1) " +
                    "instead of detecting these cases via instruction decoding in a trap handler.");

            map.put("UseStackMapTableLiveness",
                    "Use liveness information derived from StackMapTable class file attribute.");

            for (String name : map.keySet()) {
                try {
                    C1XOptions.class.getField(name);
                } catch (Exception e) {
                    throw new InternalError("The name '" + name + "' does not denote a field in " + C1XOptions.class);
                }
            }
            helpMap = Collections.unmodifiableMap(map);
        }
        return helpMap;
    }

    @HOSTED_ONLY
    public static C1X instance;

    @HOSTED_ONLY
    public C1X() {
        this(new MaxXirGenerator(), platform().target);
    }

    @HOSTED_ONLY
    protected C1X(RiXirGenerator xirGenerator, CiTarget target) {
        if (!optionsRegistered) {
            VMOptions.addFieldOptions("-C1X:", C1XOptions.class, getHelpMap());
            optionsRegistered = true;
        }
        this.xirGenerator = xirGenerator;
        this.target = target;
        if (instance == null) {
            instance = this;
        }
    }

    @Override
    public <T extends TargetMethod> Class<T> compiledType() {
        Class<Class<T>> type = null;
        return Utils.cast(type, C1XTargetMethod.class);
    }

    @Override
    public void initialize(Phase phase) {
        if (isHosted() && phase == Phase.COMPILING) {
            C1XOptions.UseConstDirectCall = true; // Default
            C1XOptions.StackShadowPages = VmThread.STACK_SHADOW_PAGES;
            compiler = new C1XCompiler(runtime, target, xirGenerator, vm().registerConfigs.globalStub);
            // search for the runtime call and register critical methods
            for (Method m : RuntimeCalls.class.getDeclaredMethods()) {
                int flags = m.getModifiers();
                if (Modifier.isStatic(flags) && Modifier.isPublic(flags)) {
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
            initialize(Phase.COMPILING);
        }
        return compiler;
    }

    public final TargetMethod compile(final ClassMethodActor classMethodActor, boolean install, CiStatistics stats) {
        RiMethod method = classMethodActor;
        CiTargetMethod compiledMethod = compiler().compileMethod(method, -1, xirGenerator, stats).targetMethod();
        if (compiledMethod != null) {
            C1XTargetMethod c1xTargetMethod = new C1XTargetMethod(classMethodActor, compiledMethod, install);
            return c1xTargetMethod;
        }
        throw FatalError.unexpected("bailout"); // compilation failed
    }

    public void resetMetrics() {
        C1XTimers.reset();
        for (Field f : C1XMetrics.class.getFields()) {
            if (f.getType() == int.class) {
                try {
                    f.set(null, 0);
                } catch (IllegalAccessException e) {
                    // do nothing.
                }
            }
        }
    }

    public CallEntryPoint calleeEntryPoint() {
        return CallEntryPoint.OPTIMIZED_ENTRY_POINT;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
