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
package test.com.sun.max.vm.compiler;

import java.lang.reflect.*;

import test.com.sun.max.vm.jit.*;

import junit.framework.*;

import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.ir.observer.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.type.*;

/**
 * A utility for compiling one or more methods specified on a command line. This utility accepts all the standard
 * options defined by the {@link PrototypeGenerator} and so the type of compilation performed is fully configurable as
 * is the VM configuration context of compilation.
 *
 * The compilation process is traced to the console through use of the {@link IrObserver} framework. In particular, this
 * utility sets the value of the {@link IrObserverConfiguration#IR_TRACE_PROPERTY} system property to trace all IRs
 * of the methods explicitly listed on the command line.
 *
 * @author Doug Simon
 */
public class CompilerRunner extends CompilerTestSetup<IrMethod> implements JITTestSetup {

    public CompilerRunner(Test test) {
        super(test);
    }

    private static OptionSet _options = new OptionSet(true);

    private static final Option<Integer> _irTraceLevel = _options.newIntegerOption("ir-trace", 3, "The detail level for IR tracing.");
    private static final Option<Boolean> _cirGui = _options.newBooleanOption("cir-gui", false, "Enable the CIR visualizer.");
    private static final Option<Boolean> _jit = _options.newBooleanOption("jit", false, "Compile with the JIT compiler.");

    @Override
    protected JavaPrototype createJavaPrototype() {
        final PrototypeGenerator prototypeGenerator = new PrototypeGenerator();
        return prototypeGenerator.createJavaPrototype(_options, false);
    }

    public static void main(String[] args) {
        _options = _options.parseArguments(args).getArgumentsAndUnrecognizedOptions();

        System.setProperty(IrObserverConfiguration.IR_TRACE_PROPERTY, _irTraceLevel.getValue() + ":");
        if (_cirGui.getValue()) {
            System.setProperty(CirGenerator.CIR_GUI_PROPERTY, "true");
        }

        final String[] arguments = _options.getArguments();
        final TestSuite suite = new TestSuite();

        for (int i = 0; i != arguments.length; ++i) {
            final String argument = arguments[i];
            final int colonIndex = argument.indexOf(':');
            final String className = colonIndex == -1 ? argument : argument.substring(0, colonIndex);
            try {
                final Class<?> javaClass = Class.forName(className, false, CompilerRunner.class.getClassLoader());
                if (colonIndex == -1) {
                    // Class only: compile all methods in class
                    addTestCase(suite, javaClass, null, null);
                } else {
                    final int parenIndex = argument.indexOf('(', colonIndex + 1);
                    final String methodName = parenIndex == -1 ? argument.substring(colonIndex + 1) : argument.substring(colonIndex + 1, parenIndex);
                    if (parenIndex == -1) {
                        // Method name only: compile all methods matching name
                        for (final Method method : javaClass.getDeclaredMethods()) {
                            if (method.getName().equals(methodName)) {
                                final SignatureDescriptor signature = SignatureDescriptor.fromJava(method);
                                addTestCase(suite, javaClass, methodName, signature);
                            }
                        }

                    } else {
                        final SignatureDescriptor signature = SignatureDescriptor.create(argument.substring(parenIndex));
                        addTestCase(suite, javaClass, methodName, signature);
                    }
                }
            } catch (ClassNotFoundException classNotFoundException) {
                ProgramWarning.message(classNotFoundException.toString());
            }
        }

        if (suite.countTestCases() == 0) {
            return;
        }
        junit.textui.TestRunner.run(new CompilerRunner(suite));
    }

    private static String createTestName(Class javaClass, String methodName, SignatureDescriptor signature) {
        if (methodName != null) {
            return javaClass.getName() + "." + methodName;
        }
        return javaClass.getName();
    }

    private static void addTestCase(final TestSuite suite, final Class javaClass, final String methodName, final SignatureDescriptor signature) {
        final String name = createTestName(javaClass, methodName, signature);
        final String value = System.getProperty(IrObserverConfiguration.IR_TRACE_PROPERTY);
        System.setProperty(IrObserverConfiguration.IR_TRACE_PROPERTY, value + "," + name);
        suite.addTest(new Test() {
            public int countTestCases() {
                return 1;
            }
            public void run(TestResult result) {
                final CompilerTestCase compilerTestCase = _jit.getValue() ? new JitCompilerTestCase(name) {} : new CompilerTestCase(name) {};
                if (signature != null) {
                    compilerTestCase.compileMethod(javaClass, methodName, signature);
                } else if (methodName != null) {
                    compilerTestCase.compileMethod(javaClass, methodName);
                } else {
                    compilerTestCase.compileClass(javaClass);
                }
            }
        });
    }

    @Override
    protected VMConfiguration createVMConfiguration() {
        return null;
    }

    @Override
    public IrMethod translate(ClassMethodActor classMethodActor) {
        return javaPrototype().vmConfiguration().compilerScheme().compile(classMethodActor, CompilationDirective.DEFAULT);
    }

    @Override
    public JitCompiler newJitCompiler(TemplateTable templateTable) {
        final JitCompiler jitScheme = (JitCompiler) VMConfiguration.target().jitScheme();
        jitScheme.initializeForJitCompilations();
        return jitScheme;
    }
}
