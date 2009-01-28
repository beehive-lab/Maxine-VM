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
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
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
 * native symbols, e.g., for debugging and monitor support. Eveything actually hinges on two native functions,
 * "dlsym' and "nativeOpenDynamicLibrary", which are passed to the @see initialize method. Every other native
 *  symbol  can be found from there, including other "dlxxx" symbols.
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 * @author Mick Jordan
 */
public final class DynamicLinker {

    private DynamicLinker() {
    }

    @CONSTANT_WHEN_NOT_ZERO
    private static Word _nativeOpenDynamicLibrary = Word.zero();

    @CONSTANT_WHEN_NOT_ZERO
    private static Word _dlsym = Word.zero();

    @CONSTANT_WHEN_NOT_ZERO
    private static Word _mainHandle = Word.zero();

    /**
     * Initialize the system, "opening" the main program dynamic library.
     *
     * @param nativeOpenDynamicLibrary
     * @param dlsym
    */
    public static void initialize(Word nativeOpenDynamicLibrary, Word dlsym) {
        _nativeOpenDynamicLibrary = nativeOpenDynamicLibrary;
        _dlsym = dlsym;
        _mainHandle = nativeOpenDynamicLibrary(Address.zero());
    }

    @C_FUNCTION
    private static native Word nativeOpenDynamicLibrary(Address absolutePath);

    @C_FUNCTION
    private static native Word dlerror();

    @C_FUNCTION
    private static native Word dlsym(Word handle, Address name);

    @C_FUNCTION
    private static native Word dlclose(Word handle);

    private static Word doLoad(String absolutePath) {
        final Word handle;
        if (absolutePath == null) {
            handle = _mainHandle;
        } else {
            final Pointer buffer = BootMemory.buffer();
            final int i = CString.writePartialUtf8(absolutePath, 0, buffer, BootMemory.bufferSize());
            assert i == absolutePath.length();
            handle = nativeOpenDynamicLibrary(buffer);
        }
        if (handle.isZero()) {
            try {
                throw new UnsatisfiedLinkError(CString.utf8ToJava(dlerror().asPointer()));
            } catch (Utf8Exception utf8Exception) {
                throw new UnsatisfiedLinkError();
            }
        }
        if (VerboseVMOption.verboseJNI()) {
            Log.println("Loaded library from " + absolutePath);
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
        synchronized (BootMemory.class) {
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
                return _nativeOpenDynamicLibrary;
            } else if ("dlsym".equals(name)) {
                return _dlsym;
            }
            h = _mainHandle;
            if (MaxineVM.isPrimordialOrPristine()) {
                // 'synchronized' does not work yet while starting,
                // but we don't need it in that case, we are still single-threaded.
                // N.B. the first call to dlsym will cause a recursive call to this
                // method since it is not yet resolved. The recursion is broken by the
                // explicit check above.
                final Pointer buffer = BootMemory.buffer();
                final int i = CString.writePartialUtf8(name, 0, buffer, BootMemory.bufferSize());
                assert i == name.length();
                return dlsym(h, buffer);
            }
        }
        synchronized (BootMemory.class) {
            final Pointer buffer = BootMemory.buffer();
            final int i = CString.writePartialUtf8(name, 0, buffer, BootMemory.bufferSize());
            assert i == name.length();
            return dlsym(h, buffer);
        }
    }

    public static void close(Word handle) {
        // TODO: should we check for unsuccessful close?
        dlclose(handle);
    }

    private static Word _javaHandle;

    public static void loadJavaLibrary(String path) {
        _javaHandle = DynamicLinker.load(path + File.separator + System.mapLibraryName("java"));
    }

    /**
     * The method ClassLoader.findNative(ClassLoader, String) has to be called via reflection as it is not publicly accessible.
     */
    private static MethodActor _findNativeMethod;

    private static MethodActor getFindNativeMethod() {
        if (_findNativeMethod == null) {
            _findNativeMethod = ClassActor.fromJava(ClassLoader.class).findLocalStaticMethodActor(SymbolTable.makeSymbol("findNative"), SignatureDescriptor.create(long.class, ClassLoader.class, String.class));
        }
        return _findNativeMethod;
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
                    final ClassLoader classLoader = classMethodActor.holder().classLoader();
                    if (classLoader == VmClassLoader.VM_CLASS_LOADER && !_javaHandle.isZero()) {
                        symbolAddress = lookupSymbol(_javaHandle, symbol);
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
                throw ProgramError.unexpected(e);
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
