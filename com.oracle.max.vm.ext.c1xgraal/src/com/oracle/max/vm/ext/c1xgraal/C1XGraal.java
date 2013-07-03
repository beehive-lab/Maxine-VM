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

import com.oracle.graal.options.*;
import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.graal.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Integration of the C1X + Graal compiler into Maxine's compilation framework.
 */
public class C1XGraal implements RuntimeCompiler {

    static boolean FailOverToC1X = true;
    static {
        addFieldOption("-XX:", "FailOverToC1X", "Retry failed Graal compilations with C1X.");
        addFieldOption("-XX:", "MaxGraalNative", "compile native code with Graal");
    }

    /**
     * Temporary option to enable/disable native method compilation by Graal.
     */
    private static boolean MaxGraalNative = true;

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
        if (MaxineVM.isHosted()) {
            testsHack(phase); // TODO remove
        }
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
        if (method.isNative() && !MaxGraalNative) {
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
            if (MaxineVM.isHosted() && !MaxGraal.MaxGraalForBoot) {
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

    /**
     * Hack option to compile given tests after boot image generation (ease of debugging).
     */
    private static String MaxGraalTests;

    /**
     * Hack to compile some tests during boot image generation - easier debugging.
     *
     */
    @HOSTED_ONLY
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
                String[] classAndMethod = getClassAndMethod(test);
                try {
                    Class< ? > testClass = Class.forName(classAndMethod[0]);
                    ClassActor testClassActor = ClassActor.fromJava(testClass);
                    MethodActor[] methodActors;
                    String methodName = classAndMethod[1];
                    if (methodName.equals("*")) {
                        methodActors = testClassActor.getLocalMethodActorsArray();
                    } else {
                        SignatureDescriptor signature = null;
                        int ix = methodName.lastIndexOf('(');
                        if (ix > 0) {
                            signature = SignatureDescriptor.create(methodName.substring(ix));
                            methodName = methodName.substring(0, ix);
                        }
                        MethodActor methodActor = testClassActor.findLocalClassMethodActor(SymbolTable.makeSymbol(methodName), signature);
                        if (methodActor == null) {
                            throw new NoSuchMethodError(methodName);
                        }
                        methodActors = new MethodActor[1];
                        methodActors[0] = methodActor;
                    }
                    VMOption vmOption = MaxGraalOptions.getVMOption("MethodFilter");
                    if (!vmOption.isPresent()) {
                        @SuppressWarnings({ "cast", "unchecked"})
                        OptionValue<String> filterOption = (OptionValue<String>) MaxGraalOptions.getOptionValue(vmOption);
                        filterOption.setValue(classAndMethod[0] + "." + methodName);
                    }

                    for (MethodActor methodActor : methodActors) {
//                        if (true) {
//                            TargetMethod c1xTm = c1x.compile((ClassMethodActor) methodActor, false, false, null);
//                        }
                        TargetMethod tm = graal.compile((ClassMethodActor) methodActor, false, true, null);
                        validate(tm);
                    }
                } catch (ClassNotFoundException ex) {
                    System.err.println("failed to find test class: " + classAndMethod[0]);
                } catch (NoSuchMethodError | ClassFormatError ex) {
                    System.err.println("failed to find test method: " + test);
                }
            }
            System.exit(0);
        }
    }

    @HOSTED_ONLY
    private void validate(TargetMethod tm) {
        final Set<MethodActor> directCalls = new HashSet<MethodActor>();
        final Set<MethodActor> virtualCalls = new HashSet<MethodActor>();
        final Set<MethodActor> interfaceCalls = new HashSet<MethodActor>();
        final Set<MethodActor> inlinedMethods = new HashSet<MethodActor>();
        // gather all direct, virtual, and interface calls and add them
        tm.gatherCalls(directCalls, virtualCalls, interfaceCalls, inlinedMethods);

        CompiledPrototype.checkInliningCorrect(directCalls, null, false, true);
        CompiledPrototype.checkInliningCorrect(virtualCalls, null, false, true);
        CompiledPrototype.checkInliningCorrect(interfaceCalls, null, false, true);
        CompiledPrototype.checkInliningCorrect(inlinedMethods, tm.classMethodActor(), true, false);
    }

    @HOSTED_ONLY
    private static String[] getClassAndMethod(String classAndMethod) {
        if (classAndMethod.indexOf(':') > 0) {
            return classAndMethod.split(":");
        } else {
            String signature = null;
            int ix = classAndMethod.lastIndexOf('(');
            if (ix > 0) {
                signature = classAndMethod.substring(ix);
                classAndMethod = classAndMethod.substring(0, ix);
            }
            ix = classAndMethod.lastIndexOf('.');
            String[] result = new String[2];
            result[0] = classAndMethod.substring(0, ix);
            result[1] = classAndMethod.substring(ix + 1) + (signature == null ? "" : signature);
            return result;
        }
    }


}
