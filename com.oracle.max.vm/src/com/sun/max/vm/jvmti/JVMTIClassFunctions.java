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

import static com.sun.max.vm.jvmti.JVMTICallbacks.*;
import static com.sun.max.vm.jvmti.JVMTIConstants.*;
import static com.sun.max.vm.jvmti.JVMTI.*;

import java.io.*;
import java.security.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * Support for all the JVMTI functions related to {@link Class} handling.
 * @author Mick Jordan
 *
 */
class JVMTIClassFunctions {

    /**
     * Dispatch the {@link JVMTIEvent#CLASS_FILE_LOAD_HOOK}.
     * We do not check the event state, caller is presumed to have called {@link JVMTI#eventNeeded(int).
     * @param classLoader null for boot
     * @param className
     * @param protectionDomain
     * @param classfileBytes
     * @return transformed bytes or null if no change
     */
    static byte[] classFileLoadHook(ClassLoader classLoader, String className, ProtectionDomain protectionDomain, byte[] classfileBytes) {
        Pointer newClassDataLenPtr = Intrinsics.stackAllocate(Pointer.size());
        Pointer newClassDataPtrPtr = Intrinsics.stackAllocate(Pointer.size());
        Pointer classDataPtr = Reference.fromJava(classfileBytes).toOrigin().plus(JVMTIUtil.byteDataOffset);
        int classfileBytesLength = classfileBytes.length;
        boolean changed = false;
        for (int i = 0; i < jvmtiEnvs.length; i++) {
            Pointer callback = getCallbackForEvent(jvmtiEnvs[i], JVMTIEvent.CLASS_FILE_LOAD_HOOK);
            if (callback.isZero()) {
                continue;
            }
            Pointer env = jvmtiEnvs[i].env;
            // class name is passed as a char * not a JNI handle, sigh
            Pointer classNamePtr = Pointer.zero();
            if (className != null) {
                classNamePtr = CString.utf8FromJava(className);
            }

            newClassDataPtrPtr.setWord(Word.zero());
            invokeClassfileLoadHookCallback(callback, env, Word.zero(), JniHandles.createLocalHandle(classLoader),
                            classNamePtr,
                            JniHandles.createLocalHandle(protectionDomain), classfileBytesLength,
                            classDataPtr, newClassDataLenPtr, newClassDataPtrPtr);
            if (!classNamePtr.isZero()) {
                Memory.deallocate(classNamePtr);
            }
            if (!newClassDataPtrPtr.getWord().isZero()) {
                debug(null);
                classDataPtr = newClassDataPtrPtr.getWord().asPointer();
                classfileBytesLength = newClassDataLenPtr.getInt();
                changed = true;
            }
        }
        if (changed) {
            byte[] result = new byte[classfileBytesLength];
            Memory.readBytes(classDataPtr, result);
            return result;
        } else {
            return null;
        }
    }

    /**
     * Add a directory/jar to the boot classpath.
     * Only one addition per agent is supported.
     * Since this if typically called in the ONLOAD phase
     * we hold the value until Maxine has reached PRISTINE.
     */
    static int addToBootstrapClassLoaderSearch(Pointer env, Pointer segment) {
        // TODO Handle the case where paths are added after getAddedBootClassPath has been called,
        // i.e, during the LIVE phase
        if (getAddedBootClassPathCalled) {
            return JVMTI_ERROR_INTERNAL;
        }
        JVMTIEnv jvmtiEnv = JVMTI.getEnv(env);
        if (jvmtiEnv == null) {
            return JVMTI_ERROR_INVALID_ENVIRONMENT;
        }
        for (int i = 0; i < jvmtiEnv.bootClassPathAdd.length; i++) {
            long p = jvmtiEnv.bootClassPathAdd[i];
            if (p == 0) {
                Size length = CString.length(segment).plus(1);
                Pointer copiedPath = Memory.allocate(length);
                Memory.copyBytes(segment, copiedPath, length);
                jvmtiEnv.bootClassPathAdd[i] = copiedPath.asAddress().toLong();
                return JVMTI_ERROR_NONE;
            }
        }
        return JVMTI_ERROR_INTERNAL;
    }

    private static boolean getAddedBootClassPathCalled;

    /**
     * Handles any extra paths added in the ONLOAD phase.
     * @return
     */
    static String getAddedBootClassPath() {
        if (!JVMTI.anyActiveAgents()) {
            return null;
        }
        String result = null;
        for (int i = 0; i < jvmtiEnvs.length; i++) {
            JVMTIEnv jvmtiEnv = jvmtiEnvs[i];
            for (long pathAsLong : jvmtiEnv.bootClassPathAdd) {
                if (pathAsLong != 0) {
                    Pointer pathPtr = Address.fromLong(pathAsLong).asPointer();
                    try {
                        String path = CString.utf8ToJava(pathPtr);
                        if (result == null) {
                            result = path;
                        } else {
                            result = result + File.pathSeparator + path;
                        }
                    } catch (Utf8Exception ex) {
                        // skip it
                    }
                }
            }

        }
        getAddedBootClassPathCalled = true;
        return result;
    }

    static int getByteCodes(MethodID methodID, Pointer bytecodeCountPtr, Pointer bytecodesPtr) {
        try {
            ClassMethodActor classMethodActor = (ClassMethodActor) MethodID.toMethodActor(methodID);
            byte[] code = classMethodActor.code();
            bytecodeCountPtr.setInt(code.length);
            Pointer nativeBytesPtr = Memory.allocate(Size.fromInt(code.length));
            Memory.writeBytes(code, nativeBytesPtr);
            bytecodesPtr.setWord(nativeBytesPtr);
            return JVMTI_ERROR_NONE;
        } catch (ClassCastException ex) {
            return JVMTI_ERROR_INVALID_METHODID;
        }
    }

    /**
     * Get the classes whose loading was initiated by the given class loader.
     * TODO: Handle initiating versus defining loaders (which requires a Maxine upgrade).
     */
    static int getClassLoaderClasses(ClassLoader classLoader, Pointer classCountPtr, Pointer classesPtr) {
        if (classLoader == null) {
            classLoader = BootClassLoader.BOOT_CLASS_LOADER;
        }
        ClassRegistry classRegistry = ClassRegistry.makeRegistry(classLoader);
        Collection<ClassActor>  classActors = classRegistry.getClassActors();
        int classCount = classActors.size();
        Pointer classesArrayPtr = Memory.allocate(Size.fromInt(classCount * Word.size()));
        if (classesArrayPtr.isZero()) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }
        classesPtr.setWord(classesArrayPtr);
        classCountPtr.setInt(classCount);
        int i = 0;
        for (ClassActor classActor : classActors) {
            classesArrayPtr.setWord(i++, JniHandles.createLocalHandle(classActor.toJava()));
            assert i <= classCount;
        }
        return JVMTI_ERROR_NONE;
    }

    static int getSourceFileName(Class klass, Pointer sourceNamePtr) {
        ClassActor classActor = ClassActor.fromJava(klass);
        String className = classActor.sourceFileName;
        Pointer classNamePtr = CString.utf8FromJava(className);
        sourceNamePtr.setWord(classNamePtr);
        return JVMTI_ERROR_NONE;
    }
}
