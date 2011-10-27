/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.tele.Inspectable.isVmInspected;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * Interface to the platform dependent functions for dealing with dynamically loaded libraries and
 * resolving symbols in the main program.
 *
 * The naming convention used matches that of the POSIX C dynamic linking functions (usually
 * defined in dlfcn.h).
 *
 * This class plays a key role in the VM bootstrap by providing a simple path to resolve critical
 * native symbols, e.g., for debugging and monitor support. Everything actually hinges on three native functions,
 * "dlsym", "dlerror" and "dlopen", which are passed to the {@link #initialize} method. Every other native
 *  symbol  can be found from there.
 *
 *  If the VM is being inspected we maintain data structures that support access to native functions.
 */
public final class DynamicLinker {

    public static boolean TraceDL;
    static {
        VMOptions.addFieldOption("-XX:", "TraceDL", "Trace Dynamic Linker calls.");
    }

    private DynamicLinker() {
    }

    private static Word mainHandle = Word.zero();

    private static final NativeFunction dlopen = resolveNativeFunction("dlopen", Address.class);
    private static final NativeFunction dlsym = resolveNativeFunction("dlsym", Word.class, Address.class);
    private static final NativeFunction dlerror = resolveNativeFunction("dlerror");

    @HOSTED_ONLY
    private static NativeFunction resolveNativeFunction(String name, Class... parameterTypes) {
        ClassMethodActor cma = (ClassMethodActor) ClassRegistry.findMethod(DynamicLinker.class, name, parameterTypes);
        return cma.nativeFunction;
    }

    /**
     * The non-moving raw memory buffer used to pass data to the native dynamic linker functions.
     * Any use of the buffer must synchronize on this object once the VM is multi-threaded.
     */
    private static final BootMemory buffer = new BootMemory(4 * Ints.K);

    /**
     * Initialize the system, "opening" the main program dynamic library.
    */
    public static void initialize(Word dlopenAddress, Word dlsymAddress, Word dlerrorAddress) {
        dlopen.setAddress(dlopenAddress.asAddress());
        dlsym.setAddress(dlsymAddress.asAddress());
        dlerror.setAddress(dlerrorAddress.asAddress());
        mainHandle = dlopen(Address.zero());
        LibInfo.add(Pointer.zero(), mainHandle);
    }

    @C_FUNCTION
    private static native Word dlopen(Address absolutePath);

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
            // This should not happen as mainHandle loaded in initialize (mjj)
            handle = mainHandle;
        } else {
            Pointer cString = CString.utf8FromJava(absolutePath);
            handle = dlopen(cString);
            if (handle.isZero()) {
                Memory.deallocate(cString);
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
            if (NativeInterfaces.verbose()) {
                Log.println("Loaded library from " + absolutePath + " to " + handle.toHexString());
            }
            LibInfo.add(cString, handle);
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
     * Variant of {@link #load(String)} used to load JVMTI agent libraries, before we have a heap.
     * @param absolutePath
     * @return
     */
    public static Word load(Pointer pathAsCString) {
        Word handle = dlopen(pathAsCString);
        if (handle.isNotZero()) {
            LibInfo.add(pathAsCString, handle);
        }
        return handle;
    }

    /**
     * Looks up a symbol from a given dynamically loaded native library.
     *
     * @param handle a handle to a native library dynamically loaded by {@link #load}
     * @param symbol the symbol to lookup
     * @return the address of the symbol or null if it is not found in the library
     */
    public static Word lookupSymbol(Word handle, String symbol) {
        Word h = handle;
        if (h.isZero()) {
            h = mainHandle;
        }
        if (MaxineVM.isPrimordialOrPristine()) {
            return dlsym(symbol, h);
        }
        synchronized (buffer) {
            return dlsym(symbol, h);
        }
    }

    /**
     * Looks up a symbol in a given library.
     *
     * @param symbol one or two symbols to be used for the lookup (see
     *            {@link Mangle#mangleMethod(TypeDescriptor, String, SignatureDescriptor, boolean)})
     * @param h a handle to a native library dynamically loaded by {@link #load}
     * @return the address or null if it is not found in the library
     */
    private static Word dlsym(String symbol, Word h) {
        if (TraceDL) {
            traceEntry(symbol, h);
        }
        Word addr;
        int delim = symbol.indexOf(Mangle.LONG_NAME_DELIMITER);
        if (delim == -1) {
            BootMemory buf = buffer;
            int bufRem = buf.size();
            final int i = CString.writePartialUtf8(symbol, 0, symbol.length(), buf.address(), bufRem);
            FatalError.check(i == symbol.length() + 1, "Symbol name is too long for buffer");
            addr = dlsym(h, buf.address());
        } else {
            BootMemory buf = buffer;
            int shortNameLength = delim;
            int i = CString.writePartialUtf8(symbol, 0, shortNameLength, buf.address(), buf.size());
            FatalError.check(i == shortNameLength + 1, "Symbol name is too long for buffer");

            addr = dlsym(h, buf.address());
            if (addr.isZero()) {
                int longNameSuffixLength = symbol.length() - (delim + 1);
                i = CString.writePartialUtf8(symbol, delim + 1, longNameSuffixLength, buf.address().plus(shortNameLength), buf.size() - i);
                FatalError.check(i == longNameSuffixLength + 1, "Symbol name is too long for buffer");
                addr = dlsym(h, buf.address());
            }
        }
        if (addr.isNotZero() && criticalDone && isVmInspected()) {
            LibInfo.setSentinel(h, buffer.address(), addr.asAddress());
        }
        return addr;
    }

    private static void traceEntry(String symbol, Word handle) {
        boolean lockDisabledSafepoints = Log.lock();
        Log.print("[Thread \"");
        Log.print(VmThread.current().getName());
        Log.print("\" dlsym(");
        Log.print(symbol); Log.print(") in ");
        printLibrary(handle);
        Log.println("]");
        Log.unlock(lockDisabledSafepoints);
    }

    private static void printLibrary(Word handle) {
        for (int i = 0; i < libInfoIndex; i++) {
            LibInfo libInfo = libInfoArray[i];
            if (libInfo.handle.equals(handle)) {
                if (libInfo.pathAsCString.isZero()) {
                    Log.print("maxvm");
                } else {
                    Log.printCString(libInfo.pathAsCString);
                }
                return;
            }
        }
        Log.print("unknown library");
    }

    public static void close(Word handle) {
        // TODO: should we check for unsuccessful close?
        dlclose(handle);
    }

    @ALIAS(declaringClass = ClassLoader.class)
    private static native long findNative(ClassLoader loader, String name);

    /**
     * Looks up the symbol for a native method.
     *
     * @param classMethodActor the actor for a native method
     * @param symbol the symbol of the native method's implementation
     * @return the address of {@code symbol}
     * @throws UnsatisfiedLinkError if the symbol cannot be found in any of the dynamic libraries bound to the VM
     */
    public static Word lookup(MethodActor classMethodActor, String symbol) throws UnsatisfiedLinkError {
        Word symbolAddress = Word.zero();
        if (MaxineVM.isHosted()) {
            symbolAddress = MethodID.fromMethodActor(classMethodActor);
        } else {
            // First look in the native libraries loaded by the class loader of the class in which this native method was declared
            ClassLoader classLoader = classMethodActor.holder().classLoader;
            symbolAddress = Address.fromLong(findNative(classLoader, symbol));
            // Now look in the system library path
            if (symbolAddress.isZero() && classLoader != null) {
                symbolAddress = Address.fromLong(findNative(null, symbol));
            }
        }
        if (symbolAddress.isZero()) {
            throw new UnsatisfiedLinkError(symbol);
        }
        return symbolAddress;
    }


   /*
    * Inspector support for finding native functions. dlfcn isn't very helpful.
    * We make an assumption that functions in the library
    * are loaded contiguously. We can't find the base address
    * only the value of a given symbol. Also dlsym has a complex
    * lookup mechanism that, for example, will return the
    * same value for a duplicate symbol when looked up using
    * different library handles. So we can't use a sentinel symbol
    * like "_init", which is found in all libraries. So we set a random
    * sentinel based on the first symbol looked up. The Inspector
    * can then 'relocate" all the other symbols using the ELF
    * file, assuming a contiguous load.
    *
    * Library name is recorded always for tracing. The sentinel symbol is only
    * recorder if the VM is being inspected.
    */

    static {
        new CriticalNativeMethod(Memory.class, "memory_allocate");
    }

    /**
     * This is only important for Inspector support as it prevents runaway recursion
     * in library sentinel function handling.
     */
    private static boolean criticalDone;

    /**
     * Critical native methods linked, so safe to call {@link Memory.allocate}.
     */
    public static void criticalLinked() {
        criticalDone = true;
        // This will force the sentinel to be set for mainHandle
        lookupSymbol(mainHandle, "log_lock");
    }

    public static class LibInfo {
        @INSPECTED
        Pointer pathAsCString;
        @INSPECTED
        Word handle;
        @INSPECTED
        Pointer sentinelAsCString;
        @INSPECTED
        Address sentinelAddress;

        static void add(Pointer pathAsCString, Word handle) {
            if (libInfoIndex < 16) {
                // TODO increase space
                LibInfo libInfo = libInfoArray[libInfoIndex];
                libInfo.pathAsCString = pathAsCString;
                libInfo.handle = handle;
                libInfoIndex++;
            }
        }

        static void setSentinel(Word handle, Pointer sentinel, Address sentinelAddress) {
            for (int i = 0; i < libInfoIndex; i++) {
                LibInfo libInfo = libInfoArray[i];
                if (libInfo.handle.equals(handle)) {
                    if (libInfo.sentinelAsCString.isZero()) {
                        Size length = CString.length(sentinel).plus(1);
                        Pointer sentinelCopy = Memory.mustAllocate(length);
                        Memory.copyBytes(sentinel, sentinelCopy, length);
                        libInfo.sentinelAsCString = sentinelCopy;
                        libInfo.sentinelAddress = sentinelAddress;
                    }
                }
            }
        }
    }

    @INSPECTED
    private static int libInfoIndex;
    @INSPECTED
    private static LibInfo[] libInfoArray;

    static {
        libInfoArray = new LibInfo[16];
        for (int i = 0; i < libInfoArray.length; i++) {
            libInfoArray[i] = new LibInfo();
        }
    }


}
