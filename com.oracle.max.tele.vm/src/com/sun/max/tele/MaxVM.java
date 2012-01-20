/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.tele.debug.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Access to an instance of the VM.
 * <p>
 * This interface is a work in progress, created originally by splitting what had
 * been very intertwined code into two layers.  The eventual goal is for all VM types
 * to be expressed behind interfaces such as these, a transformation that is only
 * partially complete.
 * <p>
 * This could in the future be merged with the JDWP interface.
 */
public interface MaxVM extends MaxEntity<MaxVM> {

    /**
     * @return the display version of the VM
     */
    String getVersion();

    /**
     * @return a textual description of the VM
     */
    String getDescription();

    /**
     * @return information about the platform on which the VM is running.
     */
    MaxPlatform platform();

    /**
     * @return the boot image from which this VM instance was created.
     */
    BootImage bootImage();

    /**
     * @return the file from which the boot image for this VM was created.
     */
    File bootImageFile();

    File programFile();

    /**
     * @return the mode in which the inspection is taking place.
     */
    MaxInspectionMode inspectionMode();

    /**
     * @return access to the VM's class registry and related information.
     */
    MaxClasses classes();

    /**
     * @return access to a map of top level allocations in the VM address space.
     */
    MaxAddressSpace addressSpace();

    /**
     * @return access to low level memory reading from the VM.
     */
    MaxMemoryIO memoryIO();

    /**
     * @return access to objects in the VM
     */
    MaxObjects objects();

    /**
     * @returns access to the heap in the VM.
     */
    MaxHeap heap();

    /**
     * @return access to the VM's cache of compiled code
     */
    MaxCodeCache codeCache();

    /**
     * Gets access to information about compiled code in the VM.
     */
    MaxNativeCode nativeCode();

    /**
     * Gets the manager for locating and managing code related information in the VM.
     * <p>
     * Thread-safe
     *
     * @return the singleton manager for information about code in the VM.
     */
    MaxCodeLocationManager codeLocations();

    /**
     * @returns access to information about machine code in the VM.
     */
    VmMachineCodeAccess machineCode();

    /**
     * Gets the manager for creating and managing VM breakpoints.
     * <p>
     * Thread-safe
     *
     * @return the singleton manager for creating and managing VM breakpoints
     */
    MaxBreakpointManager breakpointManager();

    /**
     * Gets the manager for creating and managing VM watchpoints; null
     * if watchpoints are not supported on this platform.
     * <p>
     * Thread-safe
     *
     * @return the singleton manager for creating and managing VM watchpoints, or
     * null if watchpoints not supported.
     */
    MaxWatchpointManager watchpointManager();

    /**
     * Gets the manager for locating and managing thread-related information in the VM.
     * <p>
     * Thread-safe
     *
     * @return the singleton manager for information about threads in the VM.
     */
    MaxThreadManager threadManager();

    /**
     * An immutable summary of the VM state as of the most recent state transition.
     * <p>
     * Thread-safe.
     *
     * @return VM state summary
     */
    MaxVMState state();

    /**
     * Adds a VM state listener.
     * <p>
     * Thread-safe.
     *
     * @param listener will be notified of changes to {@link #state()}.
     */
    void addVMStateListener(MaxVMStateListener listener);

    /**
     * Removes a VM state listener.
     * <p>
     * Thread-safe.
     */
    void removeVMStateListener(MaxVMStateListener listener);

    /**
     * Adds a listener for GC phase changes in the VM, either limited to the start of a specific
     * phase or for every phase transition if the phase argument is {@code null}.
     *
     * @param listener a listener for GC phase changes
     * @param phase a specific phase for which phase change notification is requested, null for every phase
     * @throws MaxVMBusyException
     */
    void addGCPhaseListener(MaxGCPhaseListener listener, HeapPhase phase) throws MaxVMBusyException;

    /**
     * Removes a listener for GC phase changes in the VM, either limited to the start of a specific
     * phase or for every phase transition if the phase argument is {@code null}.
     *
     * @param listener a listener for GC phase changes
     * @param phase a specific phase for which phase change notification was requested, null for every phase
     * @throws MaxVMBusyException
     */
    void removeGCPhaseListener(MaxGCPhaseListener listener, HeapPhase phase) throws MaxVMBusyException;

   /**
     * Adds a listener for VmThreads entering their run methods in the VM.
     * @param listener a listener for thread enter
     * @throws MaxVMBusyException
     */
    void addThreadEnterListener(MaxVMThreadEntryListener listener) throws MaxVMBusyException;

    /**
     * Adds a listener for VmThreads that just detached from the ACTIVE list of threads.
     * @param listener a listener for thread detach
     * @throws MaxVMBusyException
     */
    void addThreadDetachedListener(MaxVMThreadDetachedListener listener) throws MaxVMBusyException;

