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
package com.oracle.max.vm.ext.c1xgraal;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.VMOptions.*;

import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.graal.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
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
    static boolean DisableGraal;
    static {
        addFieldOption("-XX:", "FailOverToC1X", "Retry failed Graal compilations with C1X.");
        addFieldOption("-XX:", "DisableGraal", "Disable the Graal compiler (only C1X will be used).");
    }

    private Graal graal;
    private C1X c1x;

    @HOSTED_ONLY
    public C1XGraal() {
        this.c1x = new C1X();
        this.graal = new Graal();
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

    public final TargetMethod compile(final ClassMethodActor method, boolean install, CiStatistics stats) {
        String name = vm().compilationBroker.compilerFor(method);
        if (forceC1X(method) || (name != null && name.equals("C1X"))) {
            return c1x.compile(method, install, stats);
        } else {
            if (FailOverToC1X) {
                try {
                    return graal.compile(method, install, stats);
                } catch (Throwable t) {
                    String errorMessage = "Compilation of " + method + " by Graal failed";
                    if (VMOptions.verboseOption.verboseCompilation) {
                        boolean lockDisabledSafepoints = Log.lock();
                        Log.printCurrentThread(false);
                        Log.print(": ");
                        Log.println(errorMessage);
                        t.printStackTrace(Log.out);
                        Log.unlock(lockDisabledSafepoints);
                    }
                    if (VMOptions.verboseOption.verboseCompilation) {
                        boolean lockDisabledSafepoints = Log.lock();
                        Log.printCurrentThread(false);
                        Log.println(": Retrying with C1X...");
                        Log.unlock(lockDisabledSafepoints);
                    }
                    return c1x.compile(method, install, stats);
                }
            } else {
                return graal.compile(method, install, stats);
            }
        }
    }

    /**
     * Until Graal can compile everything, this is a mechanism to specify
     * what it cannot yet handle.
     */
    boolean forceC1X(final ClassMethodActor method) {
        if (isHosted()) {
            // !method.isNative(); // disabled until TODO in WordTypeRewriterPhase.changeToWord() is done
            return true;
        }
        return DisableGraal || method.isNative();
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
