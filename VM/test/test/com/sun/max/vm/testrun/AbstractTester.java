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
package test.com.sun.max.vm.testrun;

import test.com.sun.max.vm.testrun.all.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.run.java.*;

public abstract class AbstractTester extends JavaRunScheme {

    protected static Utf8Constant _testMethod = SymbolTable.makeSymbol("test");
    protected static boolean _nativeTests;
    protected static boolean _noTests;
    protected static int _passed;
    protected static int _finished;
    protected static int _total;
    protected static int _testNum;
    protected static int _testStart;
    protected static int _testEnd;
    protected static int _verbose = 2;

    private static VMIntOption _verboseLevel = new VMIntOption("-XX:TesterVerbose", 2,
                    "The Java tester verbosity level.", MaxineVM.Phase.STARTING);
    private static VMIntOption _startOption  = new VMIntOption("-XX:TesterStart=", -1,
                    "The number of the first test to run.", MaxineVM.Phase.STARTING);
    private static VMIntOption _endOption  = new VMIntOption("-XX:TesterEnd=", -1,
                    "The number of the last test to run.", MaxineVM.Phase.STARTING);
    private static VMOption _offOption  = new VMOption("-XX:TesterOff",
                    "Omit tests and run a standard Java program.", MaxineVM.Phase.STARTING);
    private static final boolean COMPILE_ALL_TEST_METHODS = true;

    public static void reportPassed(int passed, int total) {
        Debug.println();
        Debug.print(passed);
        Debug.print(" of ");
        Debug.print(total);
        Debug.println(" passed.");
    }

    public static void end(String run, boolean result) {
        if (result) {
            _passed++;
        }
        if (_verbose == 2) {
            verbose(result, ++_finished, _total);
        }
        if (_verbose == 3) {
            if (!result) {
                printRun(run);
                Debug.println(" failed with incorrect result");
            }
        }
        _testNum++;
    }

    public static void end(String run, Throwable t) {
        if (_verbose == 2) {
            verbose(false, ++_finished, _total);
        }
        if (_verbose == 3) {
            printRun(run);
            Debug.print(" failed with exception !");
            Debug.println(t.getClass().getName());
        }
        _testNum++;
    }

    private static void printRun(String run) {
        Debug.print("\t");
        printTestNum();
        if (run != null) {
            Debug.print(run);
        }
    }

    public static void verbose(boolean passed, int finished, int total) {
        Debug.print(passed ? '.' : 'X');
        if (finished % 10 == 0) {
            Debug.print(' ');
        }
        if (finished % 50 == 0) {
            Debug.print(' ');
            Debug.print(finished);
            Debug.print(" of ");
            Debug.println(total);
        } else if (finished == total) {
            Debug.println();
        }
    }

    public static void begin(String test) {
        if (_verbose == 3) {
            printTestNum();
            Debug.print(test);
            int i = test.length();
            while (i++ < 50) {
                Debug.print(' ');
            }
            Debug.print("  next " + _testStartOption + "=");
            Debug.print(_testNum + 1);
            Debug.println("");
        }
    }

    public static void printTestNum() {
        // print out the test number (aligned to the left)
        Debug.print(_testNum);
        Debug.print(':');
        if (_testNum < 100) {
            Debug.print(' ');
        }
        if (_testNum < 10) {
            Debug.print(' ');
        }
        Debug.print(' ');
    }

    public AbstractTester(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @PROTOTYPE_ONLY
    public void addClassToImage(Class<?> javaClass) {
        final ClassActor actor = ClassActor.fromJava(javaClass);
        if (actor == null) {
            return;
        }
        if (BinaryImageGenerator._calleeJit) {
            CompiledPrototype.registerJitClass(javaClass);
        }
        if (BinaryImageGenerator._unlinked) {
            CompiledPrototype.registerClassUnlinked(actor);
        }
        if (COMPILE_ALL_TEST_METHODS) {
            // add all virtual and static methods to the image
            addMethods(actor.localStaticMethodActors());
            addMethods(actor.localVirtualMethodActors());
        } else {
            // add only the test method to the image
            final StaticMethodActor method = actor.findLocalStaticMethodActor(_testMethod);
            if (method != null) {
                addMethodToImage(method);
            }
        }
        for (Class<?> declaredClass : javaClass.getDeclaredClasses()) {
            // load all inner and anonymous classes into the image as well
            addClassToImage(declaredClass);
        }
    }

    @PROTOTYPE_ONLY
    private void addMethods(ClassMethodActor[] methodActors) {
        if (methodActors != null) {
            for (ClassMethodActor method : methodActors) {
                addMethodToImage(method);
            }
        }
    }

    @PROTOTYPE_ONLY
    private void addMethodToImage(ClassMethodActor method) {
        CompiledPrototype.registerImageMethod(method);
        if (BinaryImageGenerator._unlinked) {
            CompiledPrototype.registerMethodUnlinked(method);
        }
    }

    @PROTOTYPE_ONLY
    private void registerClasses() {
        if (BinaryImageGenerator._callerJit) {
            CompiledPrototype.registerJitClass(JavaTesterTests.class);
        }
        for (Class<?> testClass : getClassList()) {
            addClassToImage(testClass);
        }
    }

    @Override
    protected boolean parseMain() {
        if (_noTests) {
            return super.parseMain();
        }
        return VMOptions.parseMain(false);
    }

    protected abstract void runTests();

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.STARTING) {
            _noTests = _offOption.isPresent();
            if (_nativeTests || _noTests) {
                super.initialize(phase);
            }
            if (!_noTests) {
                _testStart = _startOption.getValue();
                if (_testStart < 0) {
                    _testStart = 0;
                }
                final int testEnd = _endOption.getValue();
                if (testEnd == 0) {
                    _testEnd = _testStart + 1;
                }
                if (testEnd > 0) {
                    _testEnd = testEnd;
                }
                if (_nativeTests) {
                    System.loadLibrary("javatest");
                }
                runTests();
            }
        } else {
            super.initialize(phase);
        }
        _verbose = 3;
        if (MaxineVM.isPrototyping()) {
            registerClasses();
            _nativeTests = BinaryImageGenerator._nativeTests;
            super.initialize(phase);
        }
    }

    @PROTOTYPE_ONLY
    public abstract Class[] getClassList();
}