    /**
     * Removes a listener for VmThreads entering their run methods in the VM.
     * @param listener a listener for thread enter
     * @throws MaxVMBusyException
     */
    void removeThreadEnterListener(MaxVMThreadEntryListener listener) throws MaxVMBusyException;

   /**
    * Removes a listener for VmThreads that just detached from the ACTIVE list of threads.
    * @param listener a listener for thread detach
    * @throws MaxVMBusyException
    */
    void removeThreadDetachedListener(MaxVMThreadDetachedListener listener) throws MaxVMBusyException;

    /**
     * Gets whatever information is known about the current state of memory management at a particular location in VM
     * memory. Always returns a non-null description, even if nothing is known.
     *
     * @param address a location in VM memory
     * @return information about the location with respect to memory management in the VM
     */
    MaxMemoryManagementInfo getMemoryManagementInfo(Address address);

    /**
     * Determines if the heap and code sections in the boot image have been relocated.
     */
    boolean isBootImageRelocated();

    /**
     * @return start location of the boot image in memory.
     */
    Address bootImageStart();

    /**
     * @return how much reliance is placed on the {@link TeleInterpreter} when
     * communicating with the VM (0=none, 1=some, etc)
     */
    int getInterpreterUseLevel();

    /**
     * Controls how much reliance is placed on the {@link TeleInterpreter} when
     * communicating with the VM.
     *
     * @param interpreterUseLevel  (0=none, 1=some, etc)
     */
    void setInterpreterUseLevel(int interpreterUseLevel);

    /**
     * @return current trace level in the VM
     * @see com.sun.max.program.Trace
     */
    int getVMTraceLevel();

    /**
     * Sets tracing level in the VM.
     *
     * @param newLevel
     * @see com.sun.max.program.Trace
     */
    void setVMTraceLevel(int newLevel);

    /**
     * @return current trace threshold in the VM
     * @see com.sun.max.program.Trace
     */
    long getVMTraceThreshold();

    /**
     * Sets tracing threshold in the VM.
     *
     * @param newLevel
     * @see com.sun.max.program.Trace
     */
    void setVMTraceThreshold(long newThreshold);

    /**
     * Creates a remote object reference that can be used for access to a VM object.
     *
     * @param origin current location (origin) of a heap object's memory in the VM,
     * subject to relocation by GC.
     * @return a remote {@link Reference} to the object.
     */
    Reference makeReference(final Address origin);

    /**
     * @return a reference to the {@link ClassRegistry} in the boot heap of the VM.
     */
    Reference bootClassRegistryReference();

    /**
     * Creates a boxed value that acts as a reference to an object in the VM, useful for
     * creating arguments for remote interpretation.
     *
     * @param reference a {@link Reference} to memory in the VM.
     * @return a {@link Value} that wraps a {@link Reference} to a memory location in the VM.
     * @see #interpretMethod(ClassMethodActor, Value...)
     */
    ReferenceValue createReferenceValue(Reference reference);

    /**
     * Interesting, predefined method entries that might be useful, for example, for setting breakpoints.
     *
     * @return possibly interesting, predefined methods.
     */
    List<MaxCodeLocation> inspectableMethods();

    /**
     * Finds the remote {@link MethodActor} corresponding to a local one.
     *
     * @param <TeleMethodActor_Type> the type of the requested TeleMethodActor
     * @param teleMethodActorType the {@link Class} instance representing {@code TeleMethodActor_Type}
     * @param methodActor the local {@link MethodActor} describing the method
     * @return surrogate for the {@link MethodActor} of type {@code TeleMethodActor_Type} in the VM.
     */
    <TeleMethodActor_Type extends TeleMethodActor> TeleMethodActor_Type findTeleMethodActor(Class<TeleMethodActor_Type> teleMethodActorType, MethodActor methodActor);

    /**
     * Sets debugging trace level for the transport
     * mechanism that communicates with the VM.
     *
     * @param level new level: 0=none, 1=some, etc
     */
    void setTransportDebugLevel(int level);

    /**
     * Debugging trace level for the transport
     * mechanism that communicates with the VM.
     *
     * @return current level: 0=none, 1=some, etc
     */
    int transportDebugLevel();

    /**
     * Relocates the boot image, assuming that the inspector was invoked
     * with the option {@link MaxineInspector#suspendingBeforeRelocating()} set.
     *
     * @throws IOException
     */
    void advanceToJavaEntryPoint() throws IOException;

