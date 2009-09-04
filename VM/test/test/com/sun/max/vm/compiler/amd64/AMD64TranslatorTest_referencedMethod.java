/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package test.com.sun.max.vm.compiler.amd64;

import java.util.*;

import junit.framework.*;
import test.com.sun.max.vm.compiler.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.prototype.*;
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
        return MaxineVM.usingTarget(new Function<Class>() {
            public Class call() {
                assert !MaxineVM.isPrototypeOnly(classToInitialize);
                final Class targetClass = Classes.load(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER, classToInitialize.getName());
                final ClassActor classActor = ClassActor.fromJava(targetClass);
                MakeClassInitialized.makeClassInitialized(classActor);
                return targetClass;
            }
        });
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
        final AppendableSequence<MethodActor> directCalls = new LinkSequence<MethodActor>();
        final AppendableSequence<MethodActor> virtualCalls = new LinkSequence<MethodActor>();
        final AppendableSequence<MethodActor> interfaceCalls = new LinkSequence<MethodActor>();
        targetMethod.compilerScheme.gatherCalls(targetMethod, directCalls, virtualCalls, interfaceCalls);
        for (MethodActor methodActor : virtualCalls) {
            listMethodActor(methodActor, methodActors);
        }

        System.out.println("Statically link calls: ");
        for (MethodActor methodActor : methodActorsReferencedByStaticCalls(targetMethod)) {
            listMethodActor(methodActor, methodActors);
        }
    }

    private Iterable<MethodActor> methodActorsReferencedByStaticCalls(TargetMethod targetMethod) {
        final CirGenerator cirGenerator = ((CirGeneratorScheme) CompilerTestSetup.compilerScheme()).cirGenerator();
        final AppendableSequence<MethodActor> result = new LinkSequence<MethodActor>();
        final CirMethod cirMethod = cirGenerator.getCirMethod(targetMethod.classMethodActor());
        final CirVisitor collector = new CirVisitor() {
            @Override
            public void visitCall(CirCall call) {
                final BytecodeLocation location = call.javaFrameDescriptor();
                if (location != null) {
                    final BytecodeVisitor bytecodeVisitor = new BytecodeAdapter() {
                        private void addStaticCall(int index) {
                            final ConstantPool pool = location.classMethodActor().codeAttribute().constantPool();
                            final MethodActor methodActor = pool.classMethodAt(index).resolve(pool, index);
                            result.append(methodActor);
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
                        final byte[] bytecode = location.classMethodActor().codeAttribute().code();
                        if (bytecode != null && location.bytecodePosition() < bytecode.length) {
                            bytecodeScanner.scanInstruction(bytecode, location.bytecodePosition());
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
        MaxineVM.usingTarget(new Runnable() {
            public void run() {
                Trace.on(1);
                final TargetMethod targetMethod = compileMethod(HashMap.class, "values", SignatureDescriptor.create(Collection.class));
                Trace.off();
                listMethodActorsDirectlyReferencedBy(targetMethod);
            }
        });
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
        MaxineVM.usingTarget(new Runnable() {
            public void run() {
                Trace.on(1);
                final TargetMethod targetMethod = compileMethod(r.getClass(), "run", SignatureDescriptor.create(void.class));
                Trace.off();
                listMethodActorsDirectlyReferencedBy(targetMethod);
            }
        });
    }
    public static Test suite() {
        return new AMD64TranslatorTestSetup(new TestSuite(AMD64TranslatorTest_referencedMethod.class)); // This performs the test
    }

    public AMD64TranslatorTest_referencedMethod(String name) {
        super(name);
    }
}
