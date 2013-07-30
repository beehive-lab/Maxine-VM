/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.c1xgraal;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.VMOptions.*;

import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.graal.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;

/**
 * Integration of the C1X + Graal compiler into Maxine's compilation framework.
 */
public class C1XGraal implements RuntimeCompiler {

    static boolean FailOverToC1X = true;
    static {
        addFieldOption("-XX:", "FailOverToC1X", C1XGraal.class, "Retry failed Graal compilations with C1X.");
        addFieldOption("-XX:", "GraalForNative", C1XGraal.class, "compile native code with Graal");
    }

    /**
     * Temporary option to enable/disable native method compilation by Graal.
     */
    private static boolean GraalForNative = true;

    private MaxGraal graal;
    private C1X c1x;

    @HOSTED_ONLY
    public C1XGraal() {
        this.c1x = new C1X();
        this.graal = MaxGraalFactory.create();
        FailOverToC1X = MaxineVM.isHosted(); // i.e., only when building boot image
    }

    @Override
    public void initialize(Phase phase) {
        c1x.initialize(phase);
        graal.initialize(phase);
        if (FailOverToC1X) {
            // This is required so that assertions in Graal don't immediately stop the VM
            Throw.FatalVMAssertions = false;
        }
    }

    private static final String C1XGraal_C1X = "C1XGraal_C1X";
    private static final String C1XGraal_Graal = "C1XGraal_Graal";

    @Override
    public String name(ClassMethodActor classMethodActor) {
        RuntimeCompiler compiler = chooseCompiler(classMethodActor);
        return compiler == c1x ? C1XGraal_C1X : C1XGraal_Graal;
    }

    private RuntimeCompiler chooseCompiler(final ClassMethodActor method) {
        String name = vm().compilationBroker.compilerFor(method);
        if (method.isNative() && !GraalForNative) {
            // TODO fix this limitation
            return c1x;
        }
        if (method.isTemplate()) {
            // TODO fix this limitation
            return c1x;
        }
        if (name != null) {
            if (name.equals("Graal")) {
                return graal;
            } else if (name.equals("C1X")) {
                return c1x;
            } else {
                throw ProgramError.unexpected("C1XGraal unknown compiler: " + name);
            }
        } else {
            // unspecified
            if (MaxineVM.isHosted() && !MaxGraal.GraalForBoot) {
                // not for boot image yet
                return c1x;
            } else {
                return graal;
            }
        }
    }

    public final TargetMethod compile(final ClassMethodActor method, boolean isDeopt, boolean install, CiStatistics stats) {
        TargetMethod result = null;
        RuntimeCompiler compiler = chooseCompiler(method);
        if (compiler == c1x) {
            result = c1x.compile(method, false, install, stats);
        } else {
            if (FailOverToC1X) {
                try {
                    result = graal.compile(method, false, install, stats);
                } catch (Throwable t) {
                    String errorMessage = "Compilation of " + method + " by Graal failed";
                    boolean lockDisabledSafepoints = Log.lock();
                    Log.printCurrentThread(false);
                    Log.print(": ");
                    Log.println(errorMessage);
                    t.printStackTrace(Log.out);
                    Log.printCurrentThread(false);
                    Log.println(": Retrying with C1X...");
                    Log.unlock(lockDisabledSafepoints);
                    result = c1x.compile(method, false, install, stats);
                }
            } else {
                result = graal.compile(method, false, install, stats);
            }
        }
        return result;
    }

    public Nature nature() {
        return Nature.OPT;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public boolean matches(String compilerName) {
        return compilerName.equals("C1X") || compilerName.equals("Graal");
    }

}
