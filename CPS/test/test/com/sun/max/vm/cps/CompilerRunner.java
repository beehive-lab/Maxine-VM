/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.cps;

import static com.sun.max.vm.VMConfiguration.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import junit.framework.*;
import test.com.sun.max.vm.jit.*;

import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.adaptive.*;
import com.sun.max.vm.cps.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.observer.*;
import com.sun.max.vm.cps.jit.*;
import com.sun.max.vm.cps.template.*;
import com.sun.max.vm.hosted.*;
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

    private static final OptionSet options = new OptionSet() {
        @Override
        protected void printHelpHeader(PrintStream stream) {
            stream.println("Usage: " + CompilerRunner.class.getSimpleName() + " [-options] <compilation specs>");
            stream.println("    A compilation spec is a class name pattern followed by an optional method name");
            stream.println("    pattern separated by a ':'. For example:");
            stream.println();
            stream.println("        Object:wait String");
            stream.println();
            stream.println("    will compile all methods in a class whose name contains \"Object\" where the");
            stream.println("    method name contains \"wait\" as well as all methods in a class whose name");
            stream.println("    contains \"String\". The classes searched are those on the class path.");
            stream.println();
            stream.println("where options include:");
        }
    };

    private static final Option<Integer> irTraceLevel = options.newIntegerOption("ir-trace", 3, "The detail level for IR tracing.");
    private static final Option<Boolean> cirGui = options.newBooleanOption("cir-gui", false, "Enable the CIR visualizer.");
    private static final Option<Boolean> useBaseline = options.newBooleanOption("use-baseline", false, "Compile with the baseline compiler.");
    private static final Option<Boolean> help = options.newBooleanOption("help", false, "Show help message and exits.");

    private static final PrototypeGenerator prototypeGenerator = new PrototypeGenerator(options);

    public static void main(String[] args) {
        VMConfigurator vmConfigurator = new VMConfigurator(options);
        Trace.addTo(options);
        options.parseArguments(VMOption.extractVMArgs(args));

        if (help.getValue()) {
            options.printHelp(System.out, 80);
            return;
        }

        vmConfigurator.create(true);

        System.setProperty(IrObserverConfiguration.IR_TRACE_PROPERTY, irTraceLevel.getValue() + ":");
        if (cirGui.getValue()) {
            System.setProperty(CirGenerator.CIR_GUI_PROPERTY, "true");
        }

        final String[] arguments = options.getArguments();
        final TestSuite suite = new TestSuite();
        final Classpath classpath = Classpath.fromSystem();

        for (int i = 0; i != arguments.length; ++i) {
            final int startTestCases = suite.countTestCases();
            final String argument = arguments[i];
            final int colonIndex = argument.indexOf(':');
            final String classNamePattern = colonIndex == -1 ? argument : argument.substring(0, colonIndex);

            final List<Class<?>> matchingClasses = new ArrayList<Class<?>>();
            new ClassSearch() {
                @Override
                protected boolean visitClass(String className) {
                    if (!className.endsWith("package-info")) {
                        if (className.contains(classNamePattern)) {
                            try {
                                matchingClasses.add(Class.forName(className, false, CompilerRunner.class.getClassLoader()));
                            } catch (ClassNotFoundException classNotFoundException) {
                                ProgramWarning.message(classNotFoundException.toString());
                            }
                        }
                    }
                    return true;
                }
            }.run(classpath);

            for (Class<?> javaClass : matchingClasses) {
                if (colonIndex == -1) {
                    // Class only: compile all methods in class
                    addTestCase(suite, javaClass, null, null);
                } else {
                    final int parenIndex = argument.indexOf('(', colonIndex + 1);
                    final String methodNamePattern;
                    final SignatureDescriptor signature;
                    if (parenIndex == -1) {
                        methodNamePattern = argument.substring(colonIndex + 1);
                        signature = null;
                    } else {
                        methodNamePattern = argument.substring(colonIndex + 1, parenIndex);
                        signature = SignatureDescriptor.create(argument.substring(parenIndex));
                    }
                    for (final Method method : javaClass.getDeclaredMethods()) {
                        if (method.getName().contains(methodNamePattern)) {
                            final SignatureDescriptor methodSignature = SignatureDescriptor.fromJava(method);
                            if (signature == null || signature.equals(methodSignature)) {
                                addTestCase(suite, javaClass, method.getName(), methodSignature);
                            }
                        }
                    }
                }
            }

            if (startTestCases == suite.countTestCases()) {
                ProgramWarning.message("No compilations added by argument '" + argument + "'");
            }
        }

        if (suite.countTestCases() == 0) {
            return;
        }
        Trace.stream().println("Initializing compiler...");
        junit.textui.TestRunner.run(new CompilerRunner(suite));

        VMOptions.beforeExit();
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
                final CompilerTestCase compilerTestCase = useBaseline.getValue() ? new JitCompilerTestCase(name) {} : new CompilerTestCase(name) {};
                if (signature != null) {
                    Trace.stream().println("Compiling " + javaClass.getName() + "." + methodName + signature);
                    compilerTestCase.compileMethod(javaClass, methodName, signature);
                } else if (methodName != null) {
                    Trace.stream().println("Compiling " + javaClass.getName() + "." + methodName);
                    compilerTestCase.compileMethod(javaClass, methodName);
                } else {
                    Trace.stream().println("Compiling all methods in " + javaClass.getName());
                    compilerTestCase.compileClass(javaClass);
                }
            }
        });
    }

    @Override
    public IrMethod translate(ClassMethodActor classMethodActor) {
        CPSAbstractCompiler cpsCompilerScheme = (CPSAbstractCompiler) CPSCompiler.Static.compiler();
        return cpsCompilerScheme.compileIR(classMethodActor);
    }

    public JitCompiler newJitCompiler(TemplateTable templateTable) {
        AdaptiveCompilationScheme adc = (AdaptiveCompilationScheme) vmConfig().compilationScheme();
        final JitCompiler jitScheme = (JitCompiler) adc.baselineCompiler;
        return jitScheme;
    }

    public boolean disassembleCompiledMethods() {
        return true;
    }
}
