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
package com.sun.max.vm.jdk;

import static com.sun.max.vm.type.ClassRegistry.*;

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
        NativeLibrary_handle.setLong(this, address.toLong());
    }

    /**
     * Looks up a symbol in this native library.
     * @see java.lang.ClassLoader.NativeLibrary#load(String)
     * @param symbolName the name of the symbol as a string
     * @return a pointer to the native symbol encoded as a long; {@code 0L} if the symbol is not found
     */
    @SUBSTITUTE
    long find(String symbolName) {
        final Address handle = Address.fromLong(NativeLibrary_handle.getLong(this));
        return DynamicLinker.lookupSymbol(handle, symbolName).asAddress().toLong();
    }

    /**
     * Unloads this native library.
     * @see java.lang.ClassLoader.NativeLibrary#unload()
     */
    @SUBSTITUTE
    void unload() {
        final Address handle = Address.fromLong(NativeLibrary_handle.getLong(this));
        DynamicLinker.close(handle);
    }

}
