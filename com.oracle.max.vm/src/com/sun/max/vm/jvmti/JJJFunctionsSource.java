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

import static com.sun.max.vm.jvmti.JJJEnvImplFields.*;
import static com.sun.max.vm.jvmti.JJJConstants.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.thread.*;


// Checkstyle: stop method name check
@HOSTED_ONLY
public class JJJFunctionsSource {
    @VM_ENTRY_POINT
    private static native void reserved1();

    @VM_ENTRY_POINT
    private static int SetEventNotificationMode(Pointer env, int mode, int event_type, JniHandle event_thread) {
        // PHASES: ONLOAD,LIVE
        if (event_thread.isZero()) {
            long envMask = EVENTMASK.get(env).asAddress().toLong();
            long maskBit = getEventBitMask(event_type);
            if (maskBit < 0) {
                return JVMTI_ERROR_INVALID_EVENT_TYPE;
            }
            if (mode == JVMTI_ENABLE) {
                envMask = envMask | maskBit;
            } else if (mode == JVMTI_DISABLE) {
                envMask = envMask & ~maskBit;
            } else {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            EVENTMASK.set(env, Address.fromLong(envMask));
            return JVMTI_ERROR_NONE;
        } else {
            // TODO handle per-thread events
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved3();

    @VM_ENTRY_POINT
    private static int GetAllThreads(Pointer env, Pointer threads_count_ptr, Pointer threads_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SuspendThread(Pointer env, JniHandle thread) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int ResumeThread(Pointer env, JniHandle thread) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int StopThread(Pointer env, JniHandle thread, JniHandle exception) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int InterruptThread(Pointer env, JniHandle thread) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetThreadInfo(Pointer env, JniHandle thread, Pointer info_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetOwnedMonitorInfo(Pointer env, JniHandle thread, Pointer owned_monitor_count_ptr, Pointer owned_monitors_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetCurrentContendedMonitor(Pointer env, JniHandle thread, Pointer monitor_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int RunAgentThread(Pointer env, JniHandle jthread, Address proc, Pointer arg, int priority) {
        // PHASES: LIVE
        // NULLCHECK: proc
        return JVMTI.runAgentThread(env, jthread, proc, arg, priority);
    }

    @VM_ENTRY_POINT
    private static int GetTopThreadGroups(Pointer env, Pointer group_count_ptr, Pointer groups_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetThreadGroupInfo(Pointer env, JniHandle group, Pointer info_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetThreadGroupChildren(Pointer env, JniHandle group, Pointer thread_count_ptr, Pointer threads_ptr, Pointer group_count_ptr, Pointer groups_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetFrameCount(Pointer env, JniHandle thread, Pointer count_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetThreadState(Pointer env, JniHandle thread, Pointer thread_state_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
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
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int NotifyFramePop(Pointer env, JniHandle thread, int depth) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetLocalObject(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetLocalInt(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetLocalLong(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetLocalFloat(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetLocalDouble(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SetLocalObject(Pointer env, JniHandle thread, int depth, int slot, JniHandle value) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SetLocalInt(Pointer env, JniHandle thread, int depth, int slot, int value) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SetLocalLong(Pointer env, JniHandle thread, int depth, int slot, long value) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SetLocalFloat(Pointer env, JniHandle thread, int depth, int slot, float value) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SetLocalDouble(Pointer env, JniHandle thread, int depth, int slot, double value) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int CreateRawMonitor(Pointer env, Pointer name, Pointer monitor_ptr) {
        // PHASES: ONLOAD,LIVE
        // NULLCHECK: name,monitor_ptr
        return JJJRawMonitor.create(name, monitor_ptr);
    }

    @VM_ENTRY_POINT
    private static int DestroyRawMonitor(Pointer env, MonitorID rawMonitor) {
        // PHASES: ONLOAD,LIVE
        return JJJRawMonitor.destroy(rawMonitor);
    }

    @VM_ENTRY_POINT
    private static int RawMonitorEnter(Pointer env, MonitorID rawMonitor) {
        // PHASES: ANY
        return JJJRawMonitor.enter(rawMonitor);
    }

    @VM_ENTRY_POINT
    private static int RawMonitorExit(Pointer env, MonitorID rawMonitor) {
        // PHASES: ANY
        return JJJRawMonitor.exit(rawMonitor);
    }

    @VM_ENTRY_POINT
    private static int RawMonitorWait(Pointer env, MonitorID rawMonitor, long millis) {
        // PHASES: ANY
        return JJJRawMonitor.wait(rawMonitor, millis);
    }

    @VM_ENTRY_POINT
    private static int RawMonitorNotify(Pointer env, MonitorID rawMonitor) {
        // PHASES: ANY
        return JJJRawMonitor.notify(rawMonitor);
    }

    @VM_ENTRY_POINT
    private static int RawMonitorNotifyAll(Pointer env, MonitorID rawMonitor) {
        // PHASES: ANY
        return JJJRawMonitor.notifyAll(rawMonitor);
    }

    @VM_ENTRY_POINT
    private static int SetBreakpoint(Pointer env, MethodID method, long location) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int ClearBreakpoint(Pointer env, MethodID method, long location) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static native void reserved40();

    @VM_ENTRY_POINT
    private static int SetFieldAccessWatch(Pointer env, JniHandle klass, FieldID field) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int ClearFieldAccessWatch(Pointer env, JniHandle klass, FieldID field) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SetFieldModificationWatch(Pointer env, JniHandle klass, FieldID field) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int ClearFieldModificationWatch(Pointer env, JniHandle klass, FieldID field) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int IsModifiableClass(Pointer env, JniHandle klass, Pointer is_modifiable_class_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int Allocate(Pointer env, long size, Pointer mem_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int Deallocate(Pointer env, Pointer mem) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetClassSignature(Pointer env, JniHandle klass, Pointer signature_ptr, Pointer generic_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetClassStatus(Pointer env, JniHandle klass, Pointer status_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetSourceFileName(Pointer env, JniHandle klass, Pointer source_name_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetClassModifiers(Pointer env, JniHandle klass, Pointer modifiers_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetClassMethods(Pointer env, JniHandle klass, Pointer method_count_ptr, Pointer methods_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetClassFields(Pointer env, JniHandle klass, Pointer field_count_ptr, Pointer fields_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetImplementedInterfaces(Pointer env, JniHandle klass, Pointer interface_count_ptr, Pointer interfaces_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int IsInterface(Pointer env, JniHandle klass, Pointer is_interface_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int IsArrayClass(Pointer env, JniHandle klass, Pointer is_array_class_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetClassLoader(Pointer env, JniHandle klass, Pointer classloader_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetObjectHashCode(Pointer env, JniHandle object, Pointer hash_code_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetObjectMonitorUsage(Pointer env, JniHandle object, Pointer info_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetFieldName(Pointer env, JniHandle klass, FieldID field, Pointer name_ptr, Pointer signature_ptr, Pointer generic_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetFieldDeclaringClass(Pointer env, JniHandle klass, FieldID field, Pointer declaring_class_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetFieldModifiers(Pointer env, JniHandle klass, FieldID field, Pointer modifiers_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int IsFieldSynthetic(Pointer env, JniHandle klass, FieldID field, Pointer is_synthetic_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetMethodName(Pointer env, MethodID method, Pointer name_ptr, Pointer signature_ptr, Pointer generic_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetMethodDeclaringClass(Pointer env, MethodID method, Pointer declaring_class_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetMethodModifiers(Pointer env, MethodID method, Pointer modifiers_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static native void reserved67();

    @VM_ENTRY_POINT
    private static int GetMaxLocals(Pointer env, MethodID method, Pointer max_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetArgumentsSize(Pointer env, MethodID method, Pointer size_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetLineNumberTable(Pointer env, MethodID method, Pointer entry_count_ptr, Pointer table_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetMethodLocation(Pointer env, MethodID method, Pointer start_location_ptr, Pointer end_location_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetLocalVariableTable(Pointer env, MethodID method, Pointer entry_count_ptr, Pointer table_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SetNativeMethodPrefix(Pointer env, Pointer prefix) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SetNativeMethodPrefixes(Pointer env, int prefix_count, Pointer prefixes) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetBytecodes(Pointer env, MethodID method, Pointer bytecode_count_ptr, Pointer bytecodes_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int IsMethodNative(Pointer env, MethodID method, Pointer is_native_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int IsMethodSynthetic(Pointer env, MethodID method, Pointer is_synthetic_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetLoadedClasses(Pointer env, Pointer class_count_ptr, Pointer classes_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetClassLoaderClasses(Pointer env, JniHandle initiating_loader, Pointer class_count_ptr, Pointer classes_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int PopFrame(Pointer env, JniHandle thread) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnObject(Pointer env, JniHandle thread, JniHandle value) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnInt(Pointer env, JniHandle thread, int value) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnLong(Pointer env, JniHandle thread, long value) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnFloat(Pointer env, JniHandle thread, float value) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnDouble(Pointer env, JniHandle thread, double value) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnVoid(Pointer env, JniHandle thread) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int RedefineClasses(Pointer env, int class_count, Pointer class_definitions) {
        return JVMTI_ERROR_NOT_AVAILABLE;
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
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int IsMethodObsolete(Pointer env, MethodID method, Pointer is_obsolete_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SuspendThreadList(Pointer env, int request_count, Pointer request_list, Pointer results) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int ResumeThreadList(Pointer env, int request_count, Pointer request_list, Pointer results) {
        return JVMTI_ERROR_NOT_AVAILABLE;
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
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetThreadListStackTraces(Pointer env, int thread_count, Pointer thread_list, int max_frame_count, Pointer stack_info_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetThreadLocalStorage(Pointer env, JniHandle thread, Pointer data_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SetThreadLocalStorage(Pointer env, JniHandle thread, Pointer data) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetStackTrace(Pointer env, JniHandle thread, int start_depth, int max_frame_count, Pointer frame_buffer, Pointer count_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static native void reserved105();

    @VM_ENTRY_POINT
    private static int GetTag(Pointer env, JniHandle object, Pointer tag_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SetTag(Pointer env, JniHandle object, long tag) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int ForceGarbageCollection(Pointer env) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int IterateOverObjectsReachableFromObject(Pointer env, JniHandle object, Address object_reference_callback, Pointer user_data) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int IterateOverReachableObjects(Pointer env, Address heap_root_callback, Address stack_ref_callback, Address object_ref_callback, Pointer user_data) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int IterateOverHeap(Pointer env, int object_filter, Address heap_object_callback, Pointer user_data) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int IterateOverInstancesOfClass(Pointer env, JniHandle klass, int object_filter, Address heap_object_callback, Pointer user_data) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static native void reserved113();

    @VM_ENTRY_POINT
    private static int GetObjectsWithTags(Pointer env, int tag_count, Pointer tags, Pointer count_ptr, Pointer object_result_ptr, Pointer tag_result_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int FollowReferences(Pointer env, int heap_filter, JniHandle klass, JniHandle initial_object, Pointer callbacks, Pointer user_data) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int IterateThroughHeap(Pointer env, int heap_filter, JniHandle klass, Pointer callbacks, Pointer user_data) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static native void reserved117();

    @VM_ENTRY_POINT
    private static native void reserved118();

    @VM_ENTRY_POINT
    private static native void reserved119();

    @VM_ENTRY_POINT
    private static int SetJNIFunctionTable(Pointer env, Pointer function_table) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetJNIFunctionTable(Pointer env, Pointer function_table) {
        return JVMTI_ERROR_NOT_AVAILABLE;
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
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetExtensionFunctions(Pointer env, Pointer extension_count_ptr, Pointer extensions) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetExtensionEvents(Pointer env, Pointer extension_count_ptr, Pointer extensions) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SetExtensionEventCallback(Pointer env, int extension_event_index, Address callback) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int DisposeEnvironment(Pointer env) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetErrorName(Pointer env, int error, Pointer name_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetlongFormat(Pointer env, Pointer format_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetSystemProperties(Pointer env, Pointer count_ptr, Pointer property_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetSystemProperty(Pointer env, Pointer property, Pointer value_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SetSystemProperty(Pointer env, Pointer property, Pointer value) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetPhase(Pointer env, Pointer phase_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetCurrentThreadCpuTimerInfo(Pointer env, Pointer info_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetCurrentThreadCpuTime(Pointer env, Pointer nanos_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetThreadCpuTimerInfo(Pointer env, Pointer info_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetThreadCpuTime(Pointer env, JniHandle thread, Pointer nanos_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetTimerInfo(Pointer env, Pointer info_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetTime(Pointer env, Pointer nanos_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetPotentialCapabilities(Pointer env, Pointer capabilities_ptr) {
        // PHASES: ONLOAD,LIVE
        // NULLCHECK: capabilities_ptr
        // Currently we don't have any phase-limited or ownership limitations
        JJJCapabilities.setAll(capabilities_ptr);
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static native void reserved141();

    @VM_ENTRY_POINT
    private static int AddCapabilities(Pointer env, Pointer capabilities_ptr) {
        // PHASES: ONLOAD,LIVE
        // NULLCHECK: capabilities_ptr
        Pointer envCaps = CAPABILITIES.getPtr(env);
        for (int i = 0; i < JJJCapabilities.values.length; i++) {
            JJJCapabilities cap = JJJCapabilities.values[i];
            if (cap.get(capabilities_ptr)) {
                if (cap.can) {
                    cap.set(envCaps, true);
                } else {
                    return JVMTI_ERROR_NOT_AVAILABLE;
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
        for (int i = 0; i < JJJCapabilities.values.length; i++) {
            JJJCapabilities cap = JJJCapabilities.values[i];
            if (cap.get(capabilities_ptr)) {
               cap.set(envCaps, false);
            }
        }
        return JVMTI_ERROR_NONE;
    }

    @VM_ENTRY_POINT
    private static int GetAvailableProcessors(Pointer env, Pointer processor_count_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetClassVersionNumbers(Pointer env, JniHandle klass, Pointer minor_version_ptr, Pointer major_version_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetConstantPool(Pointer env, JniHandle klass, Pointer constant_pool_count_ptr, Pointer constant_pool_byte_count_ptr, Pointer constant_pool_bytes_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetEnvironmentLocalStorage(Pointer env, Pointer data_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SetEnvironmentLocalStorage(Pointer env, Pointer data) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int AddToBootstrapClassLoaderSearch(Pointer env, Pointer segment) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int SetVerboseFlag(Pointer env, int flag, boolean value) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int AddToSystemClassLoaderSearch(Pointer env, Pointer segment) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int RetransformClasses(Pointer env, int class_count, Pointer classes) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetOwnedMonitorStackDepthInfo(Pointer env, JniHandle thread, Pointer monitor_info_count_ptr, Pointer monitor_info_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static int GetObjectSize(Pointer env, JniHandle object, Pointer size_ptr) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    @VM_ENTRY_POINT
    private static void SetJVMTIEnv(Pointer env) {
        JVMTI.setJVMTIEnv(env);
    }

}
