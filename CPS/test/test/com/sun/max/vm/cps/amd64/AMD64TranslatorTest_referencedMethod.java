/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.cps.amd64;

import java.util.*;

import junit.framework.*;
import test.com.sun.max.vm.cps.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.snippet.Snippet.MakeClassInitialized;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

/**
 * Testing transitive closure of method calls.
 *
 * @author Laurent Daynes
 */
@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public class AMD64TranslatorTest_referencedMethod extends CompilerTestCase<CPSTargetMethod> {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AMD64TranslatorTest_referencedMethod.suite());
    }

    protected Class initializeClassInTarget(final Class classToInitialize) {
        assert !MaxineVM.isHostedOnly(classToInitialize);
        final Class targetClass = Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, classToInitialize.getName());
        final ClassActor classActor = ClassActor.fromJava(targetClass);
        MakeClassInitialized.makeClassInitialized(classActor);
        return targetClass;
    }

    private void listMethodActor(MethodActor methodActor,  IdentityHashSet<MethodActor> methodActors) {
        if (methodActors.contains(methodActor)) {
            System.out.println("\t" + methodActor + " (ALREADY SEEN)");
        } else {
            System.out.println("\t" + methodActor);
            methodActors.add(methodActor);
        }
    }

    private void listMethodActorsDirectlyReferencedBy(TargetMethod targetMethod) {
        final IdentityHashSet<MethodActor> methodActors = new IdentityHashSet<MethodActor>();

        System.out.println("Direct Callees: ");
        if (targetMethod.directCallees() != null) {
            for (Object directCallee : targetMethod.directCallees()) {
                if (directCallee instanceof MethodActor) {
                    listMethodActor((MethodActor) directCallee, methodActors);
                }
            }
        }

        System.out.println("Reference Literals: ");
        if (targetMethod.referenceLiterals() != null) {
            for (Object literal : targetMethod.referenceLiterals()) {
                if (literal instanceof MethodActor) {
                    listMethodActor((MethodActor) literal, methodActors);
                }
            }
        }

        System.out.println("Virtual and Interface calls: ");
        final Set<MethodActor> directCalls = new HashSet<MethodActor>();
        final Set<MethodActor> virtualAndInterfaceCalls = new HashSet<MethodActor>();
        final Set<MethodActor> inlinedCalls = new HashSet<MethodActor>();
        targetMethod.gatherCalls(directCalls, virtualAndInterfaceCalls, virtualAndInterfaceCalls, inlinedCalls);
        for (MethodActor methodActor : virtualAndInterfaceCalls) {
            listMethodActor(methodActor, methodActors);
        }

        System.out.println("Inlined calls: ");
        for (MethodActor methodActor : inlinedCalls) {
            listMethodActor(methodActor, methodActors);
        }

        System.out.println("Statically link calls: ");
        for (MethodActor methodActor : methodActorsReferencedByStaticCalls(targetMethod)) {
            listMethodActor(methodActor, methodActors);
        }
    }

    private Iterable<MethodActor> methodActorsReferencedByStaticCalls(TargetMethod targetMethod) {
        final CirGenerator cirGenerator = ((CirGeneratorScheme) CompilerTestSetup.compilerScheme()).cirGenerator();
        final List<MethodActor> result = new LinkedList<MethodActor>();
        final CirMethod cirMethod = cirGenerator.getCirMethod(targetMethod.classMethodActor());
        final CirVisitor collector = new CirVisitor() {
            @Override
            public void visitCall(CirCall call) {
                final BytecodeLocation location = call.javaFrameDescriptor();
                if (location != null) {
                    final BytecodeVisitor bytecodeVisitor = new BytecodeAdapter() {
                        private void addStaticCall(int index) {
                            final ConstantPool pool = location.classMethodActor.codeAttribute().cp;
                            final MethodActor methodActor = pool.classMethodAt(index).resolve(pool, index);
                            result.add(methodActor);
                        }
                        @Override
                        protected void invokestatic(int index) {
                            addStaticCall(index);
                        }

                        @Override
                        protected void invokespecial(int index) {
                            addStaticCall(index);
                        }
                    };

                    final BytecodeScanner bytecodeScanner = new BytecodeScanner(bytecodeVisitor);
                    try {
                        final byte[] bytecode = location.classMethodActor.codeAttribute().code();
                        if (bytecode != null && location.bci < bytecode.length) {
                            bytecodeScanner.scanInstruction(bytecode, location.bci);
                        }
                    } catch (Throwable throwable) {
                        ProgramError.unexpected("could not scan byte code", throwable);
                    }
                }
            }
        };
        CirVisitingTraversal.apply(cirMethod.closure(), collector);
        return result;
    }

    public void test_checkReferencedMethods1() {
        initializeClassInTarget(HashMap.class);
        Trace.on(1);
        final TargetMethod targetMethod = compileMethod(HashMap.class, "values", SignatureDescriptor.create(Collection.class));
        Trace.off();
        listMethodActorsDirectlyReferencedBy(targetMethod);
    }

    static int c;
    public static void aStaticCall() {
        c++;
    }

    public void test_checkReferencedMethods2() {
        final Runnable r = new Runnable() {
            public void run() {
                aStaticCall();
            }
        };
        initializeClassInTarget(MaxineVM.class);
        Trace.on(1);
        final TargetMethod targetMethod = compileMethod(r.getClass(), "run", SignatureDescriptor.create(void.class));
        Trace.off();
        listMethodActorsDirectlyReferencedBy(targetMethod);
    }
    public static Test suite() {
        return new AMD64TranslatorTestSetup(new TestSuite(AMD64TranslatorTest_referencedMethod.class)); // This performs the test
    }

    public AMD64TranslatorTest_referencedMethod(String name) {
        super(name);
    }
}
