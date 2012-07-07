/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jni;

import static com.sun.max.vm.VMOptions.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.sun.max.annotate.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.jni.JniFunctionsGenerator.Customizer;
import com.sun.max.vm.jni.JniFunctionsGenerator.JniCustomizer;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.ti.*;

/**
 * This class encapsulates the Java side of the native interfaces such as JNI, JVMTI and JMM supported by the VM.
 */
public final class NativeInterfaces {

    /**
     * A few native functions must not have any prologue and epilogue.  We hand-list them here.
     */
    public static boolean needsPrologueAndEpilogue(MethodActor ma) {
        return VMTI.handler().nativeCallNeedsPrologueAndEpilogue(ma) && ma != Snippets.blockOnThreadLockMethod();
    }

    private NativeInterfaces() {
    }

    /**
     * Gets the method actors corresponding to the methods implementing a native interface to the VM.
     * These are the static methods in a given class annotated by {@link VM_ENTRY_POINT}.
     *
     * @param javaClass a class that implements a native interface to the VM
     * @return the static methods in {@code javaClass} that are annotated by {@link VM_ENTRY_POINT}
     */
    @HOSTED_ONLY
    public static StaticMethodActor[] getNativeInterfaceFunctionActors(Class javaClass) {
        StaticMethodActor[] localStaticMethodActors = ClassActor.fromJava(javaClass).localStaticMethodActors();
        int count = 0;
        for (StaticMethodActor m : localStaticMethodActors) {
            if (m.isVmEntryPoint()) {
                count++;
            }
        }
        final StaticMethodActor[] nativeFunctionActors = new StaticMethodActor[count];
        int i = 0;
        for (StaticMethodActor m : localStaticMethodActors) {
            if (m.isVmEntryPoint()) {
                nativeFunctionActors[i++] = m;
            }
        }
        assert i == count;
        return nativeFunctionActors;
    }

    /**
     * Checks that a given list of methods matches the table of JNI functions declared in jni.h.
     *
     * @param jniFunctionActors the list of methods implementing the JNI function interface
     * @return the value of {@code jniFunctionActors} unmodified
     */
    @HOSTED_ONLY
    private static StaticMethodActor[] checkAgainstJniHeaderFile(StaticMethodActor[] jniFunctionActors) {
        String jniHeaderFilePath = System.getProperty("max.jni.headerFile");
        if (jniHeaderFilePath == null) {
            jniHeaderFilePath = Platform.jniHeaderFilePath();
        }

        File jniHeaderFile = new File(jniHeaderFilePath);
        ProgramError.check(jniHeaderFile.isFile(), "JNI header file " + jniHeaderFile + " does not exist or is not a file");

        List<String> jniFunctionNames = new ArrayList<String>();

        // Prepend the reserved function slots
        jniFunctionNames.add("reserved0");
        jniFunctionNames.add("reserved1");
        jniFunctionNames.add("reserved2");
        jniFunctionNames.add("reserved3");

        parseInterfaceFunctions(jniHeaderFile, jniFunctionNames);

        for (int i = 0; i != jniFunctionActors.length; ++i) {
            final String jniFunctionName = jniFunctionNames.get(i);
            final String jniFunctionActorName = jniFunctionActors[i].name.toString();
            ProgramError.check(jniFunctionName.equals(jniFunctionActorName), "JNI function " + jniFunctionName + " at index " + i + " does not match JNI function actor " + jniFunctionActorName);
        }

        ProgramError.check(jniFunctionNames.size() == jniFunctionActors.length);
        return jniFunctionActors;
    }

