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
import static com.sun.max.vm.jvmti.JVMTIUtil.ClassActorProxy;

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
import com.sun.max.vm.jni.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * Support for all the JVMTI functions related to {@link Class} handling.
 */
class JVMTIClassFunctions {

    static {
        VMOptions.addFieldOption("-XX:", "JVMTIIncludeVMClasses", "Include VM classes in JVMTI results.");
    }

    static boolean JVMTIIncludeVMClasses;

    private static final String COM_SUN_MAX = "com/sun/max";
    private static final String COM_ORACLE_MAX = "com/oracle/max";

    static boolean isVmClass(ClassActor classActor) {
        // TODO use TBD fast/accurate check
        final String name = classActor.name();
        return  name.contains(COM_SUN_MAX) || name.contains(COM_ORACLE_MAX) || name.contains("$INVOKE");
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
            invokeClassfileLoadHookCallback(callback, env, Word.zero(), JniHandles.createLocalHandle(classLoader), classNamePtr, JniHandles.createLocalHandle(protectionDomain), classfileBytesLength,
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
     * Add a directory/jar to the boot classpath. Only one addition per agent is supported. Since this if typically
     * called in the ONLOAD phase we hold the value until Maxine has reached PRISTINE.
     */
    static int addToBootstrapClassLoaderSearch(Pointer env, Pointer segment) {
        // TODO Handle the case where paths are added after getAddedBootClassPath has been called,
        // i.e, during the LIVE phase
        if (getAddedBootClassPathCalled) {
            return JVMTI_ERROR_INTERNAL;
        }
        Env jvmtiEnv = JVMTI.getEnv(env);
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
        if (!JVMTI.anyActiveAgents()) {
            return null;
        }
        String result = null;
        for (int i = 0; i < jvmtiEnvs.length; i++) {
            Env jvmtiEnv = jvmtiEnvs[i];
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

    static int getClassStatus(Class klass, Pointer statusPtr) {
        ClassActor classActor = ClassActor.fromJava(klass);
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
        statusPtr.setInt(status);
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
     * Get the classes whose loading was initiated by the given class loader. TODO: Handle initiating versus defining
     * loaders (which requires a Maxine upgrade).
     */
    static int getClassLoaderClasses(ClassLoader classLoader, Pointer classCountPtr, Pointer classesPtrPtr) {
        if (classLoader == null) {
            classLoader = BootClassLoader.BOOT_CLASS_LOADER;
        }
        Collection<ClassActor> classActors = ClassRegistry.makeRegistry(classLoader).getClassActors();
        int classCount = classActors.size();
        Pointer classesPtr = allocateClassesArray(classCount, classCountPtr, classesPtrPtr);
        if (classesPtr.isZero()) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }
        copyClassActors(classesPtr, classActors, 0, classCount);
        classCountPtr.setInt(classCount);
        return JVMTI_ERROR_NONE;
    }

    private static final int NUMBER_OF_PRIMITIVE_CLASS_ACTORS = 9; // void,byte,boolean,short,char,int,long,float,double

    static int getLoadedClasses(Pointer classCountPtr, Pointer classesPtrPtr) {
        // TODO handle all class loaders, requires changes to Maxine
        Collection<ClassActor> bootClassActors = ClassRegistry.makeRegistry(BootClassLoader.BOOT_CLASS_LOADER).getClassActors();
        Collection<ClassActor> systemClassActors = ClassRegistry.makeRegistry(ClassLoader.getSystemClassLoader()).getClassActors();
        // N.B. This is inherently a snapshot as we don't prevent class loading/unloading happening while this executes.
        // These variables define the snapshot, and copyClassActors honors these values for recording purposes.
        // Another complication is that spec sates that primitive class actors are not returned
        // and Maxine's boot class registry does include those.
        int bootClassActorsSize = bootClassActorsSize(bootClassActors) - NUMBER_OF_PRIMITIVE_CLASS_ACTORS;
        int systemClassActorsSize = systemClassActors.size();
        int totalSize = bootClassActorsSize + systemClassActorsSize;
        Pointer classesPtr = allocateClassesArray(totalSize, classCountPtr, classesPtrPtr);
        if (classesPtr.isZero()) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }
        int bootClassActorsCopied = copyClassActors(classesPtr, bootClassActors, 0, bootClassActorsSize);
        int systemClassActorsCopied = copyClassActors(classesPtr, systemClassActors, bootClassActorsCopied, systemClassActorsSize);
        classCountPtr.setInt(bootClassActorsCopied + systemClassActorsCopied);
        return JVMTI_ERROR_NONE;
    }

    private static int bootClassActorsSize(Collection<ClassActor> bootClassActors) {
        if (JVMTIIncludeVMClasses) {
            return bootClassActors.size();
        } else {
            int count = 0;
            for (ClassActor classActor : bootClassActors) {
                if (isVmClass(classActor)) {
                    continue;
                }
                count++;
            }
            return count;
        }
    }

    private static Pointer allocateClassesArray(int classCount, Pointer classCountPtr, Pointer classesPtrPtr) {
        Pointer classesPtr = Memory.allocate(Size.fromInt(classCount * Word.size()));
        if (classesPtr.isZero()) {
            return classesPtr;
        }
        classesPtrPtr.setWord(classesPtr);
        return classesPtr;
    }

    /**
     * Copies at most {@code count} classes into the C array {@code classesArrayPtr}. Returns the actual number of
     * classes copied. This maybe {@code <= count} the number of class actors has shrunk due to unloading. The added
     * classes are stored starting at {@code arrayIndex}.
     */
    private static int copyClassActors(Pointer classesArrayPtr, Collection<ClassActor> classActors, int arrayIndex, int count) {
        int index = 0;
        for (ClassActor classActor : classActors) {
            if (classActor.isPrimitiveClassActor()) {
                continue;
            }
            if (!JVMTIIncludeVMClasses && isVmClass(classActor)) {
                continue;
            }
            if (index < count) {
                classesArrayPtr.setWord(arrayIndex + index, JniHandles.createLocalHandle(classActor.toJava()));
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

    static int getLineNumberTable(ClassMethodActor classMethodActor, Pointer entryCountPtr, Pointer tablePtr) {
        if (classMethodActor.isNative()) {
            return JVMTI_ERROR_NATIVE_METHOD;
        }
        LineNumberTable table = classMethodActor.codeAttribute().lineNumberTable();
        if (table.isEmpty()) {
            return JVMTI_ERROR_ABSENT_INFORMATION;
        }
        LineNumberTable.Entry[] entries = table.entries();
        entryCountPtr.setInt(entries.length);
        Pointer nativeTablePtr = Memory.allocate(Size.fromInt(entries.length * getLineNumberEntrySize()));
        for (int i = 0; i < entries.length; i++) {
            LineNumberTable.Entry entry = entries[i];
            setJVMTILineNumberEntry(nativeTablePtr, i, entry.bci(), entry.lineNumber());
        }
        tablePtr.setWord(nativeTablePtr);
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

    static int getLocalVariableTable(ClassMethodActor classMethodActor, Pointer entryCountPtr, Pointer tablePtr) {
        if (classMethodActor.isNative()) {
            return JVMTI_ERROR_NATIVE_METHOD;
        }
        LocalVariableTable table = classMethodActor.codeAttribute().localVariableTable();
        if (table.isEmpty()) {
            return JVMTI_ERROR_ABSENT_INFORMATION;
        }
        LocalVariableTable.Entry[] entries = table.entries();
        ConstantPool constantPool = classMethodActor.holder().constantPool();
        entryCountPtr.setInt(entries.length);
        Pointer nativeTablePtr = Memory.allocate(Size.fromInt(entries.length * getLocalVariableEntrySize()));
        for (int i = 0; i < entries.length; i++) {
            LocalVariableTable.Entry entry = entries[i];
            setJVMTILocalVariableEntry(nativeTablePtr, i, CString.utf8FromJava(entry.name(constantPool).string),
                            CString.utf8FromJava(constantPool.utf8At(entry.descriptorIndex(), "local variable type").toString()),
                            entry.signatureIndex() == 0 ? Pointer.zero() : CString.utf8FromJava(entry.signature(constantPool).string), entry.startBCI(), entry.length(), entry.slot());
        }
        tablePtr.setWord(nativeTablePtr);
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
        startLocationPtr.setInt(0);
        endLocationPtr.setInt(code.length - 1);
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

    static int getClassMethods(Class klass, Pointer methodCountPtr, Pointer methodsPtrPtr) {
        ClassActor classActor = ClassActor.fromJava(klass);
        MethodActor[] methodActors = ClassActorProxy.asClassActorProxy(classActor).methodActors;
        boolean ordered = methodActors != null;
        if (!ordered) {
            methodActors = classActor.getLocalMethodActorsArray();
        }
        int length = methodActors.length;
        Pointer methodsPtr = Memory.allocate(Size.fromInt(length * Word.size()));
        if (methodsPtr.isZero()) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }
        methodsPtrPtr.setWord(methodsPtr);
        methodCountPtr.setInt(length);
        // One issue: Maxine puts clinit at the end, it should be first (based on Hotspot).
        int last = length;
        int offset = 0;
        if (ordered) {
            if (classActor.clinit != null) {
                assert methodActors[last - 1] == classActor.clinit;
                methodsPtr.setWord(0, MethodID.fromMethodActor(classActor.clinit));
                last--;
                offset++;
            }
        }
        for (int i = 0; i < last; i++) {
            methodsPtr.setWord(i + offset, MethodID.fromMethodActor(methodActors[i]));
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
