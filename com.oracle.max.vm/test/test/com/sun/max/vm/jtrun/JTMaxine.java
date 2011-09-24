/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.jtrun;

import static com.sun.max.vm.VMConfiguration.*;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.program.option.*;
import com.sun.max.test.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

/**
 * The {@code JTMaxine} class implements a main program for running the generated Java tester tests
 * specifically within the Maxine VM.
 */
public class JTMaxine {

    private static final OptionSet options = new OptionSet(true);

    private static final Option<String> packageOption = options.newStringOption("package", "all",
        "Selects the package which contains the generated tester runs to run.");
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
        List<ClassMethodActor> methods = new ArrayList<ClassMethodActor>(100);
        addMethods(methods, jtclasses.testRunClass);
        ProgressPrinter printer = new ProgressPrinter(System.out, methods.size(), 1, false);
        if (verboseOption.getValue() == 3) {
            printer.setVerbose(2);
        }
        for (ClassMethodActor method : methods) {
            printer.begin(method.toString());
            vmConfig().compilationScheme().synchronousCompile(method, null);
            printer.pass();
        }
        methods.clear();
        for (Class c : jtclasses.testClasses) {
            addMethods(methods, c);
        }
        printer = new ProgressPrinter(System.out, methods.size(), 1, false);
        if (verboseOption.getValue() == 3) {
            printer.setVerbose(2);
        }
        System.out.println("Compiling callee methods...");
        for (ClassMethodActor method : methods) {
            printer.begin(method.toString());
            vmConfig().compilationScheme().synchronousCompile(method, null);
            printer.pass();
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