    /**
     * Checks that a given list of methods matches the table of JMM functions declared in jmm.h.
     *
     * @param jmmFunctionActors the list of methods implementing the JMM function interface
     * @return the value of {@code jmmFunctionActors} unmodified
     */
    @HOSTED_ONLY
    private static StaticMethodActor[] checkAgainstJmmHeaderFile(StaticMethodActor[] jmmFunctionActors) {
        final File jmmHeaderFile = new File(new File(JavaProject.findWorkspace(), "com.oracle.max.vm.native/substrate/jmm.h").getAbsolutePath());
        ProgramError.check(jmmHeaderFile.exists(), "JMM header file " + jmmHeaderFile + " does not exist");

        List<String> jmmFunctionNames = new ArrayList<String>();

        // Prepend some reserved function slots
        jmmFunctionNames.add("reserved1");
        jmmFunctionNames.add("reserved2");

        parseInterfaceFunctions(jmmHeaderFile, jmmFunctionNames);

        // Insert some other reserved function slots
        jmmFunctionNames.add(jmmFunctionNames.indexOf("GetMemoryUsage"), "reserved4");
        jmmFunctionNames.add(jmmFunctionNames.indexOf("DumpHeap0"), "reserved5");
        jmmFunctionNames.add(jmmFunctionNames.indexOf("DumpThreads"), "reserved6");

        for (int i = 0; i != jmmFunctionActors.length; ++i) {
            final String jmmFunctionName = jmmFunctionNames.get(i);
            final String jmmFunctionActorName = jmmFunctionActors[i].name.toString();
            ProgramError.check(jmmFunctionName.equals(jmmFunctionActorName), "JMM function " + jmmFunctionName + " at index " + i + " does not match JNI function actor " + jmmFunctionActorName);
        }

        ProgramError.check(jmmFunctionNames.size() == jmmFunctionActors.length);
        return jmmFunctionActors;
    }

