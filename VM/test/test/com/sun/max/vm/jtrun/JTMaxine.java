/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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

import static com.sun.max.vm.VMConfiguration.*;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.program.option.*;
import com.sun.max.test.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.run.java.jtrun.*;

/**
 * The {@code JTMaxine} class implements a main program for running the generated Java tester tests
 * specifically within the Maxine VM.
 *
 * @author Ben L. Titzer
 */
public class JTMaxine {

    private static final OptionSet options = new OptionSet(true);

    private static final Option<String> packageOption = options.newStringOption("package", "all",
        "Selects the package which contains the generated tester runs to run.");
    private static final Option<String> callerOption = options.newStringOption("caller", "",
        "Selects the compiler that will compile the caller of the test methods. When left blank," +
        "the default VM compiler will be used. \"cps\" selects the VM's CPS compiler, \"jit\" " +
        "selects the VM's JIT compiler, and otherwise a class name can be specified.");
    private static final Option<String> calleeOption = options.newStringOption("callee", "",
        "Selects the compiler that will compile the callee test methods.");
    private static final Option<Boolean> nativeTestsOption = options.newBooleanOption("native-tests", false,
        "Causes the testing framework to load the 'javatest' native library, which is needed by " +
        "JNI tests.");
    private static final Option<Boolean> gcOption = options.newBooleanOption("gc", true,
        "Perform a GC after compilation, before running the tests.");
    private static final Option<Integer> startOption = options.newIntegerOption("start", 0,
        "Specifies the number of the first test to run.");
    private static final Option<Integer> endOption = options.newIntegerOption("end", 0,
        "Specifies the number of the last test to run (not inclusive).");
    private static final Option<Integer> verboseOption = options.newIntegerOption("verbose", 2,
        "Sets the verbosity level.");
    private static final Option<Boolean> helpOption = options.newBooleanOption("help", false,
        "Show help message and exit.");

    private static final String configFieldName = "testClasses";

