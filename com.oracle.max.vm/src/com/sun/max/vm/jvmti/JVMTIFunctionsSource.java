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

import static com.sun.max.vm.jvmti.JVMTIEnvNativeStruct.*;
import static com.sun.max.vm.jvmti.JVMTIConstants.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.jvmti.JVMTI.Env;
import com.sun.max.vm.thread.*;


/**
 * The template for the implementations of the JVMTI functions.
 * This is transformed by {@link JVMTIFunctionsGenerator} into the actual
 * implementation in {@link JVMTIFunctions}. The transformation includes
 * phase checks, null {@link Pointer} checks, JNI handle type checks,
 * capability checks, MemberID checks, jvmtiEnv checks and exception handling.
 * Essentially the goal is to automate as much error checking as possible
 * so that the real implementations can just focus on the logic.
 *
 * A check on the validity of the environment and conversion from the {@link Pointer}
 * argument to a {@link JVMTI#Env} is indicated by:
 *
 *  // ENVCHECK
 *
 *  Note that the spec arguably requires this to be checked for every method.
 *  Currently the check is only made for methods that actually use the value.
 *
 *  Null {@link Pointer} checks are indicated by:
 *  // NULLCHECK: arg1,arg2,...
 *
 *  Phase checks are indicated by:
 *  // PHASES: phase1,phase2,...
 *
 *  JNI handle type checks are indicated by:
 *
 *  // HANDLECHECK: handleVar1=ClassName,...
 *
 *  or
 *
 *  // HANDLECHECK_NULLOK: handleVar1=ClassName,...
 *
 *  in the (unusual) case that a null handle is acceptable.
 *
 *  Capability checks are indicated by:
 *  // CAPABILITIES: cap1, cap2,...
 *
 *  MemberID checks are indicated by:
 *  // MEMBERID: var1=T1,var2=T2,...
 *  where Ti are one of Member,Method,Field and SomeActor is the appropriate {@link Actor} subclass
 *
 *  Generally, the method implementations are delegated to other classes, unless the
 *  implementation is trivial.
 */
@HOSTED_ONLY
@SuppressWarnings("null")
public class JVMTIFunctionsSource {
 // Checkstyle: stop method name check

    // These exist solely to avoid compilation errors in the this code. The transformed
    // code defines them as locals in the method as part of the above error checks
    private static final MethodActor methodActor = null;
    private static final ClassMethodActor classMethodActor = null;
    private static final FieldActor fieldActor = null;
    private static final Thread handleAsThread = null;
    private static final ThreadGroup handleAsThreadGroup = null;
    private static final Class<?> handleAsClass = null;
    private static final ClassLoader handleAsClassLoader = null;
    private static final ClassLoader handleAsObject = null;
    private static final Env jvmtiEnv = null;

    @VM_ENTRY_POINT
    private static native void reserved1();

    @VM_ENTRY_POINT
    private static int SetEventNotificationMode(Pointer env, int mode, int event_type, JniHandle event_thread) {
        // PHASES: ONLOAD,LIVE
        return JVMTIEvent.setEventNotificationMode(env, mode, event_type, event_thread);
    }

    @VM_ENTRY_POINT
    private static native void reserved3();

    @VM_ENTRY_POINT
    private static int GetAllThreads(Pointer env, Pointer threads_count_ptr, Pointer threads_ptr) {
        // PHASES: LIVE
        // NULLCHECK: threads_count_ptr,threads_ptr
        return JVMTIThreadFunctions.getAllThreads(threads_count_ptr, threads_ptr);
    }

