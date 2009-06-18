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

import static test.com.sun.max.vm.compiler.c1x.C1XTest.PatternType.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.util.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.program.option.OptionSet.*;
import com.sun.max.test.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;

/**
 * A simple harness to run the C1X compiler and test it in various modes, without
 * needing JUnit.
 *
 * @author Ben L. Titzer
 */
public class C1XTest {

    private static final OptionSet _options = new OptionSet(true);

    private static final Option<Integer> _trace = _options.newIntegerOption("trace", 0,
        "Set the tracing level of the Maxine VM and runtime.");
    private static final Option<Integer> _verbose = _options.newIntegerOption("verbose", 1,
        "Set the verbosity level of the testing framework.");
    private static final Option<Boolean> _printBailout = _options.newBooleanOption("print-bailout", false,
        "Print bailout exceptions.");
    private static final Option<File> _outFile = _options.newFileOption("o", (File) null,
        "A file to which output should be sent. If not specified, then output is sent to stdout.");
    private static final Option<Boolean> _clinit = _options.newBooleanOption("clinit", true,
        "Compile class initializer (<clinit>) methods");
    private static final Option<Boolean> _failFast = _options.newBooleanOption("fail-fast", true,
        "Stop compilation upon the first bailout.");
    private static final Option<Boolean> _timing = _options.newBooleanOption("timing", false,
        "Report compilation time for each successful compile.");
    private static final Option<Boolean> _average = _options.newBooleanOption("average", true,
        "Report only the average compilation speed.");
    private static final Option<Integer> _warmup = _options.newIntegerOption("warmup", 0,
        "Set the number of warmup runs to execute before initiating the timed run.");
    private static final Option<Boolean> _help = _options.newBooleanOption("help", false,
        "Show help message and exit.");

