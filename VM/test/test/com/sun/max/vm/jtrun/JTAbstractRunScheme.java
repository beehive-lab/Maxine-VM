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
package test.com.sun.max.vm.jtrun;

import static com.sun.max.vm.VMOptions.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.run.java.*;
import test.com.sun.max.vm.jtrun.all.JTRuns;

/**
 * This abstract run scheme is shared by all the concrete run schemes generated by the {@link JTGenerator}.
 * It behaves as the standard {@link JavaRunScheme} if a main class is specified on the command.
 * If no main class is specified, then the tests will be run and the VM will exit.
 *
 * @author Doug Simon
 */
public abstract class JTAbstractRunScheme extends JavaRunScheme {

    @HOSTED_ONLY
    public JTAbstractRunScheme() {
    }

    protected static Utf8Constant testMethod = SymbolTable.makeSymbol("test");
    protected static boolean nativeTests;
    protected static boolean noTests;
    protected static int testStart;
    protected static int testEnd;
    protected static int testCount;

    private static VMIntOption startOption = register(new VMIntOption("-XX:TesterStart=", -1,
                    "The number of the first test to run."), MaxineVM.Phase.STARTING);
    private static VMIntOption endOption  = register(new VMIntOption("-XX:TesterEnd=", -1,
                    "The number of the last test to run. Specify 0 to run exactly one test."), MaxineVM.Phase.STARTING);
    private static final boolean COMPILE_ALL_TEST_METHODS = true;

    @HOSTED_ONLY
    public void addClassToImage(Class<?> javaClass) {
        final ClassActor actor = ClassActor.fromJava(javaClass);
        if (actor == null) {
            return;
        }
        if (BootImageGenerator.calleeJit) {
            CompiledPrototype.registerJitClass(javaClass);
        }
        if (BootImageGenerator.calleeC1X) {
            CompiledPrototype.registerC1XClass(javaClass);
        }
        if (COMPILE_ALL_TEST_METHODS) {
            // add all virtual and static methods to the image
            addMethods(actor.localStaticMethodActors());
            addMethods(actor.localVirtualMethodActors());
        } else {
            // add only the test method to the image
            final StaticMethodActor method = actor.findLocalStaticMethodActor(testMethod);
            if (method != null) {
                addMethodToImage(method);
            }
        }
        for (Class<?> declaredClass : javaClass.getDeclaredClasses()) {
            // load all inner and anonymous classes into the image as well
            addClassToImage(declaredClass);
        }
    }

    @HOSTED_ONLY
    private void addMethods(ClassMethodActor[] methodActors) {
        if (methodActors != null) {
            for (ClassMethodActor method : methodActors) {
                addMethodToImage(method);
            }
        }
    }

    @HOSTED_ONLY
    private void addMethodToImage(ClassMethodActor method) {
        CompiledPrototype.registerImageMethod(method);
    }

    private boolean classesRegistered;

    @HOSTED_ONLY
    private void registerClasses() {
        if (!classesRegistered) {
            classesRegistered = true;
            if (BootImageGenerator.callerJit) {
                CompiledPrototype.registerJitClass(JTRuns.class);
            }
            Class[] list = getClassList();
            for (Class<?> testClass : list) {
                addClassToImage(testClass);
            }
            testCount = list.length;
        }
    }

    @Override
    protected boolean parseMain() {
        return noTests;
    }

    protected abstract void runTests();

    @Override
    public void initialize(MaxineVM.Phase phase) {
        noTests = VMOptions.parseMain(false);
        if (phase == MaxineVM.Phase.STARTING) {
            if (nativeTests || noTests) {
                super.initialize(phase);
            }
            if (!noTests) {
                testStart = startOption.getValue();
                if (testStart < 0) {
                    testStart = 0;
                }
                testEnd = endOption.getValue();
                if (testEnd < testStart || testEnd > testCount) {
                    testEnd = testCount;
                } else if (testEnd == testStart) {
                    testEnd = testStart + 1;
                }
                if (nativeTests) {
                    System.loadLibrary("javatest");
                }
                runTests();
            }
        } else {
            super.initialize(phase);
        }
        JTUtil.verbose = 3;
        if (MaxineVM.isHosted()) {
            registerClasses();
            nativeTests = BootImageGenerator.nativeTests;
            super.initialize(phase);
        }
    }

    @HOSTED_ONLY
    public abstract Class[] getClassList();
}
