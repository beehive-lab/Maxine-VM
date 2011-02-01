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
package com.sun.max.vm.cps;

import static com.sun.max.vm.thread.VmThread.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.config.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.interpreter.*;
import com.sun.max.vm.cps.ir.observer.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * @author Bernd Mathiske
 */
public abstract class CPSAbstractCompiler implements CPSCompiler {

    public CPSAbstractCompiler() {
        if (MaxineVM.isHosted()) {
            CPSCompiler.Static.setCompiler(this);
        }
    }

    /**
     * Gets this compiler's last IR generator (typically a {@link com.sun.max.vm.cps.target.TargetGenerator}).
     *
     * @return the last IR generator of this compiler
     */
    public abstract IrGenerator irGenerator();

    @Override
    public CallEntryPoint calleeEntryPoint() {
        return CallEntryPoint.OPTIMIZED_ENTRY_POINT;
    }

    protected abstract List<IrGenerator> irGenerators();

    @Override
    public void initialize(Phase phase) {
        if (phase == Phase.BOOTSTRAPPING || phase == Phase.STARTING) {
            IrObserverConfiguration.attach(irGenerators());
        }

        if (MaxineVM.isHosted() && phase == MaxineVM.Phase.COMPILING) {
            compileSnippets();
        }

        if (phase == Phase.TERMINATING) {
            for (IrGenerator generator : irGenerators()) {
                generator.notifyAfterFinish();
            }
        }
    }

    @HOSTED_ONLY
    public void createBuiltins(PackageLoader packageLoader) {
        packageLoader.loadAndInitializeAll(Builtin.class);
        Builtin.initialize();
    }

    @HOSTED_ONLY
    public void createSnippets(PackageLoader packageLoader) {
        packageLoader.loadAndInitializeAll(Snippet.class);
        packageLoader.loadAndInitializeAll(HotpathSnippet.class);
    }

    @HOSTED_ONLY
    private boolean areSnippetsCompiled = false;

    @HOSTED_ONLY
    public boolean areSnippetsCompiled() {
        return areSnippetsCompiled;
    }

    @HOSTED_ONLY
    public void compileSnippets() {
        areSnippetsCompiled = true;
    }

    @Override
    public final <Type extends TargetMethod> Class<Type> compiledType() {
        Class irMethodType = irGenerator().irMethodType;
        if (TargetMethod.class.isAssignableFrom(irMethodType)) {
            Class<Class<Type>> type = null;
            return Utils.cast(type, irMethodType);
        }
        return null;
    }

    public final TargetMethod compile(ClassMethodActor classMethodActor) {
        IrMethod method = compileIR(classMethodActor);
        if (method instanceof TargetMethod) {
            return (TargetMethod) method;
        }
        return null;
    }

    public final IrMethod compileIR(ClassMethodActor classMethodActor) {
        return irGenerator().makeIrMethod(classMethodActor);
    }

    @HOSTED_ONLY
    public void initializeForJitCompilations() {
    }

    public boolean isBuiltinImplemented(Builtin builtin) {
        return true;
    }

    /**
     * Thread local for passing the exception when interpreting with an {@link IrInterpreter}.
     */
    @HOSTED_ONLY
    public static final ThreadLocal<Throwable> INTERPRETER_EXCEPTION = new ThreadLocal<Throwable>() {
        @Override
        public void set(Throwable value) {
            Throwable g = get();
            assert value == null || g == null;
            super.set(value);
        }
    };

    /**
     * Executes a safepoint and then gets the Throwable object from the
     * {@link VmThreadLocal#EXCEPTION_OBJECT} thread local.
     *
     * This method is only annotated to be never inlined so that it does something different
     * if being executed by an {@link IrInterpreter}.
     */
    @NEVER_INLINE
    public static Throwable safepointAndLoadExceptionObject() {
        if (MaxineVM.isHosted()) {
            return hostedSafepointAndLoadExceptionObject();
        }
        Safepoint.safepoint();
        Throwable exception = UnsafeCast.asThrowable(EXCEPTION_OBJECT.loadRef(currentTLA()).toJava());
        EXCEPTION_OBJECT.store3(Reference.zero());
        FatalError.check(exception != null, "Exception object lost during unwinding");
        return exception;
    }

    @HOSTED_ONLY
    public static Throwable hostedSafepointAndLoadExceptionObject() {
        Throwable throwable = INTERPRETER_EXCEPTION.get();
        INTERPRETER_EXCEPTION.set(null);
        return throwable;
    }
}