    public static void main(String[] args) {
        options.parseArguments(args);

        if (helpOption.getValue()) {
            options.printHelp(System.out, 80);
            return;
        }

        String configClassName = "test.com.sun.max.vm.jtrun." + packageOption.getValue() + ".JTConfig";
        try {
            Class configClass = Class.forName(configClassName);
            Field configField = configClass.getField(configFieldName);
            JTClasses jtclasses = (JTClasses) configField.get(null);

            if (nativeTestsOption.getValue()) {
                System.loadLibrary("javatest");
            }
            compileMethods(jtclasses);
            if (!run(jtclasses)) {
                System.out.println("failed.");
                System.exit(-20);
            } else {
                JTUtil.printReport();
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Could not find class: " + configClassName);
        } catch (NoSuchFieldException e) {
            System.out.println("Could not find field: " + configClassName + "." + configFieldName);
        } catch (IllegalAccessException e) {
            System.out.println("Could access field: " + configClassName + "." + configFieldName);
        } catch (ClassCastException e) {
            System.out.println("Field is of wrong type: " + configClassName + "." + configFieldName);
        }
    }

    private static void compileMethods(JTClasses jtclasses) {
        RuntimeCompilerScheme callerCompiler = getCompiler(callerOption.getValue());
        RuntimeCompilerScheme calleeCompiler = getCompiler(calleeOption.getValue());

        if (callerCompiler != null) {
            List<ClassMethodActor> methods = new ArrayList<ClassMethodActor>(100);
            addMethods(methods, jtclasses.testRunClass);
            ProgressPrinter printer = new ProgressPrinter(System.out, methods.size(), 1, false);
            if (verboseOption.getValue() == 3) {
                printer.setVerbose(2);
            }
            System.out.println("Compiling caller methods with " + callerCompiler.getClass() + "...");
            for (ClassMethodActor method : methods) {
                printer.begin(method.toString());
                vmConfig().compilationScheme().synchronousCompile(method, callerCompiler);
                printer.pass();
            }
        }
        if (calleeCompiler != null) {
            List<ClassMethodActor> methods = new ArrayList<ClassMethodActor>(100);
            for (Class c : jtclasses.testClasses) {
                addMethods(methods, c);
            }
            ProgressPrinter printer = new ProgressPrinter(System.out, methods.size(), 1, false);
            if (verboseOption.getValue() == 3) {
                printer.setVerbose(2);
            }
            System.out.println("Compiling callee methods with " + calleeCompiler.getClass() + "...");
            for (ClassMethodActor method : methods) {
                printer.begin(method.toString());
                vmConfig().compilationScheme().synchronousCompile(method, calleeCompiler);
                printer.pass();
            }
        }

        if (gcOption.getValue()) {
            System.out.print("Performing GC...");
            System.out.flush();
            System.gc();
            System.out.println("");
        }
    }

    private static void addMethods(List<ClassMethodActor> list, Class javaClass) {
        ClassActor classActor = ClassActor.fromJava(javaClass);
        // add all static methods
        for (MethodActor methodActor : classActor.localStaticMethodActors()) {
            if (methodActor instanceof ClassMethodActor && !methodActor.isClassInitializer()) {
                list.add((ClassMethodActor) methodActor);
            }
        }
        // add all instance methods
        for (MethodActor methodActor : classActor.localVirtualMethodActors()) {
            if (methodActor instanceof ClassMethodActor) {
                list.add((ClassMethodActor) methodActor);
            }
        }
    }

    static RuntimeCompilerScheme getCompiler(String name) {
        VMConfiguration vmConfiguration = vmConfig();
        if ("".equals(name)) {
            return null;
        } else if ("cps".equals(name)) {
            return CPSCompiler.Static.compiler();
        } else if ("jit".equals(name)) {
            return vmConfiguration.jitCompilerScheme();
        }
        try {
            Class<?> compilerSchemeClass = Class.forName(name);
            Constructor constructor = compilerSchemeClass.getConstructor(VMConfiguration.class);
            RuntimeCompilerScheme compiler = (RuntimeCompilerScheme) constructor.newInstance(vmConfiguration);
            compiler.initialize(MaxineVM.Phase.BOOTSTRAPPING);
            return compiler;
        } catch (ClassNotFoundException e) {
            System.out.println("Could not find compiler scheme class: " + name);
            System.exit(-1);
        } catch (NoSuchMethodException e) {
            System.out.println("Could not find constructor in: " + name);
            System.exit(-1);
        } catch (IllegalAccessException e) {
            System.out.println("Could not access constructor in: " + name);
            System.exit(-1);
        }  catch (InvocationTargetException e) {
            System.out.println("Constructor threw exception: " + e);
            System.exit(-1);
        } catch (InstantiationException e) {
            System.out.println("Could not instantiate: " + name);
            System.exit(-1);
        }
        return null;
    }

    static boolean run(JTClasses jtclasses) {
        try {
            int start = startOption.getValue();
            int end = endOption.getValue();
            if (end == 0) {
                end = jtclasses.getTestCount();
            }
            JTUtil.verbose = verboseOption.getValue();
            Method runMethod = jtclasses.testRunClass.getMethod("runTests", int.class, int.class);
            System.out.println("Running tests " + start + " to " + end + "...");
            return invokeRunMethod(runMethod, start, end);
        } catch (NoSuchMethodException e) {
            System.out.println("Could not find runTests() method in: " + jtclasses.testRunClass);
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            System.out.println("Could not access runTests() method in: " + jtclasses.testRunClass);
            e.printStackTrace();
            return false;
        } catch (InvocationTargetException e) {
            System.out.println("Could not invoke runTests() method in: " + jtclasses.testRunClass);
            e.printStackTrace();
            return false;
        }
    }

    private static boolean invokeRunMethod(Method runMethod, int start, int end) throws IllegalAccessException, InvocationTargetException {
        return (Boolean) runMethod.invoke(null, start, end);
    }
}