    /**
     * Interprets a method invocation in the context of the VM.
     *
     * @param classMethodActor method to interpret
     * @param arguments method arguments encapsulated as values, values that are understood as VM classes.
     * @return result of method invocation wrapped as a Value
     * @throws TeleInterpreterException  if an uncaught exception occurs during execution of the method
     */
    Value interpretMethod(ClassMethodActor classMethodActor, Value... arguments) throws InvocationTargetException;

    /**
     * Resumes execution of the VM.
     *
     * @param synchronous should the call wait for the execution to complete?
     * @param withClientBreakpoints should client breakpoints be enabled during execution?
     * @throws InvalidVMRequestException execution not permissible in current VM state.
     * @throws OSExecutionRequestException execution failed in OS.
     */
    void resume(final boolean synchronous, final boolean withClientBreakpoints) throws InvalidVMRequestException, OSExecutionRequestException;

    /**
     * Single steps a thread in the VM.
     *
     * @param thread a thread in the VM
     * @param synchronous should the call wait for the execution to complete.
     * @throws InvalidVMRequestException execution not permissible in current VM state.
     * @throws OSExecutionRequestException execution failed in OS.
     */
    void singleStepThread(final MaxThread maxThread, boolean synchronous) throws InvalidVMRequestException, OSExecutionRequestException;

    /**
     * Single steps a thread in the VM; if the instruction is a call, then resume VM execution until call returns.
     *
     * @param thread a thread in the VM.
     * @param synchronous should the call wait for the execution to complete?
     * @param withClientBreakpoints should client breakpoints be enabled during execution?
     * @throws InvalidVMRequestException execution not permissible in current VM state.
     * @throws OSExecutionRequestException execution failed in OS.
     */
    void stepOver(final MaxThread maxThread, boolean synchronous, final boolean withClientBreakpoints) throws InvalidVMRequestException, OSExecutionRequestException;

    /**
     * Resumes execution of the VM with a temporary breakpoint set.
     *
     * @param codeLocation location where temporary breakpoint should be set.
     * @param synchronous should the call wait for the execution to complete?
     * @param withClientBreakpoints should client breakpoints be enabled duration of the execution?
     * @throws OSExecutionRequestException execution failed in OS.
     * @throws InvalidVMRequestException execution not permissible in current VM state.
     */
    void runToInstruction(final MaxCodeLocation codeLocation, final boolean synchronous, final boolean withClientBreakpoints) throws OSExecutionRequestException, InvalidVMRequestException;

    /**
     * @param thread the thread whose top frame should be returned from
     * @param synchronous should the call wait for the execution to complete?
     * @param withClientBreakpoints should client breakpoints be enabled duration of the execution?
     * @throws OSExecutionRequestException execution failed in OS.
     * @throws InvalidVMRequestException execution not permissible in current VM state.
     */
    void returnFromFrame(final MaxThread thread, final boolean synchronous, final boolean withClientBreakpoints) throws OSExecutionRequestException, InvalidVMRequestException;

    /**
     * Pauses the running VM.
     *
     * @throws InvalidVMRequestException execution not permissible in current VM state.
     * @throws OSExecutionRequestException execution failed in OS.
     */
    void pauseVM() throws InvalidVMRequestException, OSExecutionRequestException;

    /**
     * Shuts down the VM completely.
     */
    void terminateVM() throws Exception;

    /**
     * Uses the configured source path in the VM to search for a source file corresponding to a class.
     *
     * @param classActor  the class for which a source file is to be found
     * @return the source file corresponding to {@code classActor} or null if so such source file can be found
     */
    File findJavaSourceFile(ClassActor classActor);

    /**
     * Runs commands from a specified file, supporting only "break <method signature>" commands at present.
     *
     * @param fileName name of a file containing commands.
     */
    void executeCommandsFromFile(String fileName);

    /**
     * Attempt to acquire access to the internal state of the VM, after which the VM will not be busy
     * and in fact cannot be run until the access is released. This is a temporary measure for allowing
     * free access to objects in the Tele project that are not yet isolated by interfaces.
     *
     * @see #releaseLegacyVMAccess()
     * @throws MaxVMBusyException if access cannot be acquired
     */
    void acquireLegacyVMAccess() throws MaxVMBusyException;

    /**
     * Release any access to the VM previously acquired; this must be done after access has
     * been acquired so that the VM can be run. This is a temporary measure for allowing
     * free access to objects in the Tele project that are not yet isolated by interfaces.
     *
     * @see MaxVM#acquireLegacyVMAccess()
     */
    void releaseLegacyVMAccess();

    /**
     * Gets the logger used to record detected invalid references.
     * <p>
     * Thread-safe
     *
     * @return the singleton invalid reference logger for this VM.
     */
    InvalidReferencesLogger invalidReferencesLogger();
}