    static {
        for (final Field field : C1XOptions.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                if (field.getType() == boolean.class) {
                    final String name = field.getName();
                    try {
                        final boolean defaultValue = field.getBoolean(null);
                        _options.addOption(new Option<Boolean>("XX:+" + name, defaultValue, OptionTypes.BOOLEAN_TYPE, "Enable the " + name + " option.") {
                            @Override
                            public void setValue(Boolean value) {
                                try {
                                    field.setBoolean(null, true);
                                } catch (Exception e) {
                                    ProgramError.unexpected("Error updating the value of " + field, e);
                                }
                            }
                        }, Syntax.EQUALS_OR_BLANK);
                        _options.addOption(new Option<Boolean>("XX:-" + name, !defaultValue, OptionTypes.BOOLEAN_TYPE, "Disable the " + name + " option.") {
                            @Override
                            public void setValue(Boolean value) {
                                try {
                                    field.setBoolean(null, false);
                                } catch (Exception e) {
                                    ProgramError.unexpected("Error updating the value of " + field, e);
                                }
                            }
                        }, Syntax.EQUALS_OR_BLANK);
                    } catch (Exception e) {
                        ProgramError.unexpected("Error reading the value of " + field, e);
                    }
                }
            }
        }
    }

    private static final List<Timing> _timings = new ArrayList<Timing>();

    private static PrintStream _out = System.out;

    public static void main(String[] args) {
        _options.parseArguments(args);
        final String[] arguments = _options.getArguments();

        if (_help.getValue()) {
            _options.printHelp(System.out, 80);
            return;
        }

        if (_outFile.getValue() != null) {
            try {
                _out = new PrintStream(new FileOutputStream(_outFile.getValue()));
                Trace.setStream(_out);
            } catch (FileNotFoundException e) {
                System.err.println("Could not open " + _outFile.getValue() + " for writing: " + e);
                System.exit(1);
            }
        }

        Trace.on(_trace.getValue());

        // create the prototype
        if (_verbose.getValue() > 0) {
            _out.print("Creating Java prototype... ");
        }
        new PrototypeGenerator(_options).createJavaPrototype(false);
        if (_verbose.getValue() > 0) {
            _out.println("done");
        }

        // create MaxineRuntime
        final MaxCiRuntime runtime = new MaxCiRuntime();
        final List<MethodActor> methods = findMethodsToCompile(arguments);
        final ProgressPrinter progress = new ProgressPrinter(_out, methods.size(), _verbose.getValue(), false);

        for (int i = 0; i < _warmup.getValue(); i++) {
            if (_verbose.getValue() > 0) {
                if (i == 0) {
                    _out.print("Warming up");
                }
                _out.print(".");
                _out.flush();
            }
            for (MethodActor actor : methods) {
                compile(runtime, actor, false, true);
            }
            if (_verbose.getValue() > 0 && i == _warmup.getValue() - 1) {
                _out.print("\n");
            }
        }

        for (MethodActor methodActor : methods) {
            progress.begin(methodActor.toString());
            final C1XCompilation compilation = compile(runtime, methodActor, _printBailout.getValue(), false);
            if (compilation == null || compilation.startBlock() != null) {
                progress.pass();

                if (compilation != null && C1XOptions.PrintIR) {
                    _out.println(methodActor.format("IR for %H.%n(%p)"));
                    final LogStream out = new LogStream(_out);
                    final InstructionPrinter ip = new InstructionPrinter(out, true);
                    final BlockPrinter bp = new BlockPrinter(ip, false, false);
                    compilation.startBlock().iteratePreOrder(bp);
                }

            } else {
                progress.fail("failed");
                if (_failFast.getValue()) {
                    _out.println("");
                    break;
                }
            }
        }

        progress.report();
        reportTiming();
        reportMetrics();
    }

    private static C1XCompilation compile(MaxCiRuntime runtime, MethodActor method, boolean printBailout, boolean warmup) {
        if (isCompilable(method)) {
            final long startNs = System.nanoTime();
            final C1XCompilation compilation = new C1XCompilation(runtime, runtime.getCiMethod(method));
            if (compilation.startBlock() == null) {
                if (printBailout) {
                    compilation.bailout().printStackTrace(_out);
                }
            }
            if (!warmup) {
                // record the time for successful compilations
                recordTime(method, compilation.totalInstructions(), System.nanoTime() - startNs);
            }
            return compilation;
        }
        return null;
    }

    private static boolean isCompilable(MethodActor method) {
        return method instanceof ClassMethodActor && !method.isAbstract() && !method.isNative() && !method.isBuiltin() && !method.isUnsafeCast();
    }

    enum PatternType {
        EXACT("matching") {
            @Override
            public boolean matches(String input, String pattern) {
                return input.equals(pattern);
            }
        },
        PREFIX("starting with") {
            @Override
            public boolean matches(String input, String pattern) {
                return input.startsWith(pattern);
            }
        },
        SUFFIX("ending with") {
            @Override
            public boolean matches(String input, String pattern) {
                return input.endsWith(pattern);
            }
        },
        CONTAINS("containing") {
            @Override
            public boolean matches(String input, String pattern) {
                return input.contains(pattern);
            }
        };

        final String _relationship;

        private PatternType(String relationship) {
            _relationship = relationship;
        }

        abstract boolean matches(String input, String pattern);

        @Override
        public String toString() {
            return _relationship;
        }
    }

    static class PatternMatcher {
        final String _pattern;
        // 1: exact, 2: prefix, 3: suffix, 4: substring
        final PatternType _type;

        public PatternMatcher(String pattern) {
            if (pattern.startsWith("^") && pattern.endsWith("^")) {
                _type = EXACT;
                _pattern = pattern.substring(1, pattern.length() - 1);
            } else if (pattern.startsWith("^")) {
                _type = PREFIX;
                _pattern = pattern.substring(1);
            } else if (pattern.endsWith("^")) {
                _type = SUFFIX;
                _pattern = pattern.substring(0, pattern.length() - 1);
            } else {
                _type = CONTAINS;
                _pattern = pattern;
            }
        }

        boolean matches(String input) {
            return _type.matches(input, _pattern);
        }
    }

    private static List<MethodActor> findMethodsToCompile(String[] arguments) {
        final Classpath classpath = Classpath.fromSystem();
        final List<MethodActor> methods = new ArrayList<MethodActor>() {
            @Override
            public boolean add(MethodActor e) {
                final boolean result = super.add(e);
                if ((size() % 1000) == 0 && _verbose.getValue() >= 1) {
                    _out.print('.');
                }
                return result;
            }
        };

        for (int i = 0; i != arguments.length; ++i) {
            final String argument = arguments[i];
            final int colonIndex = argument.indexOf(':');
            final PatternMatcher classNamePattern = new PatternMatcher(colonIndex == -1 ? argument : argument.substring(0, colonIndex));

            // search for matching classes on the class path
            final AppendableSequence<String> matchingClasses = new ArrayListSequence<String>();
            if (_verbose.getValue() > 0) {
                _out.print("Classes " + classNamePattern._type + " '" + classNamePattern._pattern + "'... ");
            }

            new ClassSearch() {
                @Override
                protected boolean visitClass(String className) {
                    if (!className.endsWith("package-info")) {
                        if (classNamePattern.matches(className)) {
                            matchingClasses.append(className);
                        }
                    }
                    return true;
                }
            }.run(classpath);

            if (_verbose.getValue() > 0) {
                _out.println(matchingClasses.length());
            }

            final int startMethods = methods.size();
            if (_verbose.getValue() > 0) {
                _out.print("Gathering methods");
            }
            // for all found classes, search for matching methods
            for (String className : matchingClasses) {
                try {
                    final Class<?> javaClass = Class.forName(className, false, C1XTest.class.getClassLoader());
                    final ClassActor classActor = getClassActorNonfatal(javaClass);
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
                        final PatternMatcher methodNamePattern;
                        final SignatureDescriptor signature;
                        if (parenIndex == -1) {
                            methodNamePattern = new PatternMatcher(argument.substring(colonIndex + 1));
                            signature = null;
                        } else {
                            methodNamePattern = new PatternMatcher(argument.substring(colonIndex + 1, parenIndex));
                            signature = SignatureDescriptor.create(argument.substring(parenIndex));
                        }
                        addMatchingMethods(methods, classActor, methodNamePattern, signature, classActor.localStaticMethodActors());
                        addMatchingMethods(methods, classActor, methodNamePattern, signature, classActor.localVirtualMethodActors());
                    }
                } catch (ClassNotFoundException classNotFoundException) {
                    ProgramWarning.message(classNotFoundException.toString());
                }
            }
            if (_verbose.getValue() > 0) {
                _out.println(" " + (methods.size() - startMethods));
            }
        }
        return methods;
    }

    private static ClassActor getClassActorNonfatal(Class<?> javaClass) {
        ClassActor classActor = null;
        try {
            classActor = ClassActor.fromJava(javaClass);
        } catch (Throwable t) {
            // do nothing.
        }
        return classActor;
    }

    private static void addMatchingMethods(final List<MethodActor> methods, final ClassActor classActor, final PatternMatcher methodNamePattern, final SignatureDescriptor signature, MethodActor[] methodActors) {
        for (final MethodActor method : methodActors) {
            if (methodNamePattern.matches(method.name().toString())) {
                final SignatureDescriptor methodSignature = method.descriptor();
                if (signature == null || signature.equals(methodSignature)) {
                    methods.add(method);
                }
            }
        }
    }

    private static void recordTime(MethodActor method, int instructions, long ns) {
        if (_timing.getValue()) {
            _timings.add(new Timing((ClassMethodActor) method, instructions, ns));
        }
    }

    private static void reportTiming() {
        if (_timing.getValue()) {
            double totalBcps = 0d;
            double totalIps = 0d;
            int count = 0;
            for (Timing timing : _timings) {
                final MethodActor method = timing._classMethodActor;
                final long ns = timing._nanoSeconds;
                final double bcps = timing.bytecodesPerSecond();
                final double ips = timing.instructionsPerSecond();
                if (!_average.getValue()) {
                    _out.print(Strings.padLengthWithSpaces("#" + timing._number, 6));
                    _out.print(Strings.padLengthWithSpaces(method.toString(), 80) + ": ");
                    _out.print(Strings.padLengthWithSpaces(13, ns + " ns "));
                    _out.print(Strings.padLengthWithSpaces(18, Strings.fixedDouble(bcps, 2) + " bytes/s"));
                    _out.print(Strings.padLengthWithSpaces(18, Strings.fixedDouble(ips, 2) + " insts/s"));
                    _out.println();
                }
                totalBcps += bcps;
                totalIps += ips;
                count++;
            }
            _out.print("Average: ");
            _out.print(Strings.fixedDouble(totalBcps / count, 2) + " bytes/s   ");
            _out.print(Strings.fixedDouble(totalIps / count, 2) + " insts/s");
            _out.println();
        }
    }

    private static void reportMetrics() {
        if (C1XOptions.PrintMetrics && _verbose.getValue() > 0) {
            for (final Field field : C1XMetrics.class.getFields()) {
                if (field.getType() == int.class) {
                    try {
                        int value = field.getInt(null);
                        String name = field.getName();
                        _out.print(name + ": " + value + "\n");
                    } catch (IllegalAccessException e) {
                        // do nothing.
                    }
                }
            }
        }
    }

    private static class Timing {
        private final int _number;
        private final ClassMethodActor _classMethodActor;
        private final int _instructions;
        private final long _nanoSeconds;

        Timing(ClassMethodActor classMethodActor, int instructions, long ns) {
            _number = _timings.size();
            _classMethodActor = classMethodActor;
            _instructions = instructions;
            _nanoSeconds = ns;
        }

        public double bytecodesPerSecond() {
            return 1000000000 * (_classMethodActor.rawCodeAttribute().code().length / (double) _nanoSeconds);
        }

        public double instructionsPerSecond() {
            return 1000000000 * (_instructions / (double) _nanoSeconds);
        }
    }
}
