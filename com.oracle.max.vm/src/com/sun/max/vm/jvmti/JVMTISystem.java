/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jvmti;

import static com.sun.max.platform.Platform.*;

import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.jni.*;

/**
 * Similar code to that in {@link System#initProperties} that works in PRISTINE mode.
 * Really wish this wasn't necessary. Needed to locate agent libraries specified without explicit paths.
 */
public class JVMTISystem {

    private static final byte[] JAVA_HOME_BYTES = "JAVA_HOME".getBytes();
    private static final byte[] DARWIN_JAVA_HOME_DEFAULT_BYTES = "/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home".getBytes();
    private static final byte[] LD_LIBRARY_PATH_BYTES = "LD_LIBRARY_PATH".getBytes();
    private static final byte[] LIB_BYTES = "lib".getBytes();
    private static final byte[] JNILIB_BYTES = "jnilib".getBytes();
    private static final byte[] DYLIB_BYTES = "dylib".getBytes();
    /**
     * Finds the Java home path, where the JDK libraries are found.
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
                    javaHome = JVMTIUtil.getByteArrayStart(DARWIN_JAVA_HOME_DEFAULT_BYTES);
                }

                if (!CString.endsWith(javaHome, "/Home")) {
                    javaHome = CString.append(javaHome, "/Home");
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
                FatalError.check(CString.endsWith(javaHome, "/Home"), "The java.home system property should end with \"/Home\"");
                final Pointer javaPath = CString.chopSuffix(javaHome, "/Home");
                return CString.append(javaPath, "/Libraries");
            }

            case LINUX:
            case SOLARIS: {
                FatalError.check(CString.endsWith(javaHome, "jre"), "The java.home system property should end with \"/jre\"");
                final Pointer jreLibPath = CString.append(javaHome, "lib");
                return CString.append(jreLibPath, "amd64");
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

    private static class FatalError {

        static void check(boolean value, String msg) {
            if (!value) {
                Log.println(msg);
                MaxineVM.native_exit(1);
            }
        }
    }
}
