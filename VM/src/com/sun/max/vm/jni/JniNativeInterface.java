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
package com.sun.max.vm.jni;

import static com.sun.max.vm.VMOptions.*;

import java.io.*;
import java.util.regex.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;

/**
 * This class provides static functions for accessing the C data structure holding
 * the pointers to all the JNI functions. This is the data structure labeled as "Array of
 * pointers to JNI functions" in <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jni/spec/design.html#wp16696">Figure 2-1</a>.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class JniNativeInterface {

    private JniNativeInterface() {
    }

    @PROTOTYPE_ONLY
    private static StaticMethodActor[] getJniFunctionActors() {
        StaticMethodActor[] localStaticMethodActors = ClassActor.fromJava(JniFunctions.class).localStaticMethodActors();
        int count = 0;
        for (StaticMethodActor m : localStaticMethodActors) {
            if (m.isJniFunction()) {
                count++;
            }
        }
        final StaticMethodActor[] jniFunctionActors = new StaticMethodActor[count];
        int i = 0;
        for (StaticMethodActor m : localStaticMethodActors) {
            if (m.isJniFunction()) {
                jniFunctionActors[i++] = m;
            }
        }
        assert i == count;

        checkAgainstJniHeaderFile(jniFunctionActors);
        return jniFunctionActors;
    }

    @PROTOTYPE_ONLY
    private static void checkAgainstJniHeaderFile(StaticMethodActor[] jniFunctionActors) {
        String jniHeaderFilePath = System.getProperty("max.jni.headerFile");
        if (jniHeaderFilePath == null) {
            jniHeaderFilePath = System.getProperty("java.home");
            final String jreTail = File.separator + "jre";
            if (jniHeaderFilePath.endsWith(jreTail)) {
                jniHeaderFilePath = Strings.chopSuffix(jniHeaderFilePath, jreTail);
            }
            jniHeaderFilePath += File.separator + "include" + File.separator + "jni.h";
        }

        final File jniHeaderFile = new File(jniHeaderFilePath);
        ProgramError.check(jniHeaderFile.exists(), "JNI header file " + jniHeaderFile + " does not exist");

        // This program expects all JNI function prototypes in jni.h to split over 2 lines,
        // with the name of the function being on the first line and having the form:
        //
        //    <return_type> (JNICALL *<function name>)
        //
        // For example:
        //
        //    jlong (JNICALL *CallLongMethod)
        //
        final Pattern pattern = Pattern.compile(".*\\(JNICALL \\*([^\\)]+)\\).*");

        final AppendableIndexedSequence<String> jniFunctionNames = new ArrayListSequence<String>();

        // Prepend the reserved function slots
        jniFunctionNames.append("reserved0");
        jniFunctionNames.append("reserved1");
        jniFunctionNames.append("reserved2");
        jniFunctionNames.append("reserved3");

        try {
            final BufferedReader lineReader = new BufferedReader(new FileReader(jniHeaderFile));
            String line;
            while ((line = lineReader.readLine()) != null) {
                final Matcher matcher = pattern.matcher(line);
                if (matcher.matches() && !line.contains("JavaVM *vm")) {
                    final String functionName = matcher.group(1);
                    jniFunctionNames.append(functionName);
                }
            }
        } catch (IOException ioException) {
            ProgramError.unexpected("Error reading JNI header file " + jniHeaderFilePath, ioException);
        }

        // Add the two MaxineVM specific JNI functions
        jniFunctionNames.append("GetNumberOfArguments");
        jniFunctionNames.append("GetKindsOfArguments");

        for (int i = 0; i != jniFunctionActors.length; ++i) {
            final String jniFunctionName = jniFunctionNames.get(i);
            final String jniFunctionActorName = jniFunctionActors[i].name.toString();
            ProgramError.check(jniFunctionName.equals(jniFunctionActorName), "JNI function " + jniFunctionName + " at index " + i + " does not match JNI function actor " + jniFunctionActorName);
        }

        ProgramError.check(jniFunctionNames.length() == jniFunctionActors.length);
    }

    private static final StaticMethodActor[] jniFunctionActors = getJniFunctionActors();

    private static final CriticalMethod[] jniFunctions;
    static {
        jniFunctions = new CriticalMethod[jniFunctionActors.length];
        for (int i = 0; i < jniFunctions.length; ++i) {
            StaticMethodActor staticMethodActor = jniFunctionActors[i];
            if (!staticMethodActor.isNative()) {
                jniFunctions[i] = new CriticalMethod(staticMethodActor, CallEntryPoint.C_ENTRY_POINT);
            }
        }
    }

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
     *
     * @param jniEnv pointer the JNI function table
     */
    public static void initialize(Pointer jniEnv) {
        JniNativeInterface.jniEnv = jniEnv;
        for (int i = 0; i < jniFunctions.length; i++) {
            CriticalMethod function = jniFunctions[i];
            if (function != null) {
                final Word functionPointer = function.address();
                if (!jniEnv.getWord(i).isZero()) {
                    Log.print("Overwriting value ");
                    Log.print(jniEnv.getWord(i));
                    Log.print(" in JNI function table at index ");
                    Log.print(i);
                    Log.print(" with function ");
                    Log.printMethod(function.classMethodActor, true);
                    FatalError.crash("Multiple implementations for a JNI function");
                }
                jniEnv.setWord(i, functionPointer);
            } else {
                if (jniEnv.getWord(i).isZero()) {
                    Log.print("Entry in JNI function table at index ");
                    Log.print(i);
                    Log.println(" for ");
                    Log.printMethod(jniFunctionActors[i], false);
                    Log.println(" has no implementation");
                    FatalError.crash("Missing implementation for JNI function");
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
        return verboseOption.verboseJNI || ClassMethodActor.traceJNI();
    }
}
