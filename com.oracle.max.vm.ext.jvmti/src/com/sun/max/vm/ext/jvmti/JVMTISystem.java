/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.ext.jvmti;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.ext.jvmti.JJVMTI.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.jni.*;

/**
 * Custom access to system properties for JVMTI.
 *
 * Much of this is similar code to that in {@link System#initProperties} but works in PRISTINE mode.
 * Really wish this wasn't necessary. It is needed to locate agent libraries specified without explicit paths,
 * i.e., {@code -agentlib}.
 */
public class JVMTISystem {

    private static final byte[] JAVA_HOME_BYTES = "JAVA_HOME".getBytes();
    private static final byte[] DARWIN_JAVA_HOME_JDK6_DEFAULT_BYTES = JDK_java_lang_System.DARWIN_JAVA_HOME_JDK6_DEFAULT.getBytes();
    private static final byte[] DARWIN_JAVA_HOME_JDK7_DEFAULT_BYTES = JDK_java_lang_System.DARWIN_JAVA_HOME_JDK7_DEFAULT.getBytes();
    private static final byte[] LD_LIBRARY_PATH_BYTES = "LD_LIBRARY_PATH".getBytes();
    private static final byte[] LIB_BYTES = "lib".getBytes();
    private static final byte[] JNILIB_BYTES = "jnilib".getBytes();
    private static final byte[] DYLIB_BYTES = "dylib".getBytes();

    /**
     * Finds the Java home path, where the JDK libraries are found.
     * Code should be functionally equivalent to {@link JDK_java_lang_System#findJavaHome}.
     *
     * @return a C string representing the path of the Java home
     */
    static Pointer findJavaHome() {
        switch (platform().os) {
            case MAXVE:
            case SOLARIS:
            case LINUX: {
                // TODO: Assume we are in the JRE and walk around from there.

                // For now, we just rely on the JAVA_HOME environment variable being set:
                // String javaHome = getenv("JAVA_HOME", false);
                Pointer javaHome = JVMTIEnvVar.getValue(JVMTIUtil.getByteArrayStart(JAVA_HOME_BYTES));
                // FatalError.check(javaHome != null, "Environment variable JAVA_HOME not set");
                if (javaHome.isZero()) {
                    Log.println("Environment variable JAVA_HOME not set");
                    MaxineVM.native_exit(-1);
                }
                // FatalError.check(javaHome != null, "Environment variable JAVA_HOME not set");

                if (!CString.endsWith(javaHome, "/jre")) {
                    // A lot of JDK code expects java.home to point to the "jre" subdirectory in a JDK.
                    // This makes interpreting java.home easier if only a JRE is installed.
                    // However, quite often JAVA_HOME points to the top level directory of a JDK
                    // installation and so it needs to be adjusted to append the "jre" subdirectory.
                    javaHome = CString.append(javaHome, "/jre");
                }
                return javaHome;
            }
            case WINDOWS: {
                FatalError.check(false, "Initialization of java.home is unimplemented");
                break;
            }
            case DARWIN: {
                // TODO: Assume we are in the JRE and walk around from there.

                // For now, we just rely on the JAVA_HOME environment variable being set:
                // String javaHome = getenv("JAVA_HOME", false);
                Pointer javaHome = JVMTIEnvVar.getValue(JVMTIUtil.getByteArrayStart(JAVA_HOME_BYTES));
                if (javaHome.isZero()) {
                    if (JDK.JDK_VERSION == JDK.JDK_6) {
                        javaHome = JVMTIUtil.getByteArrayStart(DARWIN_JAVA_HOME_JDK6_DEFAULT_BYTES);
                    } else if (JDK.JDK_VERSION == JDK.JDK_7) {
                        javaHome = JVMTIUtil.getByteArrayStart(DARWIN_JAVA_HOME_JDK7_DEFAULT_BYTES);
                    } else {
                        FatalError.check(false, "Unsupported DARWIN JDK version");
                    }
                }
                if (JDK.JDK_VERSION == JDK.JDK_6) {
                    if (!CString.endsWith(javaHome, "/Home")) {
                        javaHome = CString.append(javaHome, "/Home");
                    }
                } else if (JDK.JDK_VERSION == JDK.JDK_7) {
                    if (!CString.endsWith(javaHome, "/jre")) {
                        javaHome = CString.append(javaHome, "/jre");
                    }
                }
                return javaHome;
            }
        }
        return Pointer.zero();
    }

