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

import java.io.*;
import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Interface to the platform dependent functions for dealing with dynamically loaded libraries and
 * resolving symbols in the main program.
 *
 * The naming convention used matches that of the POSIX C dynamic linking functions (usually
 * defined in dlfcn.h).
 *
 * This class plays a key role in the VM bootstrap by providing a simple path to resolve critical
 * native symbols, e.g., for debugging and monitor support. Everything actually hinges on three native functions,
 * "dlsym", "dlerror" and "nativeOpenDynamicLibrary", which are passed to the {@link #initialize} method. Every other native
 *  symbol  can be found from there.
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 * @author Mick Jordan
 */
public final class DynamicLinker {

    private DynamicLinker() {
    }

    @CONSTANT_WHEN_NOT_ZERO
    private static Word nativeOpenDynamicLibrary = Word.zero();

    @CONSTANT_WHEN_NOT_ZERO
    private static Word dlsym = Word.zero();

    @CONSTANT_WHEN_NOT_ZERO
    private static Word dlerror = Word.zero();

    @CONSTANT_WHEN_NOT_ZERO
    private static Word mainHandle = Word.zero();

    /**
     * The non-moving raw memory buffer used to pass data to the native dynamic linker functions.
     * Any use of the buffer must synchronize on this object once the VM is multi-threaded.
     */
    private static final BootMemory buffer = new BootMemory(Ints.K);

    /**
     * Initialize the system, "opening" the main program dynamic library.
     *
     * @param nativeOpenDynamicLibrary
     * @param dlsym
     * @param dlerror
    */
    public static void initialize(Word nativeOpenDynamicLibraryAddress, Word dlsymAddress, Word dlerrorAddress) {
        nativeOpenDynamicLibrary = nativeOpenDynamicLibraryAddress;
        dlsym = dlsymAddress;
        dlerror = dlerrorAddress;
        mainHandle = nativeOpenDynamicLibrary(Address.zero());
    }

    @C_FUNCTION
    private static native Word nativeOpenDynamicLibrary(Address absolutePath);

    @C_FUNCTION
    private static native Word dlerror();

    @C_FUNCTION
    private static native Word dlsym(Word handle, Address name);

    @C_FUNCTION
    private static native Word dlclose(Word handle);

    public static native int invokeJNIOnLoad(Address onload);

    private static Word doLoad(String absolutePath) {
        final Word handle;
        if (absolutePath == null) {
            handle = mainHandle;
        } else {
            final int i = CString.writePartialUtf8(absolutePath, 0, buffer.address(), buffer.size());
            FatalError.check(i == absolutePath.length(), "Dynamic library path is too long for buffer");
            handle = nativeOpenDynamicLibrary(buffer.address());
        }
        if (handle.isZero()) {
            try {
                final Pointer errorMessage = dlerror().asPointer();
                if (errorMessage.isZero()) {
                    throw new UnsatisfiedLinkError(absolutePath);
                }
                throw new UnsatisfiedLinkError(absolutePath + ": " + CString.utf8ToJava(errorMessage));
            } catch (Utf8Exception utf8Exception) {
                throw new UnsatisfiedLinkError();
            }
        }
        if (JniNativeInterface.verbose()) {
            Log.println("Loaded library from " + absolutePath + " to " + handle.toHexString());
        }
        return handle;
    }

    /**
     * Loads the dynamic native library contained in a given file.
     *
     * @param absolutePath an absolute pathname denoting the library file to load
     * @return the opaque handle for the loaded library or {@link Address#zero()} if the library could not be loaded
     * @throws UnsatisfiedLinkError if there was an error locating or loading the library
     */
    public static Word load(String absolutePath) throws UnsatisfiedLinkError {
        if (MaxineVM.isPrimordialOrPristine()) {
            return doLoad(absolutePath);
        }
        synchronized (buffer) {
            return doLoad(absolutePath);
        }
    }

    /**
     * Looks up a symbol from a given dynamically loaded native library.
     *
     * @param handle a handle to a native library dynamically loaded by {@link #load}
     * @param name the name of the symbol to lookup
     * @return the address of the symbol or null if it is not found in the library
     */
    public static Word lookupSymbol(Word handle, String name) {
        Word h = handle;
        if (h.isZero()) {
            if ("nativeOpenDynamicLibrary".equals(name)) {
                return nativeOpenDynamicLibrary;
            } else if ("dlsym".equals(name)) {
                return dlsym;
            } else if ("dlerror".equals(name)) {
                return dlerror;
            }
            h = mainHandle;
            if (MaxineVM.isPrimordialOrPristine()) {
                // N.B. the first call to dlsym will cause a recursive call to this
                // method since it is not yet resolved. The recursion is broken by the
                // explicit check above.
                final int i = CString.writePartialUtf8(name, 0, buffer.address(), buffer.size());
                FatalError.check(i == name.length(), "Symbol name is too long for buffer");
                return dlsym(h, buffer.address());
            }
        }
        synchronized (buffer) {
            final int i = CString.writePartialUtf8(name, 0, buffer.address(), buffer.size());
            FatalError.check(i == name.length(), "Symbol name is too long for buffer");
            return dlsym(h, buffer.address());
        }
    }

    public static void close(Word handle) {
        // TODO: should we check for unsuccessful close?
        dlclose(handle);
    }

    private static Word javaHandle;

    public static void loadJavaLibrary(String path) {
        javaHandle = DynamicLinker.load(path + File.separator + System.mapLibraryName("java"));
    }

    /**
     * The method ClassLoader.findNative(ClassLoader, String) has to be called via reflection as it is not publicly accessible.
     */
    private static MethodActor findNativeMethod;

    private static MethodActor getFindNativeMethod() {
        if (findNativeMethod == null) {
            findNativeMethod = ClassActor.fromJava(ClassLoader.class).findLocalStaticMethodActor(SymbolTable.makeSymbol("findNative"), SignatureDescriptor.create(long.class, ClassLoader.class, String.class));
        }
        return findNativeMethod;
    }

    /**
     * Looks up the symbol for a native method.
     *
     * @param classMethodActor the actor for a native method
     * @param symbol the symbol of the native method's implementation
     * @return the address of {@code symbol}
     * @throws UnsatisfiedLinkError if the symbol cannot be found in any of the dynamic libraries bound to the VM
     */
    public static Word lookup(MethodActor classMethodActor, String symbol) throws UnsatisfiedLinkError {
        Word symbolAddress = null;
        if (MaxineVM.isPrototyping()) {
            symbolAddress = MethodID.fromMethodActor(classMethodActor);
        } else {
            try {
                // First look in the boot image
                // TODO: This could be removed if ClassLoader.findNative could find symbols in the boot image
                symbolAddress = lookupSymbol(Word.zero(), symbol);
                if (symbolAddress.isZero()) {
                    final ClassLoader classLoader = classMethodActor.holder().classLoader;
                    if (classLoader == VmClassLoader.VM_CLASS_LOADER && !javaHandle.isZero()) {
                        symbolAddress = lookupSymbol(javaHandle, symbol);
                        if (!symbolAddress.isZero()) {
                            return symbolAddress;
                        }
                    }

                    // Now look in the native libraries loaded by the class loader of the class in which this native method was declared
                    symbolAddress = Address.fromLong(getFindNativeMethod().invoke(ReferenceValue.from(classLoader), ReferenceValue.from(symbol)).asLong());
                    // Now look in the system library path
                    if (symbolAddress.isZero() && classLoader != null) {
                        symbolAddress = Address.fromLong(getFindNativeMethod().invoke(ReferenceValue.NULL, ReferenceValue.from(symbol)).asLong());
                    }
                }
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw ProgramError.unexpected(e);
            }
            if (symbolAddress.isZero()) {
                throw new UnsatisfiedLinkError(symbol);
            }
        }
        return symbolAddress;
    }
}
