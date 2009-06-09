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
package test.com.sun.max.vm.compiler.c1x;

import java.util.*;

import com.sun.c1x.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.test.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;
import com.sun.max.lang.Strings;

/**
 * A simple harness to run the C1X compiler and test it in various modes, without
 * needing JUnit.
 *
 * @author Ben L. Titzer
 */
public class C1XTest {

    private static final OptionSet _options = new OptionSet(true);

    private static final Option<Integer> _trace = _options.newIntegerOption("trace", 0,
        "Sets the tracing level of the Maxine VM and runtime.");
    private static final Option<Integer> _verbose = _options.newIntegerOption("verbose", 1,
        "Sets the verbosity level of the testing framework.");
    private static final Option<Boolean> _clinit = _options.newBooleanOption("clinit", true,
        "Compile class initializer (<clinit>) methods");
    private static final Option<Boolean> _failFast = _options.newBooleanOption("fail-fast", true,
        "Stop compilation upon the first bailout");
    private static final Option<Boolean> _timing = _options.newBooleanOption("timing", false,
        "Report compilation time for each successful compile.");

    private static final List<Timing> _timings = new ArrayList<Timing>();

    public static void main(String[] args) {
        _options.parseArguments(args);
        final String[] arguments = _options.getArguments();
        Trace.on(_trace.getValue());

        // create the prototype
        new PrototypeGenerator(_options).createJavaPrototype(false);

        // create MaxineRuntime
        final MaxCiRuntime runtime = new MaxCiRuntime();
        final List<MethodActor> methods = findMethodsToCompile(arguments);
        final ProgressPrinter progress = new ProgressPrinter(System.out, methods.size(), _verbose.getValue(), false);

        for (MethodActor actor : methods) {
            progress.begin(actor.toString());
            if (compile(runtime, actor)) {
                progress.pass();
            } else {
                progress.fail("failed");
                if (_failFast.getValue()) {
                    System.out.println("");
                    break;
                }
            }
        }

        progress.report();
        reportTiming();
    }

    private static boolean compile(MaxCiRuntime runtime, MethodActor method) {
        if (method instanceof ClassMethodActor && !method.isAbstract() && !method.isNative()) {
            final long startNs = System.nanoTime();
            final C1XCompilation compilation = new C1XCompilation(runtime, runtime.getCiMethod(method));
            if (compilation.startBlock() == null) {
                compilation.bailout().printStackTrace();
                return false;
            }
            // record the time for successful compilations
            recordTime(method, System.nanoTime() - startNs);
        }
        return true;
    }

    private static List<MethodActor> findMethodsToCompile(String[] arguments) {
        final Classpath classpath = Classpath.fromSystem();
        final List<MethodActor> methods = new ArrayList<MethodActor>();

        for (int i = 0; i != arguments.length; ++i) {
            final String argument = arguments[i];
            final int colonIndex = argument.indexOf(':');
            final String classNamePattern = colonIndex == -1 ? argument : argument.substring(0, colonIndex);

            // search for matching classes on the class path
            final AppendableSequence<String> matchingClasses = new ArrayListSequence<String>();
            new ClassSearch() {
                @Override
                protected boolean visitClass(String className) {
                    if (!className.endsWith("package-info")) {
                        if (className.contains(classNamePattern)) {
                            matchingClasses.append(className);
                        }
                    }
                    return true;
                }
            }.run(classpath);

            // for all found classes, search for matching methods
            for (String className : matchingClasses) {
                try {
                    final ClassActor classActor = ClassActor.fromJava(Class.forName(className, false, C1XTest.class.getClassLoader()));
                    if (classActor == null) {
                        continue;
                    }

                    if (colonIndex == -1) {
                        // Class only: compile all methods in class
                        for (MethodActor actor : classActor.localStaticMethodActors()) {
                            if (_clinit.getValue() || actor != classActor.classInitializer()) {
                                methods.add(actor);
                            }
                        }
                        for (MethodActor actor : classActor.localVirtualMethodActors()) {
                            methods.add(actor);
                        }
                    } else {
                        // a method pattern was specified, find matching methods
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
                        addMatchingMethods(methods, classActor, methodNamePattern, signature, classActor.localStaticMethodActors());
                        addMatchingMethods(methods, classActor, methodNamePattern, signature, classActor.localVirtualMethodActors());
                    }
                } catch (ClassNotFoundException classNotFoundException) {
                    ProgramWarning.message(classNotFoundException.toString());
                }
            }
        }
        return methods;
    }

    private static void addMatchingMethods(final List<MethodActor> methods, final ClassActor classActor, final String methodNamePattern, final SignatureDescriptor signature, MethodActor[] methodActors) {
        for (final MethodActor method : methodActors) {
            if (method.name().toString().contains(methodNamePattern)) {
                final SignatureDescriptor methodSignature = method.descriptor();
                if (signature == null || signature.equals(methodSignature)) {
                    methods.add(method);
                }
            }
        }
    }

    private static void recordTime(MethodActor method, long ns) {
        if (_timing.getValue()) {
            _timings.add(new Timing((ClassMethodActor) method, ns));
        }
    }

    private static void reportTiming() {
        if (_timing.getValue()) {
            double totalBcps = 0d;
            int count = 0;
            for (Timing timing : _timings) {
                final MethodActor method = timing._classMethodActor;
                final long ns = timing._nanoSeconds;
                final double bcps = timing._bytecodesPerSecond;
                System.out.print(Strings.padLengthWithSpaces("#" + timing._number, 6));
                System.out.print(Strings.padLengthWithSpaces(method.toString(), 80) + ": ");
                System.out.print(Strings.padLengthWithSpaces(13, ns + " ns "));
                System.out.print(Strings.padLengthWithSpaces(18, Strings.fixedDouble(bcps, 2) + " bc/s"));
                System.out.println();
                totalBcps += bcps;
                count++;
            }
            System.out.print("Average: ");
            System.out.print(Strings.fixedDouble(totalBcps / count, 2) + " bc/s");
            System.out.println();
        }
    }

    private static class Timing {
        private final int _number;
        private final ClassMethodActor _classMethodActor;
        private final long _nanoSeconds;
        private final double _bytecodesPerSecond;

        Timing(ClassMethodActor classMethodActor, long ns) {
            _number = _timings.size();
            _classMethodActor = classMethodActor;
            _nanoSeconds = ns;
            _bytecodesPerSecond = 1000000000 * (classMethodActor.rawCodeAttribute().code().length / (double) ns);
        }
    }
}