    /**
     * Retrieves the java library path from OS-specific environment variable(s).
     *
     * @return a string representing the java library path as determined from the OS environment
     */
    private static Pointer getLdLibraryPath() {
        switch (platform().os) {
            case DARWIN:
            case LINUX:
            case SOLARIS: {
                return JVMTIEnvVar.getValue(JVMTIUtil.getByteArrayStart(LD_LIBRARY_PATH_BYTES));
            }
            case WINDOWS:
            case MAXVE:
            default: {
                FatalError.check(false, "unimplemented");
                return Pointer.zero();
            }
        }
    }

    static Pointer getBootLibraryPath() {
        Pointer javaHome = findJavaHome();
        switch (platform().os) {
            case DARWIN: {
                if (JDK.JDK_VERSION == JDK.JDK_7) {
                    FatalError.check(CString.endsWith(javaHome, "/jre"), "The java.home system property should end with \"/jre\"");
                    return CString.append(javaHome, "/lib");
                } else if (JDK.JDK_VERSION == JDK.JDK_6) {
                    FatalError.check(CString.endsWith(javaHome, "/Home"), "The java.home system property should end with \"/Home\"");
                    final Pointer javaPath = CString.chopSuffix(javaHome, "/Home");
                    return CString.append(javaPath, "/Libraries");
                }
            }

            case LINUX:
            case SOLARIS: {
                FatalError.check(CString.endsWith(javaHome, "jre"), "The java.home system property should end with \"/jre\"");
                final Pointer jreLibPath = CString.append(javaHome, "/lib");
                return CString.append(jreLibPath, "/amd64");
            }
            case WINDOWS:
            case MAXVE:
            default: {
                return Pointer.zero();
            }
        }

    }

    static Pointer asPath(Pointer head, Pointer tail) {
        return CString.appendCString(CString.append(head, "/"), tail);
    }

    /**
     * Converts a simple library name to a platform-specific name with either or both
     * a prefix and a suffix.
     *
     * @param libraryName the name of the library
     * @return a string representing the traditional name of a native library on this platform
     */
    static Pointer mapLibraryName(Pointer libraryName, int attempt) {
        Pointer libName = CString.appendCString(JVMTIUtil.getByteArrayStart(LIB_BYTES), libraryName);
        switch (platform().os) {
            case DARWIN:
                return CString.append(libName, attempt == 0 ? ".jnilib" : ".dylib");
            case LINUX:
            case SOLARIS:
                return  CString.append(libName, ".so");
            case WINDOWS:
            case MAXVE:
            default:
                return Pointer.zero();
        }
    }

    static Word load(Pointer name) {
        Word handle = Pointer.zero();
        Pointer libPath = JVMTISystem.getBootLibraryPath();
        int mapAttempt = 0;
        boolean triedBoth = false;
        while (true) {
            Pointer libName = JVMTISystem.mapLibraryName(name, mapAttempt);
            Pointer absPath = JVMTISystem.asPath(libPath, libName);
            handle = DynamicLinker.load(absPath);
            if (handle.isNotZero()) {
                break;
            }
            if (platform().os == OS.DARWIN && mapAttempt == 0) {
                mapAttempt++;
            } else {
                if (triedBoth) {
                    break;
                }
                triedBoth = true;
                libPath = getLdLibraryPath();
                if (libPath.isZero()) {
                    break;
                }
            }
        }
        return handle;
    }

    static int setVerboseFlag(int flag, boolean value) {
        // PHASES: ANY
        switch (flag) {
            case JVMTI_VERBOSE_GC:
                VMOptions.verboseOption.verboseGC = value;
                break;
            case JVMTI_VERBOSE_CLASS:
                VMOptions.verboseOption.verboseClass = value;
                break;
            case JVMTI_VERBOSE_JNI:
                VMOptions.verboseOption.verboseJNI = value;
                break;
            case JVMTI_VERBOSE_OTHER:
                VMOptions.verboseOption.verboseCompilation = value;
                break;
            default:
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        }
        return JVMTI_ERROR_NONE;
    }

    /*
     * Access to VM properties (misleadingly named "system" properties in the API). Simple enough, except for one issue:
     *
     * 1. The values are all supposed to be available in the OnLoad phase, which is a problem for Maxine as
     *    many of them are not computable until the VM is initialized. TODO not implemented.
     *    With a lot of grunt work the existing code that sets these properties could be made to work "C style",
     *    but does anyone care?
     */