    /**
     * Parses a given file for declarations of functions in a VM native interface.
     * The declaration of such functions match this pattern:
     *
     *     (JNICALL *<function name>)
     *
     * @param nativeHeaderFile the C header file to parse
     * @param functionNames the list to which the matched function names are added
     */
    @HOSTED_ONLY
    private static void parseInterfaceFunctions(File nativeHeaderFile, List<String> functionNames) {
        final Pattern pattern = Pattern.compile("\\(JNICALL \\*([^\\)]+)\\)\\s*\\(JNIEnv\\s*\\*");

        try {
            String content = new String(Files.toChars(nativeHeaderFile));
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                final String functionName = matcher.group(1);
                functionNames.add(functionName);
            }
        } catch (IOException ioException) {
            throw ProgramError.unexpected("Error reading native header file " + nativeHeaderFile.getPath(), ioException);
        }
    }

    @HOSTED_ONLY
    public static CriticalMethod[] toCriticalMethods(StaticMethodActor[] methodActors) {
        CriticalMethod[] result = new CriticalMethod[methodActors.length];
        for (int i = 0; i < result.length; ++i) {
            StaticMethodActor staticMethodActor = methodActors[i];
            if (!staticMethodActor.isNative()) {
                result[i] = new CriticalMethod(staticMethodActor, CallEntryPoint.C_ENTRY_POINT);
            }
        }
        return result;
    }

    private static final StaticMethodActor[] vmFunctionActors = getNativeInterfaceFunctionActors(VMFunctions.class);
    private static final StaticMethodActor[] jniFunctionActors = checkAgainstJniHeaderFile(getNativeInterfaceFunctionActors(JniFunctions.class));
    private static final StaticMethodActor[] jmmFunctionActors = checkAgainstJmmHeaderFile(getNativeInterfaceFunctionActors(JmmFunctions.class));

    private static final CriticalMethod[] vmFunctions = toCriticalMethods(vmFunctionActors);
    private static final CriticalMethod[] jniFunctions = toCriticalMethods(jniFunctionActors);
    private static final CriticalMethod[] jmmFunctions = toCriticalMethods(jmmFunctionActors);

    public static CriticalMethod[] jniFunctions() {
        return jniFunctions;
    }

    private static Pointer jniEnv = Pointer.zero();

    /**
     * Get the address of the table of JNI functions.
     */
    public static Pointer jniEnv() {
        if (jniEnv.isZero()) {
            FatalError.unexpected("JNI env pointer is zero");
        }
        return jniEnv;
    }

    /**
     * Completes the JNI function table for the JNI functions that are implemented in Java.
     * @param vmInterface pointer to the VM function table
     * @param jniEnv pointer to the JNI function table
     * @param jmmInterface pointer to the JMM function table
     */
    public static void initialize(Pointer vmInterface, Pointer jniEnv, Pointer jmmInterface) {
        NativeInterfaces.jniEnv = jniEnv;
        initFunctionTable(vmInterface, vmFunctions, vmFunctionActors);
        initFunctionTable(jniEnv, jniFunctions, jniFunctionActors);
        initFunctionTable(jmmInterface, jmmFunctions, jmmFunctionActors);
    }

    public static void initFunctionTable(Pointer functionTable, CriticalMethod[] functions, StaticMethodActor[] functionActors) {
        for (int i = 0; i < functions.length; i++) {
            CriticalMethod function = functions[i];
            if (function != null) {
                final Word functionPointer = function.address();
                if (!functionTable.getWord(i).isZero()) {
                    Log.print("Overwriting value ");
                    Log.print(functionTable.getWord(i));
                    Log.print(" in JNI/JMM/JVMTI function table at index ");
                    Log.print(i);
                    Log.print(" with function ");
                    Log.printMethod(function.classMethodActor, true);
                    FatalError.crash("Multiple implementations for a JNI/JMM/JVMTI function");
                }
                functionTable.setWord(i, functionPointer);
            } else {
                if (functionTable.getWord(i).isZero()) {
                    Log.print("Entry in JNI/JMM/JVMTI function table at index ");
                    Log.print(i);
                    Log.println(" for ");
                    Log.printMethod(functionActors[i], false);
                    Log.println(" has no implementation");
                    FatalError.crash("Missing implementation for JNI/JMM/JVMTI function");
                }
            }
        }
    }

    /**
     * Gets the target method of the JNI function that contains a given instruction address.
     *
     * @param instructionPointer
     * @return {@code null} if {@code code instructionPointer} is not within any JNI function
     */
    public static TargetMethod jniTargetMethod(Address instructionPointer) {
        for (int i = 0; i < jniFunctions.length; i++) {
            CriticalMethod function = jniFunctions[i];
            if (function != null) {
                final TargetMethod targetMethod = function.targetMethod();
                if (targetMethod != null && targetMethod.contains(instructionPointer)) {
                    return targetMethod;
                }
            }
        }
        return null;
    }

    /**
     * Determines if information should be displayed about use of native methods and other Java Native Interface activity.
     */
    public static boolean verbose() {
        return verboseOption.verboseJNI;
    }

    static {
        JavaPrototype.registerGeneratedCodeCheckerCallback(new GeneratedCodeCheckerCallback());
    }

    @HOSTED_ONLY
    private static class GeneratedCodeCheckerCallback implements JavaPrototype.GeneratedCodeCheckerCallback {

        @Override
        public void checkGeneratedCode() {
            checkGenerateSourcesInSync(VMFunctionsSource.class, VMFunctions.class, new JniFunctionsGenerator.VMCustomizer(true));
            checkGenerateSourcesInSync(JniFunctionsSource.class, JniFunctions.class, null);
            checkGenerateSourcesInSync(JmmFunctionsSource.class, JmmFunctions.class, null);
        }
    }

    @HOSTED_ONLY
    public static void checkGenerateSourcesInSync(Class source, Class target, Customizer customizer) {
        checkGenerateSourcesInSync("com.oracle.max.vm", source, target, customizer);
    }

    @HOSTED_ONLY
    public static void checkGenerateSourcesInSync(String project, Class source, Class target, Customizer customizer) {
        try {
            if (JniFunctionsGenerator.generate(true, project, source, target, customizer == null ? new JniCustomizer() : customizer)) {
                String thisFile = target.getSimpleName() + ".java";
                String sourceFile = source.getSimpleName() + ".java";
                FatalError.unexpected(String.format("%n%n" + thisFile +
                    " is out of sync with respect to " + sourceFile + ".%n" +
                    "Run 'mx jnigen or mx jvmtigen', recompile " + thisFile + " (or refresh it in your IDE)" +
                    " and restart the bootstrapping process.%n%n"));
            }
        } catch (Exception exception) {
            FatalError.unexpected("Error while generating source for " + target, exception);
        }
    }
}