    @VM_ENTRY_POINT
    private static int SuspendThread(Pointer env, JniHandle thread) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_SUSPEND
        // HANDLECHECK: thread=Thread
        return JVMTIThreadFunctions.suspendThread(handleAsThread);
    }

    @VM_ENTRY_POINT
    private static int ResumeThread(Pointer env, JniHandle thread) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_SUSPEND
        // HANDLECHECK: thread=Thread
        return JVMTIThreadFunctions.resumeThread(handleAsThread);
    }

    @VM_ENTRY_POINT
    private static int StopThread(Pointer env, JniHandle thread, JniHandle exception) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int InterruptThread(Pointer env, JniHandle thread) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_SIGNAL_THREAD
        // HANDLECHECK: thread=Thread
        return JVMTIThreadFunctions.interruptThread(handleAsThread);
    }

    @VM_ENTRY_POINT
    private static int GetThreadInfo(Pointer env, JniHandle thread, Pointer info_ptr) {
        // PHASES: LIVE
        // NULLCHECK: info_ptr
        // HANDLECHECK: thread=Thread
        return JVMTIThreadFunctions.getThreadInfo(handleAsThread, info_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetOwnedMonitorInfo(Pointer env, JniHandle thread, Pointer owned_monitor_count_ptr, Pointer owned_monitors_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetCurrentContendedMonitor(Pointer env, JniHandle thread, Pointer monitor_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int RunAgentThread(Pointer env, JniHandle jthread, Address proc, Pointer arg, int priority) {
        // PHASES: LIVE
        // NULLCHECK: proc
        return JVMTI.runAgentThread(env, jthread, proc, arg, priority);
    }

    @VM_ENTRY_POINT
    private static int GetTopThreadGroups(Pointer env, Pointer group_count_ptr, Pointer groups_ptr) {
        // PHASES: LIVE
        // NULLCHECK: group_count_ptr,groups_ptr
        return JVMTIThreadFunctions.getTopThreadGroups(group_count_ptr, groups_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetThreadGroupInfo(Pointer env, JniHandle group, Pointer info_ptr) {
        // PHASES: LIVE
        // HANDLECHECK: group=ThreadGroup
        // NULLCHECK: info_ptr
        return JVMTIThreadFunctions.getThreadGroupInfo(handleAsThreadGroup, info_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetThreadGroupChildren(Pointer env, JniHandle group, Pointer thread_count_ptr, Pointer threads_ptr, Pointer group_count_ptr, Pointer groups_ptr) {
        // PHASES: LIVE
        // NULLCHECK: thread_count_ptr,thread_count_ptr,group_count_ptr,groups_ptr
        // HANDLECHECK: group=ThreadGroup
        return JVMTIThreadFunctions.getThreadGroupChildren(handleAsThreadGroup, thread_count_ptr,
                        threads_ptr, group_count_ptr,  groups_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetFrameCount(Pointer env, JniHandle thread, Pointer count_ptr) {
        // PHASES: LIVE
        // NULLCHECK: count_ptr
        // HANDLECHECK: thread=Thread
        return JVMTIThreadFunctions.getFrameCount(handleAsThread, count_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetThreadState(Pointer env, JniHandle thread, Pointer thread_state_ptr) {
        // PHASES: LIVE
        // NULLCHECK: thread_state_ptr
        // HANDLECHECK: thread=Thread
        return JVMTIThreadFunctions.getThreadState(handleAsThread, thread_state_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetCurrentThread(Pointer env, Pointer thread_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: thread_ptr
        thread_ptr.setWord(JniHandles.createLocalHandle(VmThread.current().javaThread()));
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int GetFrameLocation(Pointer env, JniHandle thread, int depth, Pointer method_ptr, Pointer location_ptr) {
        // PHASES: LIVE
        // HANDLECHECK: thread=Thread
        // NULLCHECK: method_ptr, location_ptr
        return JVMTIThreadFunctions.getFrameLocation(handleAsThread, depth, method_ptr, location_ptr);
    }

    @VM_ENTRY_POINT
    private static int NotifyFramePop(Pointer env, JniHandle thread, int depth) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetLocalObject(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_ACCESS_LOCAL_VARIABLES
        // HANDLECHECK: thread=Thread
        // NULLCHECK: value_ptr
        return JVMTIThreadFunctions.getLocalValue(handleAsThread, depth, slot, value_ptr, 'L');
    }

    @VM_ENTRY_POINT
    private static int GetLocalInt(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_ACCESS_LOCAL_VARIABLES
        // HANDLECHECK: thread=Thread
        // NULLCHECK: value_ptr
        return JVMTIThreadFunctions.getLocalValue(handleAsThread, depth, slot, value_ptr, 'I');
    }

    @VM_ENTRY_POINT
    private static int GetLocalLong(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_ACCESS_LOCAL_VARIABLES
        // HANDLECHECK: thread=Thread
        // NULLCHECK: value_ptr
        return JVMTIThreadFunctions.getLocalValue(handleAsThread, depth, slot, value_ptr, 'J');
    }

    @VM_ENTRY_POINT
    private static int GetLocalFloat(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_ACCESS_LOCAL_VARIABLES
        // HANDLECHECK: thread=Thread
        // NULLCHECK: value_ptr
        return JVMTIThreadFunctions.getLocalValue(handleAsThread, depth, slot, value_ptr, 'F');
    }

    @VM_ENTRY_POINT
    private static int GetLocalDouble(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_ACCESS_LOCAL_VARIABLES
        // HANDLECHECK: thread=Thread
        // NULLCHECK: value_ptr
        return JVMTIThreadFunctions.getLocalValue(handleAsThread, depth, slot, value_ptr, 'D');
    }

    @VM_ENTRY_POINT
    private static int SetLocalObject(Pointer env, JniHandle thread, int depth, int slot, JniHandle value) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_ACCESS_LOCAL_VARIABLES
        // HANDLECHECK: thread=Thread
        return JVMTIThreadFunctions.setLocalObject(handleAsThread, depth, slot, value);
    }

    @VM_ENTRY_POINT
    private static int SetLocalInt(Pointer env, JniHandle thread, int depth, int slot, int value) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_ACCESS_LOCAL_VARIABLES
        // HANDLECHECK: thread=Thread
        return JVMTIThreadFunctions.setLocalInt(handleAsThread, depth, slot, value);
    }

    @VM_ENTRY_POINT
    private static int SetLocalLong(Pointer env, JniHandle thread, int depth, int slot, long value) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_ACCESS_LOCAL_VARIABLES
        // HANDLECHECK: thread=Thread
        return JVMTIThreadFunctions.setLocalLong(handleAsThread, depth, slot, value);
    }

    @VM_ENTRY_POINT
    private static int SetLocalFloat(Pointer env, JniHandle thread, int depth, int slot, float value) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_ACCESS_LOCAL_VARIABLES
        // HANDLECHECK: thread=Thread
        return JVMTIThreadFunctions.setLocalFloat(handleAsThread, depth, slot, value);
    }

    @VM_ENTRY_POINT
    private static int SetLocalDouble(Pointer env, JniHandle thread, int depth, int slot, double value) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_ACCESS_LOCAL_VARIABLES
        // HANDLECHECK: thread=Thread
        return JVMTIThreadFunctions.setLocalDouble(handleAsThread, depth, slot, value);
    }

    @VM_ENTRY_POINT
    private static int CreateRawMonitor(Pointer env, Pointer name, Pointer monitor_ptr) {
        // PHASES: ONLOAD,LIVE
        // NULLCHECK: name,monitor_ptr
        return JVMTIRawMonitor.create(name, monitor_ptr);
    }

    @VM_ENTRY_POINT
    private static int DestroyRawMonitor(Pointer env, Word rawMonitor) {
        // PHASES: ONLOAD,LIVE
        return JVMTIRawMonitor.destroy(rawMonitor);
    }

    @VM_ENTRY_POINT
    private static int RawMonitorEnter(Pointer env, Word rawMonitor) {
        // PHASES: ANY
        return JVMTIRawMonitor.enter(rawMonitor);
    }

    @VM_ENTRY_POINT
    private static int RawMonitorExit(Pointer env, Word rawMonitor) {
        // PHASES: ANY
        return JVMTIRawMonitor.exit(rawMonitor);
    }

    @VM_ENTRY_POINT
    private static int RawMonitorWait(Pointer env, Word rawMonitor, long millis) {
        // PHASES: ANY
        return JVMTIRawMonitor.wait(rawMonitor, millis);
    }

    @VM_ENTRY_POINT
    private static int RawMonitorNotify(Pointer env, Word rawMonitor) {
        // PHASES: ANY
        return JVMTIRawMonitor.notify(rawMonitor);
    }

    @VM_ENTRY_POINT
    private static int RawMonitorNotifyAll(Pointer env, Word rawMonitor) {
        // PHASES: ANY
        return JVMTIRawMonitor.notifyAll(rawMonitor);
    }

    @VM_ENTRY_POINT
    private static int SetBreakpoint(Pointer env, MethodID method, long location) {
        // PHASES: LIVE
        // MEMBERID: method=Class:Method
        return JVMTIBreakpoints.setBreakpoint(classMethodActor, method, location);
    }

    @VM_ENTRY_POINT
    private static int ClearBreakpoint(Pointer env, MethodID method, long location) {
        // PHASES: LIVE
        // MEMBERID: method=Class:Method
        return JVMTIBreakpoints.clearBreakpoint(classMethodActor, method, location);
    }

    @VM_ENTRY_POINT
    private static native void reserved40();

    @VM_ENTRY_POINT
    private static int SetFieldAccessWatch(Pointer env, JniHandle klass, FieldID field) {
        // PHASES: LIVE
        // HANDLECHECK: klass=Class
        // MEMBERID: field=Field
        // CAPABILITIES: CAN_GENERATE_FIELD_ACCESS_EVENTS
        return JVMTIFieldWatch.setAccessWatch(handleAsClass, fieldActor);
    }

    @VM_ENTRY_POINT
    private static int ClearFieldAccessWatch(Pointer env, JniHandle klass, FieldID field) {
        // PHASES: LIVE
        // HANDLECHECK: klass=Class
        // MEMBERID: field=Field
        // CAPABILITIES: CAN_GENERATE_FIELD_ACCESS_EVENTS
        return JVMTIFieldWatch.clearAccessWatch(handleAsClass, fieldActor);
    }

    @VM_ENTRY_POINT
    private static int SetFieldModificationWatch(Pointer env, JniHandle klass, FieldID field) {
        // PHASES: LIVE
        // HANDLECHECK: klass=Class
        // MEMBERID: field=Field
        // CAPABILITIES: CAN_GENERATE_FIELD_MODIFICATION_EVENTS
        return JVMTIFieldWatch.setModificationWatch(handleAsClass, fieldActor);
    }

    @VM_ENTRY_POINT
    private static int ClearFieldModificationWatch(Pointer env, JniHandle klass, FieldID field) {
        // PHASES: LIVE
        // HANDLECHECK: klass=Class
        // MEMBERID: field=Field
        // CAPABILITIES: CAN_GENERATE_FIELD_MODIFICATION_EVENTS
        return JVMTIFieldWatch.clearModificationWatch(handleAsClass, fieldActor);
    }

    @VM_ENTRY_POINT
    private static int IsModifiableClass(Pointer env, JniHandle klass, Pointer is_modifiable_class_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int Allocate(Pointer env, long size, Pointer mem_ptr) {
        // PHASES: ANY
        // NULLCHECK: mem_ptr
        if (size < 0) {
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        }
        if (size == 0) {
            mem_ptr.setWord(Word.zero());
        } else {
            Pointer mem = Memory.allocate(Size.fromLong(size));
            if (mem.isZero()) {
                return JVMTI_ERROR_OUT_OF_MEMORY;
            }
            mem_ptr.setWord(mem);
        }
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int Deallocate(Pointer env, Pointer mem) {
        // PHASES: ANY
        Memory.deallocate(mem);
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int GetClassSignature(Pointer env, JniHandle klass, Pointer signature_ptr, Pointer generic_ptr) {
        // PHASES: START,LIVE
        // HANDLECHECK: klass=Class
        return JVMTIClassFunctions.getClassSignature(handleAsClass, signature_ptr, generic_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetClassStatus(Pointer env, JniHandle klass, Pointer status_ptr) {
        // PHASES: START,LIVE
        // HANDLECHECK: klass=Class
        // NULLCHECK: status_ptr
        return JVMTIClassFunctions.getClassStatus(handleAsClass, status_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetSourceFileName(Pointer env, JniHandle klass, Pointer source_name_ptr) {
        // PHASES: START,LIVE
        // CAPABILITIES: CAN_GET_SOURCE_FILE_NAME
        // NULLCHECK: source_name_ptr
        // HANDLECHECK: klass=Class
        return JVMTIClassFunctions.getSourceFileName(handleAsClass, source_name_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetClassModifiers(Pointer env, JniHandle klass, Pointer modifiers_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: modifiers_ptr
        // HANDLECHECK: klass=Class
        modifiers_ptr.setInt(ClassActor.fromJava(handleAsClass).accessFlags());
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int GetClassMethods(Pointer env, JniHandle klass, Pointer method_count_ptr, Pointer methods_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: method_count_ptr,methods_ptr
        // HANDLECHECK: klass=Class
        return JVMTIClassFunctions.getClassMethods(handleAsClass, method_count_ptr, methods_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetClassFields(Pointer env, JniHandle klass, Pointer field_count_ptr, Pointer fields_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: field_count_ptr,fields_ptr
        // HANDLECHECK: klass=Class
        return JVMTIClassFunctions.getClassFields(handleAsClass, field_count_ptr, fields_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetImplementedInterfaces(Pointer env, JniHandle klass, Pointer interface_count_ptr, Pointer interfaces_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: interface_count_ptr,interfaces_ptr
        // HANDLECHECK: klass=Class
        return JVMTIClassFunctions.getImplementedInterfaces(handleAsClass, interface_count_ptr, interfaces_ptr);
    }

    @VM_ENTRY_POINT
    private static int IsInterface(Pointer env, JniHandle klass, Pointer is_interface_ptr) {
        // PHASES LIVE
        // NULLCHECK: is_interface_ptr
        // HANDLECHECK: klass=Class
        boolean is = ClassActor.isInterface(ClassActor.fromJava(handleAsClass).flags());
        is_interface_ptr.setBoolean(is);
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int IsArrayClass(Pointer env, JniHandle klass, Pointer is_array_class_ptr) {
        // PHASES LIVE
        // NULLCHECK: is_array_class_ptr
        // HANDLECHECK: klass=Class
        boolean is = ClassActor.fromJava(handleAsClass).isArrayClass();
        is_array_class_ptr.setBoolean(is);
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int GetClassLoader(Pointer env, JniHandle klass, Pointer classloader_ptr) {
        // PHASES START,LIVE
        // NULLCHECK: classloader_ptr
        // HANDLECHECK: klass=Class
        classloader_ptr.setWord(JniHandles.createLocalHandle(handleAsClass.getClassLoader()));
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int GetObjectHashCode(Pointer env, JniHandle object, Pointer hash_code_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetObjectMonitorUsage(Pointer env, JniHandle object, Pointer info_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetFieldName(Pointer env, JniHandle klass, FieldID field, Pointer name_ptr, Pointer signature_ptr, Pointer generic_ptr) {
        // PHASES: START,LIVE
        // HANDLECHECK: klass=Class
        // MEMBERID: field=Field
        return JVMTIClassFunctions.getFieldName(fieldActor, name_ptr, signature_ptr, generic_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetFieldDeclaringClass(Pointer env, JniHandle klass, FieldID field, Pointer declaring_class_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: declaring_class_ptr
        // MEMBERID: field=Field
        return JVMTIClassFunctions.getFieldDeclaringClass(fieldActor, declaring_class_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetFieldModifiers(Pointer env, JniHandle klass, FieldID field, Pointer modifiers_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: modifiers_ptr
        // MEMBERID: field=Field
        modifiers_ptr.setInt(fieldActor.flags());
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int IsFieldSynthetic(Pointer env, JniHandle klass, FieldID field, Pointer is_synthetic_ptr) {
        // PHASES: START,LIVE
        // CAPABILITIES: CAN_GET_SYNTHETIC_ATTRIBUTE
        // NULLCHECK: is_synthetic_ptr
        // MEMBERID: field=Field
        boolean result = (fieldActor.flags() & Actor.ACC_SYNTHETIC) != 0;
        is_synthetic_ptr.setBoolean(result);
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int GetMethodName(Pointer env, MethodID method, Pointer name_ptr, Pointer signature_ptr, Pointer generic_ptr) {
        // PHASES: START,LIVE
        // MEMBERID: method=Method
        return JVMTIClassFunctions.getMethodName(methodActor, name_ptr, signature_ptr, generic_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetMethodDeclaringClass(Pointer env, MethodID method, Pointer declaring_class_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: declaring_class_ptr
        // MEMBERID: method=Method
        return JVMTIClassFunctions.getMethodDeclaringClass(methodActor, declaring_class_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetMethodModifiers(Pointer env, MethodID method, Pointer modifiers_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: modifiers_ptr
        // MEMBERID: method=Method
        modifiers_ptr.setInt(methodActor.flags());
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static native void reserved67();

    @VM_ENTRY_POINT
    private static int GetMaxLocals(Pointer env, MethodID method, Pointer max_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: max_ptr
        // MEMBERID: method=Class:Method
        return JVMTIClassFunctions.getMaxLocals(classMethodActor, max_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetArgumentsSize(Pointer env, MethodID method, Pointer size_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: size_ptr
        // MEMBERID: method=Class:Method
        return JVMTIClassFunctions.getArgumentsSize(classMethodActor, size_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetLineNumberTable(Pointer env, MethodID method, Pointer entry_count_ptr, Pointer table_ptr) {
        // PHASES: START,LIVE
        // CAPABILITIES: CAN_GET_LINE_NUMBERS
        // NULLCHECK: entry_count_ptr,table_ptr
        // MEMBERID: method=Class:Method
        return JVMTIClassFunctions.getLineNumberTable(classMethodActor, entry_count_ptr, table_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetMethodLocation(Pointer env, MethodID method, Pointer start_location_ptr, Pointer end_location_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: start_location_ptr,end_location_ptr
        // MEMBERID: method=Class:Method
        return JVMTIClassFunctions.getMethodLocation(classMethodActor, start_location_ptr, end_location_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetLocalVariableTable(Pointer env, MethodID method, Pointer entry_count_ptr, Pointer table_ptr) {
        // PHASES: LIVE
        // NULLCHECK: entry_count_ptr, table_ptr
        // MEMBERID: method=Class:Method
        return JVMTIClassFunctions.getLocalVariableTable(classMethodActor, entry_count_ptr, table_ptr);
    }

    @VM_ENTRY_POINT
    private static int SetNativeMethodPrefix(Pointer env, Pointer prefix) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int SetNativeMethodPrefixes(Pointer env, int prefix_count, Pointer prefixes) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetBytecodes(Pointer env, MethodID method, Pointer bytecode_count_ptr, Pointer bytecodes_ptr) {
        // PHASES: START,LIVE
        // CAPABILITIES: CAN_GET_BYTECODES
        // NULLCHECK: bytecode_count_ptr,bytecodes_ptr
        // MEMBERID: method=Class:Method
        return JVMTIClassFunctions.getByteCodes(classMethodActor, bytecode_count_ptr, bytecodes_ptr);
    }

    @VM_ENTRY_POINT
    private static int IsMethodNative(Pointer env, MethodID method, Pointer is_native_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: is_native_ptr
        // MEMBERID: method=Method
        is_native_ptr.setBoolean(methodActor.isNative());
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int IsMethodSynthetic(Pointer env, MethodID method, Pointer is_synthetic_ptr) {
        // PHASES: START,LIVE
        // CAPABILITIES: CAN_GET_SYNTHETIC_ATTRIBUTE
        // NULLCHECK: is_synthetic_ptr
        // MEMBERID: method=Method
        boolean result = (methodActor.flags() & Actor.ACC_SYNTHETIC) != 0;
        is_synthetic_ptr.setBoolean(result);
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int GetLoadedClasses(Pointer env, Pointer class_count_ptr, Pointer classes_ptr) {
        // PHASES: LIVE
        // NULLCHECK: class_count_ptr,classes_ptr
        return JVMTIClassFunctions.getLoadedClasses(class_count_ptr, classes_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetClassLoaderClasses(Pointer env, JniHandle initiatingLoader, Pointer class_count_ptr, Pointer classes_ptr) {
        // PHASES: LIVE
        // NULLCHECK: class_count_ptr,classes_ptr
        // HANDLECHECK: initiatingLoader=ClassLoader
        return JVMTIClassFunctions.getClassLoaderClasses(handleAsClassLoader, class_count_ptr, classes_ptr);
    }

    @VM_ENTRY_POINT
    private static int PopFrame(Pointer env, JniHandle thread) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnObject(Pointer env, JniHandle thread, JniHandle value) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnInt(Pointer env, JniHandle thread, int value) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnLong(Pointer env, JniHandle thread, long value) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnFloat(Pointer env, JniHandle thread, float value) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnDouble(Pointer env, JniHandle thread, double value) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnVoid(Pointer env, JniHandle thread) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int RedefineClasses(Pointer env, int class_count, Pointer class_definitions) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetVersionNumber(Pointer env, Pointer version_ptr) {
        // PHASES: ANY
        // NULLCHECK: version_ptr
        version_ptr.setInt(JVMTI_VERSION);
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int GetCapabilities(Pointer env, Pointer capabilities_ptr) {
        // PHASES: ANY
        // NULLCHECK: capabilities_ptr
        capabilities_ptr.setLong(0, CAPABILITIES.getPtr(env).readLong(0));
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int GetSourceDebugExtension(Pointer env, JniHandle klass, Pointer source_debug_extension_ptr) {
        // PHASES: START,LIVE
        // CAPABILITIES: CAN_GET_SOURCE_DEBUG_EXTENSION
        // NULLCHECK: source_debug_extension_ptr
        // HANDLECHECK: klass=Class
        return JVMTIClassFunctions.getSourceDebugExtension(handleAsClass, source_debug_extension_ptr);
    }

    @VM_ENTRY_POINT
    private static int IsMethodObsolete(Pointer env, MethodID method, Pointer is_obsolete_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int SuspendThreadList(Pointer env, int request_count, Pointer request_list, Pointer results) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_SUSPEND
        // NULLCHECK: request_list,results
        if (request_count < 0) {
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        }
        return JVMTIThreadFunctions.suspendThreadList(request_count, request_list, results);
    }

    @VM_ENTRY_POINT
    private static int ResumeThreadList(Pointer env, int request_count, Pointer request_list, Pointer results) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_SUSPEND
        // NULLCHECK: request_list,results
        if (request_count < 0) {
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        }
        return JVMTIThreadFunctions.resumeThreadList(request_count, request_list, results);
    }

    @VM_ENTRY_POINT
    private static native void reserved94();

    @VM_ENTRY_POINT
    private static native void reserved95();

    @VM_ENTRY_POINT
    private static native void reserved96();

    @VM_ENTRY_POINT
    private static native void reserved97();

    @VM_ENTRY_POINT
    private static native void reserved98();

    @VM_ENTRY_POINT
    private static native void reserved99();

    @VM_ENTRY_POINT
    private static int GetAllStackTraces(Pointer env, int max_frame_count, Pointer stack_info_ptr, Pointer thread_count_ptr) {
        // PHASES: LIVE
        // NULLCHECK: stack_info_ptr,thread_count_ptr
        if (max_frame_count < 0) {
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        }
        return JVMTIThreadFunctions.getAllStackTraces(max_frame_count, stack_info_ptr, thread_count_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetThreadListStackTraces(Pointer env, int thread_count, Pointer thread_list, int max_frame_count, Pointer stack_info_ptr) {
        // PHASES: LIVE
        // NULLCHECK: thread_list,stack_info_ptr
        if (thread_count < 0 || max_frame_count < 0) {
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        }
        return JVMTIThreadFunctions.getThreadListStackTraces(thread_count, thread_list, max_frame_count, stack_info_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetThreadLocalStorage(Pointer env, JniHandle thread, Pointer data_ptr) {
        // PHASES: START,LIVE
        // HANDLECHECK: thread=Thread
        // NULLCHECK: data_ptr
        return JVMTIThreadLocalStorage.getThreadLocalStorage(handleAsThread, data_ptr);
    }

    @VM_ENTRY_POINT
    private static int SetThreadLocalStorage(Pointer env, JniHandle thread, Pointer data) {
        // PHASES: START,LIVE
        // HANDLECHECK: thread=Thread
        return JVMTIThreadLocalStorage.setThreadLocalStorage(handleAsThread, data);
    }

    @VM_ENTRY_POINT
    private static int GetStackTrace(Pointer env, JniHandle thread, int start_depth, int max_frame_count, Pointer frame_buffer, Pointer count_ptr) {
        // PHASES: LIVE
        // NULLCHECK: frame_buffer,count_ptr
        // HANDLECHECK: thread=Thread
        if (max_frame_count < 0) {
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        }
        return JVMTIThreadFunctions.getStackTrace(handleAsThread, start_depth, max_frame_count, frame_buffer, count_ptr);
    }

    @VM_ENTRY_POINT
    private static native void reserved105();

    @VM_ENTRY_POINT
    private static int GetTag(Pointer env, JniHandle object, Pointer tag_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: tag_ptr
        // ENVCHECK
        return jvmtiEnv.tags.getTag(object.unhand(), tag_ptr);
    }

    @VM_ENTRY_POINT
    private static int SetTag(Pointer env, JniHandle object, long tag) {
        // PHASES: START,LIVE
        // ENVCHECK
        return jvmtiEnv.tags.setTag(object.unhand(), tag);
    }

    @VM_ENTRY_POINT
    private static int ForceGarbageCollection(Pointer env) {
        System.gc();
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int IterateOverObjectsReachableFromObject(Pointer env, JniHandle object, Address object_reference_callback, Pointer user_data) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int IterateOverReachableObjects(Pointer env, Address heap_root_callback, Address stack_ref_callback, Address object_ref_callback, Pointer user_data) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int IterateOverHeap(Pointer env, int object_filter, Address heap_object_callback, Pointer user_data) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int IterateOverInstancesOfClass(Pointer env, JniHandle klass, int object_filter, Address heap_object_callback, Pointer user_data) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static native void reserved113();

    @VM_ENTRY_POINT
    private static int GetObjectsWithTags(Pointer env, int tag_count, Pointer tags, Pointer count_ptr, Pointer object_result_ptr, Pointer tag_result_ptr) {
        // CAPABILITIES: CAN_TAG_OBJECTS
        // NULLCHECK: tags,count_ptr
        return JVMTI.getEnv(env).tags.getObjectsWithTags(tag_count, tags, count_ptr, object_result_ptr, tag_result_ptr);
    }

    @VM_ENTRY_POINT
    private static int FollowReferences(Pointer env, int heap_filter, JniHandle klass, JniHandle initial_object, Pointer callbacks, Pointer user_data) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int IterateThroughHeap(Pointer env, int heap_filter, JniHandle klass, Pointer callbacks, Pointer user_data) {
        // PHASES: LIVE
        // CAPABILITIES: CAN_TAG_OBJECTS
        // NULLCHECK: callbacks
        // HANDLECHECK_NULLOK: klass=Class
        // ENVCHECK
        return JVMTIHeapFunctions.iterateThroughHeap(jvmtiEnv, heap_filter, handleAsClass, callbacks, user_data);
    }

    @VM_ENTRY_POINT
    private static native void reserved117();

    @VM_ENTRY_POINT
    private static native void reserved118();

    @VM_ENTRY_POINT
    private static native void reserved119();

    @VM_ENTRY_POINT
    private static int SetJNIFunctionTable(Pointer env, Pointer function_table) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetJNIFunctionTable(Pointer env, Pointer function_table) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int SetEventCallbacks(Pointer env, Pointer callbacks, int size_of_callbacks) {
        // PHASES: ONLOAD,LIVE
        Pointer envCallbacks = CALLBACKS.get(env).asPointer();
        Memory.copyBytes(callbacks, envCallbacks, Size.fromInt(size_of_callbacks));
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int GenerateEvents(Pointer env, int event_type) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetExtensionFunctions(Pointer env, Pointer extension_count_ptr, Pointer extensions) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetExtensionEvents(Pointer env, Pointer extension_count_ptr, Pointer extensions) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int SetExtensionEventCallback(Pointer env, int extension_event_index, Address callback) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int DisposeEnvironment(Pointer env) {
        // PHASES: ANY
        return JVMTI.disposeEnv(env);
    }

    @VM_ENTRY_POINT
    private static int GetErrorName(Pointer env, int error, Pointer name_ptr) {
        // PHASES: ANY
        // NULLCHECK: name_ptr
        if (error < 0 || error > JVMTI_ERROR_MAX) {
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        }
        byte[] nameBytes = JVMTIError.nameBytes[error];
        Pointer cstring = Memory.allocate(Size.fromInt(nameBytes.length + 1));
        CString.writeBytes(nameBytes, 0, nameBytes.length, cstring, nameBytes.length + 1);
        name_ptr.setWord(0, cstring);
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int GetlongFormat(Pointer env, Pointer format_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetSystemProperties(Pointer env, Pointer count_ptr, Pointer property_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetSystemProperty(Pointer env, Pointer property, Pointer value_ptr) {
        // PHASES: ONLOAD,LIVE
        // NULLCHECK: property,value_ptr
        return JVMTI.getSystemProperty(env, property, value_ptr);
    }

    @VM_ENTRY_POINT
    private static int SetSystemProperty(Pointer env, Pointer property, Pointer value) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetPhase(Pointer env, Pointer phase_ptr) {
        // PHASES: ANY
        // NULLCHECK: phase_ptr
        return JVMTI.getPhase(phase_ptr);
    }

    @VM_ENTRY_POINT
    private static int GetCurrentThreadCpuTimerInfo(Pointer env, Pointer info_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetCurrentThreadCpuTime(Pointer env, Pointer nanos_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetThreadCpuTimerInfo(Pointer env, Pointer info_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetThreadCpuTime(Pointer env, JniHandle thread, Pointer nanos_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetTimerInfo(Pointer env, Pointer info_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetTime(Pointer env, Pointer nanos_ptr) {
        // PHASES: ANY
        // NULLCHECK: nanos_ptr
        nanos_ptr.setLong(System.nanoTime());
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int GetPotentialCapabilities(Pointer env, Pointer capabilities_ptr) {
        // PHASES: ONLOAD,LIVE
        // NULLCHECK: capabilities_ptr
        // Currently we don't have any phase-limited or ownership limitations
        JVMTICapabilities.setAll(capabilities_ptr);
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static native void reserved141();

    @VM_ENTRY_POINT
    private static int AddCapabilities(Pointer env, Pointer capabilities_ptr) {
        // PHASES: ONLOAD,LIVE
        // NULLCHECK: capabilities_ptr
        Pointer envCaps = CAPABILITIES.getPtr(env);
        for (int i = 0; i < JVMTICapabilities.values.length; i++) {
            JVMTICapabilities cap = JVMTICapabilities.values[i];
            if (cap.get(capabilities_ptr)) {
                if (cap.can) {
                    cap.set(envCaps, true);
                } else {
                    JVMTI.debug(cap);
                    return JVMTI_ERROR_NOT_AVAILABLE; // TODO
                }
            }
        }
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int RelinquishCapabilities(Pointer env, Pointer capabilities_ptr) {
        // PHASES: ONLOAD,LIVE
        // NULLCHECK: capabilities_ptr
        Pointer envCaps = CAPABILITIES.getPtr(env);
        for (int i = 0; i < JVMTICapabilities.values.length; i++) {
            JVMTICapabilities cap = JVMTICapabilities.values[i];
            if (cap.get(capabilities_ptr)) {
               cap.set(envCaps, false);
            }
        }
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int GetAvailableProcessors(Pointer env, Pointer processor_count_ptr) {
        // PHASES: ANY
        // NULLCHECK: processor_count_ptr
        processor_count_ptr.setInt(Runtime.getRuntime().availableProcessors());
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int GetClassVersionNumbers(Pointer env, JniHandle klass, Pointer minor_version_ptr, Pointer major_version_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: minor_version_ptr, minor_version_ptr
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetConstantPool(Pointer env, JniHandle klass, Pointer constant_pool_count_ptr, Pointer constant_pool_byte_count_ptr, Pointer constant_pool_bytes_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetEnvironmentLocalStorage(Pointer env, Pointer data_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int SetEnvironmentLocalStorage(Pointer env, Pointer data) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int AddToBootstrapClassLoaderSearch(Pointer env, Pointer segment) {
        // PHASES ONLOAD,LIVE
        // NULLCHECK: segment
        return JVMTIClassFunctions.addToBootstrapClassLoaderSearch(env, segment);
    }

    @VM_ENTRY_POINT
    private static int SetVerboseFlag(Pointer env, int flag, boolean value) {
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

    @VM_ENTRY_POINT
    private static int AddToSystemClassLoaderSearch(Pointer env, Pointer segment) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int RetransformClasses(Pointer env, int class_count, Pointer classes) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetOwnedMonitorStackDepthInfo(Pointer env, JniHandle thread, Pointer monitor_info_count_ptr, Pointer monitor_info_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
    }

    @VM_ENTRY_POINT
    private static int GetObjectSize(Pointer env, JniHandle object, Pointer size_ptr) {
        // PHASES: START,LIVE
        // NULLCHECK: size_ptr
        return JVMTIClassFunctions.getObjectSize(object.unhand(), size_ptr);
    }

    /**
     * This function is an extension and appears in the extended JVMTI interface table,
     * as that is a convenient way to invoke it from native code. It's purpose is
     * simply to record the value of the C struct that denotes the JVMTI environment.
     */
    @VM_ENTRY_POINT
    private static int SetJVMTIEnv(Pointer env) {
        JVMTI.setJVMTIEnv(env);
        return 0;
    }

}
