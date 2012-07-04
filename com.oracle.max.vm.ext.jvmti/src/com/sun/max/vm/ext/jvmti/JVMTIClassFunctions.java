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

import static com.sun.max.vm.ext.jvmti.JVMTI.*;
import static com.sun.max.vm.ext.jvmti.JVMTICallbacks.*;
import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;
import static com.sun.max.vm.ext.jvmti.JVMTIUtil.*;

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
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.ext.jvmti.JJVMTI.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * Support for all the JVMTI functions related to {@link Class} handling.
 */
class JVMTIClassFunctions {

    /**
     * Strict check on whether a class is a VM class.
     * @param classActor
     * @return
     */
    static boolean isVMClass(ClassActor classActor) {
        return classActor.classLoader == VMClassLoader.VM_CLASS_LOADER;
    }

    /**
     * Non-strict check whether a class is a VM class.
     *
     * @param classActor
     * @return If {@link JVMTI#JVMTI_VM} is {@code false} equivalent to {@code !isVMClass(classActor), otherwise {@link true}.
     */
    static boolean isVisibleClass(ClassActor classActor) {
        return JVMTI.JVMTI_VM || !isVMClass(classActor);
    }

    static int getObjectSize(Object object, Pointer sizePtr) {
        sizePtr.setInt(Layout.size(Reference.fromJava(object)).toInt());
        return JVMTI_ERROR_NONE;
    }

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
        Pointer newClassDataLenPtr = Intrinsics.alloca(Pointer.size(), false);
        Pointer newClassDataPtrPtr = Intrinsics.alloca(Pointer.size(), false);
        Pointer classDataPtr = Reference.fromJava(classfileBytes).toOrigin().plus(JVMTIUtil.byteDataOffset);
        int classfileBytesLength = classfileBytes.length;
        boolean changed = false;
        for (int i = 0; i < MAX_NATIVE_ENVS; i++) {
            NativeEnv nativEnv = (NativeEnv) jvmtiEnvs[i];
            Pointer callback = getCallbackForEvent(nativEnv, JVMTIEvent.CLASS_FILE_LOAD_HOOK, VmThread.current());
            if (callback.isZero()) {
                continue;
            }
            Pointer cstruct = nativEnv.cstruct;
            // class name is passed as a char * not a JNI handle, sigh
            Pointer classNamePtr = Pointer.zero();
            if (className != null) {
                classNamePtr = CString.utf8FromJava(className);
            }

            newClassDataPtrPtr.setWord(Word.zero());
            invokeClassfileLoadHookCallback(
                            callback, cstruct, Word.zero(),
                            JniHandles.createLocalHandle(classLoader), classNamePtr, JniHandles.createLocalHandle(protectionDomain), classfileBytesLength,
                            classDataPtr, newClassDataLenPtr, newClassDataPtrPtr);
            if (!classNamePtr.isZero()) {
                Memory.deallocate(classNamePtr);
            }
            if (!newClassDataPtrPtr.getWord().isZero()) {
                classDataPtr = newClassDataPtrPtr.getWord().asPointer();
                classfileBytesLength = newClassDataLenPtr.getInt();
                changed = true;
            }
        }
        if (changed) {
            classfileBytes = new byte[classfileBytesLength];
            Memory.readBytes(classDataPtr, classfileBytes);
        }