    /**
     * Called during OnLoad to make sure the command line properties are processed here before they get processed
     * {@link VMOptions} which destroys them.
     */
    static void initSystemProperties() {
        VMOptionsAccess.getCommandLineSystemProperties();
    }

    static String getSystemProperty(String name) {
        // This is access to a VM property (including command line properties), not an arbitrary system property.
        int clCount = VMOptionsAccess.getCommandLineSystemProperties();
        int vmpCount = VMProperty.VALUES.length;
        int count = clCount + vmpCount;
        for (int i = 0; i < count; i++) {
            if (i < vmpCount) {
                VMProperty vmProperty = VMProperty.VALUES[i];
                if (name.equals(vmProperty.property)) {
                    String value = vmProperty.value();
                    if (value != null) {
                        return value;
                    } else {
                        throw new JJVMTIException(JVMTI_ERROR_NOT_AVAILABLE);
                    }
                }
            } else {
                Pointer key = VMOptionsAccess.keysArray.getWord(i - vmpCount).asPointer();
                if (CString.equals(key, name)) {
                    Pointer propValPtr = VMOptionsAccess.valuesArray.getWord(i - vmpCount).asPointer();
                    try {
                        return CString.utf8ToJava(propValPtr);
                    } catch (Utf8Exception ex) {
                        FatalError.check(false, "JVMTI: unexpected Utf8Exception");
                    }
                }
            }
        }
        throw new JJVMTIException(JVMTI_ERROR_NOT_AVAILABLE);
    }

