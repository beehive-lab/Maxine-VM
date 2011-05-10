/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jdk;

import static com.sun.cri.bytecode.Bytecodes.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.jni.*;

/**
 * Implements method substitutions necessary for native library handling.
 * This class makes substitutions for methods of a non-public inner class of
 * {@link java.lang.ClassLoader ClassLoader} called {@code NativeLibrary}.
 * Thus, it is only used internally in the JDK to handle loading and linking of
 * native libraries and symbols.
 */
@METHOD_SUBSTITUTIONS(value = ClassLoader.class, innerClass = "NativeLibrary")
public final class JDK_java_lang_ClassLoader_NativeLibrary {

    /**
     * Loads a library with the give absolute path.
     * @see java.lang.ClassLoader.NativeLibrary#load(String)
     * @param absolutePathname the pathname to the file that is a native library
     */
    @SUBSTITUTE
    void load(String absolutePathname) throws Throwable {
        final Address address = DynamicLinker.load(absolutePathname).asAddress();
        if (!address.isZero()) {
            // we need to look up JNI_OnLoad and if it exists, invoke it.
            final Address onload = DynamicLinker.lookupSymbol(address, "JNI_OnLoad").asAddress();
            if (!onload.isZero()) {
                try {
                    if (NativeInterfaces.verbose()) {
                        Log.println("Invoking JNI_OnLoad for library loaded from " + absolutePathname);
                    }
                    DynamicLinker.invokeJNIOnLoad(onload);
                    // TODO: check against the supported JNI version of this VM and throw UnsatisfiedLinkError if not supported
                } catch (Throwable t) {
                    if (NativeInterfaces.verbose()) {
                        Log.println("Error loading library from " + absolutePathname + ":");
                        t.printStackTrace(Log.out);
                    }
                    DynamicLinker.close(address);
                    if (NativeInterfaces.verbose()) {
                        Log.println("Closed library loaded from " + absolutePathname);
                    }
                    throw t;
                }
            }
        }
        JDK_java_lang_ClassLoader_NativeLibrary thisNativeLibrary = asThis(this);
        thisNativeLibrary.handle = address.toLong();
    }

    @ALIAS(declaringClass = ClassLoader.class, innerClass = "NativeLibrary")
    private long handle;

    @INTRINSIC(UNSAFE_CAST)
    private static native JDK_java_lang_ClassLoader_NativeLibrary asThis(Object nativeLibrary);

    /**
     * Looks up a symbol in this native library.
     * @see java.lang.ClassLoader.NativeLibrary#load(String)
     * @param symbolName the name of the symbol as a string
     * @return a pointer to the native symbol encoded as a long; {@code 0L} if the symbol is not found
     */
    @SUBSTITUTE
    long find(String symbolName) {
        JDK_java_lang_ClassLoader_NativeLibrary thisNativeLibrary = asThis(this);
        final Address handle = Address.fromLong(thisNativeLibrary.handle);
        return DynamicLinker.lookupSymbol(handle, symbolName).asAddress().toLong();
    }

    /**
     * Unloads this native library.
     * @see java.lang.ClassLoader.NativeLibrary#unload()
     */
    @SUBSTITUTE
    void unload() {
        JDK_java_lang_ClassLoader_NativeLibrary thisNativeLibrary = asThis(this);
        final Address handle = Address.fromLong(thisNativeLibrary.handle);
        DynamicLinker.close(handle);
    }

}
