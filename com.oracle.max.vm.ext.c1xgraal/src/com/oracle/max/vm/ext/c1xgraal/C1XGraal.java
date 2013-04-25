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

import java.util.*;

import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.graal.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;

/**
 * Integration of the C1X + Graal compiler into Maxine's compilation framework.
 */
public class C1XGraal implements RuntimeCompiler {

    static boolean FailOverToC1X;
    static boolean DisableGraal;
    static {
        addFieldOption("-XX:", "FailOverToC1X", "Retry failed Graal compilations with C1X.");
        addFieldOption("-XX:", "DisableGraal", "Disable the Graal compiler (only C1X will be used).");
        addFieldOption("-XX:", "MaxGraalCompare", "compare compiled code against C1X/T1X");
    }

    /**
     * Prevents compilation (and comparison with C1X/T1X) when {@code false}.
     * Important in boot image mode, as multiple compilations of the same method
     * cause an assertion error.
     */
    private static boolean MaxGraalCompare = true;

    private MaxGraal graal;
    private C1X c1x;

    @HOSTED_ONLY
    public C1XGraal() {
        this.c1x = new C1X();
        this.graal = new MaxGraal();
        FailOverToC1X = MaxineVM.isHosted(); // i.e., only when building boot image
    }

    @Override
    public void initialize(Phase phase) {
        c1x.initialize(phase);
        graal.initialize(phase);
        testsHack(phase); // TODO remove
        if (FailOverToC1X) {
            // This is required so that assertions in Graal don't immediately stop the VM
            Throw.FatalVMAssertions = false;
        }
    }

    private static class ChoiceName extends ThreadLocal<ChoiceName> {
        Stack<String> names = new Stack<String>();

        @Override
        protected ChoiceName initialValue() {
            return new ChoiceName();
        }

        void push(String name) {
            names.push(name);
        }

        void pop() {
            names.pop();
        }
    }

    private static final ChoiceName choiceNameTL = new ChoiceName();
    private static final String C1XGraal_C1X = "C1XGraal_C1X";
    private static final String C1XGraal_Graal = "C1XGraal_Graal";

    @Override
    public String name(ClassMethodActor classMethodActor) {
        RuntimeCompiler compiler = chooseCompiler(classMethodActor);
        return compiler == c1x ? C1XGraal_C1X : C1XGraal_Graal;
    }

    private RuntimeCompiler chooseCompiler(final ClassMethodActor method) {
        String name = vm().compilationBroker.compilerFor(method);
        // For now use Graal only for methods specified by -XX:CompileCommand
        if (name == null || name.equals("C1X") || method.isNative()) {
            return c1x;
        } else {
            return graal;
        }
    }

    public final TargetMethod compile(final ClassMethodActor method, boolean isDeopt, boolean install, CiStatistics stats) {
        TargetMethod result = null;
        RuntimeCompiler compiler = chooseCompiler(method);
        if (compiler == c1x) {
            result = c1x.compile(method, false, install, stats);
        } else {
            TargetMethod c1xTM = null;
            TargetMethod t1xTM = null;
            if (!MaxineVM.isHosted() && MaxGraalCompare) {
                // compile with C1X/T1X for comparison
                c1xTM = c1x.compile(method, isDeopt, install, stats);
                t1xTM = CompilationBroker.singleton.baselineCompiler.compile(method, isDeopt, install, stats);
                defaultCompilations(c1xTM, t1xTM);
            }

            if (FailOverToC1X) {
                try {
                    result =  graal.compile(method, false, install, stats);
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
                    result = c1x.compile(method, false, install, stats);
                }
            } else {
                result = graal.compile(method, false, install, stats);
            }
            if (!MaxineVM.isHosted() && MaxGraalCompare) {
                compare(result, c1xTM, t1xTM);
            }
        }
        return result;
    }

    @NEVER_INLINE
    private static void compare(TargetMethod graalTM, TargetMethod c1xTM, TargetMethod t1xTM) {

    }

    @NEVER_INLINE
    private static void defaultCompilations(TargetMethod c1xTM, TargetMethod t1xTM) {

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

    /**
     * Hack option to compile given tests after boot image generation (ease of debugging).
     */
    private static String MaxGraalTests;

    /**
     * Hack to compile some tests during boot image generation - easier debugging.
     *
     */
    private void testsHack(Phase phase) {
        if (phase == MaxineVM.Phase.BOOTSTRAPPING) {
            VMOptions.addFieldOption("-XX:", "MaxGraalTests", "list of test methods to compile");
        }
        if (phase != MaxineVM.Phase.HOSTED_COMPILING) {
            return;
        }
        if (MaxGraalTests != null) {
            String[] tests = MaxGraalTests.split(",");
            for (String test : tests) {
                String[] classAndMethod = test.split(":");
                try {
                    Class< ? > testClass = Class.forName(classAndMethod[0]);
                    ClassActor testClassActor = ClassActor.fromJava(testClass);
                    MethodActor[] methodActors;
                    if (classAndMethod[1].equals("*")) {
                        methodActors = testClassActor.getLocalMethodActorsArray();
                    } else {
                        MethodActor methodActor = testClassActor.findLocalClassMethodActor(SymbolTable.makeSymbol(classAndMethod[1]), null);
                        if (methodActor == null) {
                            throw new NoSuchMethodError(classAndMethod[1]);
                        }
                        methodActors = new MethodActor[1];
                        methodActors[0] = methodActor;
                    }
                    for (MethodActor methodActor : methodActors) {
                        graal.compile((ClassMethodActor) methodActor, false, true, null);
                    }
                } catch (ClassNotFoundException ex) {
                    System.err.println("failed to find test class: " + test);
                } catch (NoSuchMethodError ex) {
                    System.err.println("failed to find test method: " + test);
                }
            }
        }
    }


}