        // now the Java agents
        for (int i = MAX_NATIVE_ENVS; i < MAX_ENVS; i++) {
            JavaEnv javaEnv = (JavaEnv) jvmtiEnvs[i];
            if (javaEnv == null || !JVMTIEvent.isEventSet(javaEnv, JVMTIEvent.CLASS_FILE_LOAD_HOOK, VmThread.current())) {
                continue;
            }
            byte[] newClassfileBytes = javaEnv.callbackHandler.classFileLoadHook(classLoader, className, protectionDomain, classfileBytes);
            if (newClassfileBytes != null) {
                classfileBytes = newClassfileBytes;
                changed = true;
            }
        }
        if (changed) {
            return classfileBytes;
        } else {
            return null;
        }
    }

    /**
     * Add a directory/jar to the boot classpath. Only one addition per agent is supported. Since this if typically
     * called in the ONLOAD phase we hold the value until Maxine has reached PRISTINE.
     */
    static int addToBootstrapClassLoaderSearch(Pointer env, Pointer segment) {
        // TODO Handle the case where paths are added after getAddedBootClassPath has been called,
        // i.e, during the LIVE phase
        if (getAddedBootClassPathCalled) {
            return JVMTI_ERROR_INTERNAL;
        }
        NativeEnv jvmtiEnv = (NativeEnv) JVMTI.getEnv(env);
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
     *
     * @return
     */
    static String getAddedBootClassPath() {
        if (JVMTI.activeEnvCount == 0) {
            return null;
        }
        String result = null;
        for (int i = 0; i < MAX_NATIVE_ENVS; i++) {
            NativeEnv jvmtiEnv = (NativeEnv) jvmtiEnvs[i];
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

    static int getClassSignature(Class klass, Pointer signaturePtrPtr, Pointer genericPtrPtr) {
        ClassActor classActor = ClassActor.fromJava(klass);
        if (!signaturePtrPtr.isZero()) {
            Pointer signaturePtr = CString.utf8FromJava(classActor.typeDescriptor.string);
            if (signaturePtr.isZero()) {
                return JVMTI_ERROR_OUT_OF_MEMORY;
            }
            signaturePtrPtr.setWord(signaturePtr);
        }
        if (!genericPtrPtr.isZero()) {
            genericPtrPtr.setWord(Pointer.zero());
        }
        return JVMTI_ERROR_NONE;
    }

    static int getClassStatus(ClassActor classActor) {
        int status = 0;
        if (classActor.isArrayClass()) {
            status = JVMTI_CLASS_STATUS_ARRAY;
        } else if (classActor.isPrimitiveClassActor()) {
            status = JVMTI_CLASS_STATUS_PRIMITIVE;
        } else {
            // ClassActor keeps a single state for the class, so we have to work backwards.
            ClassActorProxy proxyClassActor = ClassActorProxy.asClassActorProxy(classActor);
            if (proxyClassActor.initializationState == ClassActorProxy.INITIALIZED) {
                status = JVMTI_CLASS_STATUS_VERIFIED | JVMTI_CLASS_STATUS_PREPARED | JVMTI_CLASS_STATUS_INITIALIZED;
            } else if (proxyClassActor.initializationState == ClassActorProxy.VERIFIED_) {
                status = JVMTI_CLASS_STATUS_VERIFIED | JVMTI_CLASS_STATUS_PREPARED;
            } else if (proxyClassActor.initializationState == ClassActorProxy.PREPARED) {
                status = JVMTI_CLASS_STATUS_PREPARED;
            } else {
                // any other value implies some kind of error
                status = JVMTI_CLASS_STATUS_ERROR;
            }
        }
        return status;
    }

    static int getClassStatus(Class<?> klass) {
        return getClassStatus(ClassActor.fromJava(klass));
    }

    static int getClassStatus(Class<?> klass, Pointer statusPtr) {
        statusPtr.setInt(getClassStatus(klass));
        return JVMTI_ERROR_NONE;
    }

    static int getByteCodes(ClassMethodActor classMethodActor, Pointer bytecodeCountPtr, Pointer bytecodesPtr) {
        byte[] code = classMethodActor.code();
        bytecodeCountPtr.setInt(code.length);
        Pointer nativeBytesPtr = Memory.allocate(Size.fromInt(code.length));
        Memory.writeBytes(code, nativeBytesPtr);
        bytecodesPtr.setWord(nativeBytesPtr);
        return JVMTI_ERROR_NONE;
    }

    /**
     * Union class to handle native/java variants for {@link #getLoadedClasses} etc.
     */
    private static class LoadedClassesUnion extends ModeUnion {
        ClassActor[] classActorArray;
        Pointer classesArrayPtr;
        int classCount;

        LoadedClassesUnion(boolean isNative) {
            super(isNative);
        }
    }

    /**
     * Get the classes whose loading was initiated by the given class loader. TODO: Handle initiating versus defining
     * loaders (which requires a Maxine upgrade).
     */
    static ClassActor[] getClassLoaderClasses(ClassLoader classLoader) {
        LoadedClassesUnion lcu = new LoadedClassesUnion(false);
        getClassLoaderClasses(lcu, classLoader);
        return lcu.classActorArray;
    }

    static int getClassLoaderClasses(ClassLoader classLoader, Pointer classCountPtr, Pointer classesPtrPtr) {
        LoadedClassesUnion lcu = new LoadedClassesUnion(true);
        int error = getClassLoaderClasses(lcu, classLoader);
        if (error == JVMTI_ERROR_NONE) {
            classCountPtr.setInt(lcu.classCount);
            classesPtrPtr.setWord(lcu.classesArrayPtr);
        }
        return error;
    }
    static int getClassLoaderClasses(LoadedClassesUnion lcu, ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = BootClassLoader.BOOT_CLASS_LOADER;
        }
        Collection<ClassActor> classActors = ClassRegistry.makeRegistry(classLoader).getClassActors();
        int classCount = classActors.size();
        if (lcu.isNative) {
            lcu.classesArrayPtr = allocateClassesArray(classCount);
            if (lcu.classesArrayPtr.isZero()) {
                return JVMTI_ERROR_OUT_OF_MEMORY;
            }
        } else {
            lcu.classActorArray = new ClassActor[classCount];
        }
        copyClassActors(lcu, classActors, 0, classCount);
        lcu.classCount = classCount;
        return JVMTI_ERROR_NONE;
    }

    private static final int NUMBER_OF_PRIMITIVE_CLASS_ACTORS = 9; // void,byte,boolean,short,char,int,long,float,double

    static int getLoadedClasses(Pointer classCountPtr, Pointer classesPtrPtr) {
        LoadedClassesUnion lcu = new LoadedClassesUnion(true);
        int error = getLoadedClasses(lcu);
        if (error == JVMTI_ERROR_NONE) {
            classCountPtr.setInt(lcu.classCount);
            classesPtrPtr.setWord(lcu.classesArrayPtr);
        }
        return error;
    }

    static ClassActor[] getLoadedClassActors() {
        LoadedClassesUnion lcu = new LoadedClassesUnion(false);
        getLoadedClasses(lcu);
        return lcu.classActorArray;

    }

    private static int getLoadedClasses(LoadedClassesUnion lcu) {
        // TODO handle all class loaders, requires changes to Maxine
        Collection<ClassActor> bootClassActors = ClassRegistry.makeRegistry(BootClassLoader.BOOT_CLASS_LOADER).getClassActors();
        Collection<ClassActor> systemClassActors = ClassRegistry.makeRegistry(ClassLoader.getSystemClassLoader()).getClassActors();
        @SuppressWarnings("unchecked")
        Collection<ClassActor> vmClassActors = JVMTI.JVMTI_VM ? ClassRegistry.makeRegistry(VMClassLoader.VM_CLASS_LOADER).getClassActors() : Collections.EMPTY_SET;
        // N.B. This is inherently a snapshot as we don't prevent class loading/unloading happening while this executes.
        // These variables define the snapshot, and copyClassActors honors these values for recording purposes.
        // Another complication is that spec sates that primitive class actors are not returned
        // and Maxine's boot class registry does include those.
        int bootClassActorsSize = bootClassActors.size() - NUMBER_OF_PRIMITIVE_CLASS_ACTORS;
        int systemClassActorsSize = systemClassActors.size();
        int totalSize = bootClassActorsSize + systemClassActorsSize + vmClassActors.size();
        if (lcu.isNative) {
            lcu.classesArrayPtr = allocateClassesArray(totalSize);
            if (lcu.classesArrayPtr.isZero()) {
                return JVMTI_ERROR_OUT_OF_MEMORY;
            }
        } else {
            lcu.classActorArray = new ClassActor[totalSize];
        }
        int bootClassActorsCopied = copyClassActors(lcu, bootClassActors, 0, bootClassActorsSize);
        int systemClassActorsCopied = copyClassActors(lcu, systemClassActors, bootClassActorsCopied, systemClassActorsSize);
        int vmClassActorsCopied = JVMTI.JVMTI_VM ? copyClassActors(lcu, vmClassActors, systemClassActorsCopied + bootClassActorsCopied, vmClassActors.size()) : 0;

        lcu.classCount = bootClassActorsCopied + systemClassActorsCopied + vmClassActorsCopied;
        return JVMTI_ERROR_NONE;
    }

    private static Pointer allocateClassesArray(int classCount) {
        Pointer classesPtr = Memory.allocate(Size.fromInt(classCount * Word.size()));
        if (classesPtr.isZero()) {
            return classesPtr;
        }
        return classesPtr;
    }

    /**
     * Copies at most {@code count} classes into a C array or Java array. Returns the actual number of
     * classes copied. This maybe {@code <= count} the number of class actors has shrunk due to unloading. The added
     * classes are stored starting at {@code arrayIndex}.
     */
    private static int copyClassActors(LoadedClassesUnion lcu, Collection<ClassActor> classActors, int arrayIndex, int count) {
        int index = 0;

        for (ClassActor classActor : classActors) {
            if (classActor.isPrimitiveClassActor()) {
                continue;
            }
            if (index < count) {
                Class<?> klass = classActor.toJava();
                if (lcu.isNative) {
                    lcu.classesArrayPtr.setWord(arrayIndex + index, JniHandles.createLocalHandle(klass));
                } else {
                    lcu.classActorArray[arrayIndex + index] = classActor;
                }
                index++;
            } else {
                break;
            }
        }
        return index;
    }

    static int getSourceFileName(Class klass, Pointer sourceNamePtr) {
        ClassActor classActor = ClassActor.fromJava(klass);
        String className = classActor.sourceFileName;
        Pointer classNamePtr = CString.utf8FromJava(className);
        sourceNamePtr.setWord(classNamePtr);
        return JVMTI_ERROR_NONE;
    }

    private static class LineNumberTableUnion extends ModeUnion {
        // NATIVE
        Pointer nativeTablePtr;
        int count;
        // JAVA
        LineNumberEntry[] lineNumberEntries;

        LineNumberTableUnion(boolean isNative) {
            super(isNative);
        }
    }

    static LineNumberEntry[] getLineNumberTable(ClassMethodActor method) {
        LineNumberTableUnion lnu = new LineNumberTableUnion(ModeUnion.JAVA);
        int error = getLineNumberTable(method, lnu);
        if (error == JVMTI_ERROR_NONE) {
            return lnu.lineNumberEntries;
        } else {
            throw new JJVMTI.JJVMTIException(error);
        }
    }

    static int getLineNumberTable(ClassMethodActor classMethodActor, Pointer entryCountPtr, Pointer tablePtr) {
        LineNumberTableUnion lnu = new LineNumberTableUnion(ModeUnion.NATIVE);
        int error = getLineNumberTable(classMethodActor, lnu);
        if (error == JVMTI_ERROR_NONE) {
            entryCountPtr.setInt(lnu.count);
            tablePtr.setWord(lnu.nativeTablePtr);
        }
        return error;
    }

    static int getLineNumberTable(ClassMethodActor classMethodActor, LineNumberTableUnion lnu) {
        if (classMethodActor.isNative()) {
            return JVMTI_ERROR_NATIVE_METHOD;
        }
        LineNumberTable table = classMethodActor.codeAttribute().lineNumberTable();
        if (table.isEmpty()) {
            return JVMTI_ERROR_ABSENT_INFORMATION;
        }
        LineNumberTable.Entry[] entries = table.entries();
        lnu.count = entries.length;
        if (lnu.isNative) {
            lnu.nativeTablePtr = Memory.allocate(Size.fromInt(entries.length * getLineNumberEntrySize()));
        } else {
            lnu.lineNumberEntries = new LineNumberEntry[entries.length];
        }
        for (int i = 0; i < entries.length; i++) {
            LineNumberTable.Entry entry = entries[i];
            if (lnu.isNative) {
                setJVMTILineNumberEntry(lnu.nativeTablePtr, i, entry.bci(), entry.lineNumber());
            } else {
                lnu.lineNumberEntries[i] = new LineNumberEntry(entry.bci(), entry.lineNumber());
            }
        }
        return JVMTI_ERROR_NONE;
    }

    private static int lineNumberEntrySize = -1;

    private static int getLineNumberEntrySize() {
        if (lineNumberEntrySize < 0) {
            lineNumberEntrySize = getJVMTILineNumberEntrySize();
        }
        return lineNumberEntrySize;
    }

    @C_FUNCTION
    private static native int getJVMTILineNumberEntrySize();

    @C_FUNCTION
    private static native void setJVMTILineNumberEntry(Pointer table, int index, long location, int lineNumber);

    /**
     * To order local variable entries by slot index.
     */
    private static class EntryComparator implements Comparator<LocalVariableTable.Entry> {
        public int compare(LocalVariableTable.Entry a, LocalVariableTable.Entry b) {
            final int aSlot = a.slot();
            final int bSlot = b.slot();
            if (aSlot < bSlot) {
                return -1;
            } else if (aSlot > bSlot) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private static final EntryComparator entryComparator = new EntryComparator();

    private static class LocalVariableTableUnion extends ModeUnion {
        int entryCount;
        Pointer nativeTablePtr;
        JJVMTI.LocalVariableEntry[] localVariableEntryArray;
        LocalVariableTableUnion(boolean isNative) {
            super(isNative);
        }
    }

    static int getLocalVariableTable(ClassMethodActor classMethodActor, Pointer entryCountPtr, Pointer tablePtr) {
        LocalVariableTableUnion lvtu = new LocalVariableTableUnion(true);
        int error = getLocalVariableTable(lvtu, classMethodActor);
        if (error == JVMTI_ERROR_NONE) {
            entryCountPtr.setInt(lvtu.entryCount);
            tablePtr.setWord(lvtu.nativeTablePtr);
        }
        return error;
    }

    static JJVMTI.LocalVariableEntry[] getLocalVariableTable(ClassMethodActor classMethodActor) {
        LocalVariableTableUnion lvtu = new LocalVariableTableUnion(false);
        int error = getLocalVariableTable(lvtu, classMethodActor);
        if (error == JVMTI_ERROR_NONE) {
            return lvtu.localVariableEntryArray;
        } else {
            if (error == JVMTI_ERROR_ABSENT_INFORMATION) {
                return new JJVMTI.LocalVariableEntry[0];
            }
            throw new JJVMTI.JJVMTIException(error);
        }
    }

    static int getLocalVariableTable(LocalVariableTableUnion lvtu, ClassMethodActor classMethodActor) {
        if (classMethodActor.isNative()) {
            return JVMTI_ERROR_NATIVE_METHOD;
        }
        LocalVariableTable table = classMethodActor.codeAttribute().localVariableTable();
        // Maxine does not (currently) distinguish a class file with no LocalVariableTableAttribute
        // and one with an empty table.
        if (table.isEmpty()) {
            return JVMTI_ERROR_ABSENT_INFORMATION;
        }
        LocalVariableTable.Entry[] entries = table.entries();
        // The spec doesn't say anything about ordering but, experimentally, it is important to order by slot
        // otherwise debuggers show the arguments to a method in the random order returned by table.entries().
        Arrays.sort(entries, entryComparator);
        ConstantPool constantPool = classMethodActor.holder().constantPool();
        lvtu.entryCount = entries.length;
        if (lvtu.isNative) {
            lvtu.nativeTablePtr = Memory.allocate(Size.fromInt(entries.length * getLocalVariableEntrySize()));
            for (int i = 0; i < entries.length; i++) {
                LocalVariableTable.Entry entry = entries[i];
                setJVMTILocalVariableEntry(lvtu.nativeTablePtr, i, CString.utf8FromJava(entry.name(constantPool).string),
                                CString.utf8FromJava(constantPool.utf8At(entry.descriptorIndex(), "local variable type").toString()),
                                entry.signatureIndex() == 0 ? Pointer.zero() : CString.utf8FromJava(entry.signature(constantPool).string), entry.startBCI(), entry.length(), entry.slot());
            }
        } else {
            lvtu.localVariableEntryArray = new JJVMTI.LocalVariableEntry[entries.length];
            for (int i = 0; i < entries.length; i++) {
                LocalVariableTable.Entry entry = entries[i];
                lvtu.localVariableEntryArray[i] = new JJVMTI.LocalVariableEntry(entry.startBCI(), entry.length(),
                                entry.name(constantPool).string, constantPool.utf8At(entry.descriptorIndex(), "local variable type").toString(),
                                entry.signatureIndex() == 0 ? null : entry.signature(constantPool).string, entry.slot());
            }
        }
        return JVMTI_ERROR_NONE;
    }

    private static int localVariableEntrySize = -1;

    private static int getLocalVariableEntrySize() {
        if (localVariableEntrySize < 0) {
            localVariableEntrySize = getJVMTILocalVariableEntrySize();
        }
        return localVariableEntrySize;
    }

    @C_FUNCTION
    private static native int getJVMTILocalVariableEntrySize();

    @C_FUNCTION
    private static native void setJVMTILocalVariableEntry(Pointer table, int index, Pointer name, Pointer signature, Pointer genericSignature, long location, int length, int slot);

    static int getSourceDebugExtension(Class klass, Pointer sourceDebugExtensionPtr) {
        return JVMTI_ERROR_ABSENT_INFORMATION;
    }

    static int isMethodObsolete(ClassMethodActor classMethodActor, Pointer isObsoletePtr) {
        // TODO implement properly!
        isObsoletePtr.setBoolean(false);
        return JVMTI_ERROR_NONE;
    }

    static int getMethodName(MethodActor methodActor, Pointer namePtrPtr, Pointer signaturePtrPtr, Pointer genericPtrPtr) {
        if (!namePtrPtr.isZero()) {
            namePtrPtr.setWord(CString.utf8FromJava(methodActor.name()));
        }
        if (!signaturePtrPtr.isZero()) {
            signaturePtrPtr.setWord(CString.utf8FromJava(methodActor.descriptor().asString()));
        }
        if (!genericPtrPtr.isZero()) {
            String generic = methodActor.genericSignatureString();
            genericPtrPtr.setWord(generic == null ? Pointer.zero() : CString.utf8FromJava(generic));
        }
        return JVMTI_ERROR_NONE;
    }

    static int getMethodDeclaringClass(MethodActor methodActor, Pointer declaringClassPtr) {
        declaringClassPtr.setWord(JniHandles.createLocalHandle(methodActor.holder().toJava()));
        return JVMTI_ERROR_NONE;
    }

    static int getMaxLocals(ClassMethodActor classMethodActor, Pointer maxPtr) {
        if (classMethodActor.isNative()) {
            return JVMTI_ERROR_INVALID_METHODID;
        }
        maxPtr.setInt(classMethodActor.codeAttribute().maxLocals);
        return JVMTI_ERROR_NONE;
    }

    static int getArgumentsSize(ClassMethodActor classMethodActor, Pointer sizePtr) {
        if (classMethodActor.isNative()) {
            return JVMTI_ERROR_INVALID_METHODID;
        }
        sizePtr.setInt(classMethodActor.numberOfParameterSlots());
        return JVMTI_ERROR_NONE;
    }

    static int getMethodLocation(ClassMethodActor classMethodActor, Pointer startLocationPtr, Pointer endLocationPtr) {
        if (classMethodActor.isNative()) {
            return JVMTI_ERROR_INVALID_METHODID;
        }
        byte[] code = classMethodActor.codeAttribute().code();
        startLocationPtr.setLong(0);
        endLocationPtr.setLong(code.length - 1);
        return JVMTI_ERROR_NONE;
    }

    static int getFieldName(FieldActor fieldActor, Pointer namePtrPtr, Pointer signaturePtrPtr, Pointer genericPtrPtr) {
        if (!namePtrPtr.isZero()) {
            namePtrPtr.setWord(CString.utf8FromJava(fieldActor.name()));
        }
        if (!signaturePtrPtr.isZero()) {
            signaturePtrPtr.setWord(CString.utf8FromJava(fieldActor.descriptor().string));
        }
        if (!genericPtrPtr.isZero()) {
            String generic = fieldActor.genericSignatureString();
            genericPtrPtr.setWord(generic == null ? Pointer.zero() : CString.utf8FromJava(generic));
        }
        return JVMTI_ERROR_NONE;
    }

    static int getFieldDeclaringClass(FieldActor fieldActor, Pointer declaringClassPtr) {
        declaringClassPtr.setWord(JniHandles.createLocalHandle(fieldActor.holder().toJava()));
        return JVMTI_ERROR_NONE;
    }

    private static class ClassMethodsUnion extends ModeUnion {
        int length;
        Pointer methodsPtr;
        MethodActor[] methodsArray;

        ClassMethodsUnion(boolean isNative) {
            super(isNative);
        }
    }

    static MethodActor[] getClassMethods(ClassActor klass) {
        ClassMethodsUnion cmu = new ClassMethodsUnion(false);
        getClassMethods(klass, cmu);
        return cmu.methodsArray;
    }

    static int getClassMethods(Class klass, Pointer methodCountPtr, Pointer methodsPtrPtr) {
        ClassMethodsUnion cmu = new ClassMethodsUnion(true);
        int error = getClassMethods(ClassActor.fromJava(klass), cmu);
        methodsPtrPtr.setWord(cmu.methodsPtr);
        methodCountPtr.setInt(cmu.length);
        return error;
    }

    private static int getClassMethods(ClassActor classActor, ClassMethodsUnion cmu) {
        MethodActor[] methodActors = ClassActorProxy.asClassActorProxy(classActor).methodActors;
        boolean ordered = methodActors != null;
        if (!ordered) {
            methodActors = classActor.getLocalMethodActorsArray();
        }
        int length = methodActors.length;

        if (cmu.isNative) {
            cmu.methodsPtr = Memory.allocate(Size.fromInt(length * Word.size()));
            if (cmu.methodsPtr.isZero()) {
                return JVMTI_ERROR_OUT_OF_MEMORY;
            }
        } else {
            cmu.methodsArray = new MethodActor[length];
        }
        cmu.length = length;

        // One issue: Maxine puts clinit at the end, it should be first (based on Hotspot).
        int last = length;
        int offset = 0;
        if (ordered) {
            if (classActor.clinit != null) {
                assert methodActors[last - 1] == classActor.clinit;
                if (cmu.isNative) {
                    cmu.methodsPtr.setWord(0, MethodID.fromMethodActor(classActor.clinit));
                } else {
                    cmu.methodsArray[0] = classActor.clinit;
                }
                last--;
                offset++;
            }
        }
        for (int i = 0; i < last; i++) {
            if (cmu.isNative) {
                cmu.methodsPtr.setWord(i + offset, MethodID.fromMethodActor(methodActors[i]));
            } else {
                cmu.methodsArray[i + offset] = methodActors[i];
            }
        }
        return JVMTI_ERROR_NONE;
    }

    static int getClassFields(Class klass, Pointer fieldCountPtr, Pointer fieldsPtrPtr) {
        List<FieldActor> fieldActors = ClassActor.fromJava(klass).getLocalFieldActors();
        Pointer fieldsPtr = Memory.allocate(Size.fromInt(fieldActors.size() * Word.size()));
        if (fieldsPtr.isZero()) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }
        fieldsPtrPtr.setWord(fieldsPtr);
        int size = fieldActors.size();
        for (int i = 0; i < size; i++) {
            fieldsPtr.setWord(i, FieldID.fromFieldActor(fieldActors.get(i)));
        }
        fieldCountPtr.setInt(size);
        return JVMTI_ERROR_NONE;
    }

    static int getImplementedInterfaces(Class klass, Pointer interfaceCountPtr, Pointer interfacesPtrPtr) {
        List<InterfaceActor> interfaceActors = ClassActor.fromJava(klass).getLocalInterfaceActors();
        Pointer interfacesPtr = Memory.allocate(Size.fromInt(interfaceActors.size() * Word.size()));
        if (interfacesPtr.isZero()) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }
        interfacesPtrPtr.setWord(interfacesPtr);
        int size = interfaceActors.size();
        for (int i = 0; i < size; i++) {
            interfacesPtr.setWord(i, JniHandles.createLocalHandle(interfaceActors.get(i).toJava()));
        }
        interfaceCountPtr.setInt(size);
        return JVMTI_ERROR_NONE;

    }

}