    static String[] getSystemProperties() throws JJVMTIException {
        int clCount = VMOptionsAccess.getCommandLineSystemProperties();
        int vmpCount = VMProperty.VALUES.length;
        int count = clCount + vmpCount;
        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            if (i < vmpCount) {
                result[i] = VMProperty.VALUES[i].property;
            } else {
                try {
                    result[i] = CString.utf8ToJava(VMOptionsAccess.keysArray.getWord(i - vmpCount).asPointer());
                } catch (Utf8Exception ex) {
                    FatalError.check(false, "JVMTI: unexpected Utf8Exception");
                }
            }
        }
        return result;
    }

    static int getSystemProperty(Pointer env, Pointer property, Pointer valuePtr) {
        // This is access to a VM property (including command line properties), not an arbitrary system property.
        int clCount = VMOptionsAccess.getCommandLineSystemProperties();
        int vmpCount = VMProperty.VALUES.length;
        int count = clCount + vmpCount;
        for (int i = 0; i < count; i++) {
            Pointer propValPtr = Pointer.zero();
            int length = 0;
            if (i < vmpCount) {
                VMProperty vmProperty = VMProperty.VALUES[i];
                if (CString.equals(property, vmProperty.property)) {
                    if (vmProperty.valueAsCString().isNotZero()) {
                        propValPtr = vmProperty.valueAsCString();
                        length = CString.length(propValPtr).toInt();
                    } else {
                        return JVMTI_ERROR_NOT_AVAILABLE;
                    }
                }
            } else {
                Pointer key = VMOptionsAccess.keysArray.getWord(i - vmpCount).asPointer();
                if (CString.equals(property, key)) {
                    propValPtr = VMOptionsAccess.valuesArray.getWord(i - vmpCount).asPointer();
                    length = CString.length(propValPtr).toInt();
                }
            }
            if (propValPtr.isNotZero()) {
                Pointer propValCopyPtr = Memory.allocate(Size.fromInt(length + 1));
                if (propValCopyPtr.isZero()) {
                    return JVMTI_ERROR_OUT_OF_MEMORY;
                }
                Memory.copyBytes(propValPtr, propValCopyPtr, Size.fromInt(length));
                propValCopyPtr.setByte(length, (byte) 0);
                valuePtr.setWord(propValCopyPtr);
                return JVMTI_ERROR_NONE;
            }
        }
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    static int getSystemProperties(Pointer env, Pointer countPtr, Pointer propertyArrayPtrPtr) {
        int clCount = VMOptionsAccess.getCommandLineSystemProperties();
        int vmpCount = VMProperty.VALUES.length;
        int count = clCount + vmpCount;
        countPtr.setInt(count);
        Pointer propertyArrayPtr = Memory.allocate(Size.fromInt(count * Word.size()));
        if (propertyArrayPtr.isZero()) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }
        propertyArrayPtrPtr.setWord(propertyArrayPtr);

        for (int i = 0; i < count; i++) {
            Pointer propertyCString;
            int length;
            if (i < vmpCount) {
                VMProperty vmProperty = VMProperty.VALUES[i];
                String property = vmProperty.property;
                length = property.length();
                propertyCString = vmProperty.propertyAsCString();
            } else {
                propertyCString = VMOptionsAccess.keysArray.getWord(i - vmpCount).asPointer();
                length = CString.length(propertyCString).toInt();
            }
            Pointer propertyCopyPtr = Memory.allocate(Size.fromInt(length + 1));
            if (propertyCopyPtr.isZero()) {
                return JVMTI_ERROR_OUT_OF_MEMORY;
            }
            Memory.copyBytes(propertyCString, propertyCopyPtr, Size.fromInt(length));
            propertyCopyPtr.setByte(length, (byte) 0);
            propertyArrayPtr.setWord(i, propertyCopyPtr);
        }
        return JVMTI_ERROR_NONE;
    }

    static int setSystemProperty(Pointer env, Pointer property, Pointer valuePtr) {
        // we only process VM properties
        for (int i = 0; i < VMProperty.VALUES.length; i++) {
            VMProperty vmProperty = VMProperty.isVMProperty(property);
            if (vmProperty != null) {
                if (valuePtr.isZero()) {
                    // writeable test
                    return vmProperty.mutable ? JVMTI_ERROR_NONE : JVMTI_ERROR_NOT_AVAILABLE;
                } else {
                    vmProperty.setValue(valuePtr);
                    return JVMTI_ERROR_NONE;
                }
            }
        }
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    /**
     * Extracts any system properties from the raw command line and saves them as key/value pairs.
     */
    private static class VMOptionsAccess extends VMOptions {
        private static int count;
        private static Pointer keysArray;
        private static Pointer valuesArray;
        private static boolean initialized;

        private static int getCommandLineSystemProperties() {
            if (initialized) {
                return count;
            }
            int argc = VMOptions.getArgumentCount();
            for (int i = 0; i < argc; i++) {
                Pointer arg = VMOptions.getArgumentCString(i);
                if (arg.isNotZero() && CString.startsWith(arg, "-D")) {
                    count++;
                }
            }
            keysArray = allocate(count * Word.size());
            valuesArray = allocate(count * Word.size());

            count = 0;
            for (int i = 0; i < argc; i++) {
                Pointer arg = VMOptions.getArgumentCString(i);
                if (arg.isNotZero() && CString.startsWith(arg, "-D")) {
                    Pointer keyStart = arg.plus(2);
                    Pointer p = keyStart;
                    Pointer valueStart = Pointer.zero();
                    while (p.readByte(0) != (byte) 0) {
                        if (p.readByte(0) == '=') {
                            valueStart = p.plus(1);
                        }
                        p = p.plus(1);
                    }
                    int keyLength;
                    int valueLength;
                    if (valueStart.isZero()) {
                        keyLength = p.minus(keyStart).toInt();
                        valueLength = 0;
                    } else {
                        keyLength = valueStart.minus(keyStart).toInt() - 1;
                        valueLength = p.minus(valueStart).toInt();
                    }
                    Pointer key = copyBytesAndNull(keyStart, keyLength);
                    // Check if an existing VM property, in which case we ignore it.
                    if (VMProperty.isVMProperty(key) != null) {
                        continue;
                    }
                    keysArray.setWord(count, key);
                    valuesArray.setWord(count, copyBytesAndNull(valueStart, valueLength));
                    count++;
                }
            }
            initialized = true;
            return count;
        }

        private static Pointer allocate(int length) {
            Pointer result = Memory.allocate(Size.fromInt(length));
            FatalError.check(result.isNotZero(), "JVMTI system property initialization failure");
            return result;
        }

        private static Pointer copyBytesAndNull(Pointer src, int length) {
            Pointer dst = allocate(length + 1);
            if (length > 0) {
                Memory.copyBytes(src, dst, Size.fromInt(length));
            }
            dst.writeByte(length, (byte) 0);
            return dst;
        }
    }


    private static class FatalError {

        static void check(boolean value, String msg) {
            if (!value) {
                Log.println(msg);
                MaxineVM.native_exit(1);
            }
        }
    }
}
