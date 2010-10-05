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
package com.sun.max.tele;

import java.io.*;
import java.util.*;

import com.sun.max.tele.debug.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Access to an instance of the VM.
 * <br>
 * This interface is a work in progress, created originally by splitting what had
 * been very intertwined code into two layers.  The eventual goal is for all VM types
 * to be expressed behind interfaces such as these, a transformation that is only
 * partially complete.
 * <br>
 * This could in the future be merged with the JDWP interface.
 *
 * @author Michael Van De Vanter
 * @author Hannes Payer
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
     * @return size of a word in the VM.
     */
    Size wordSize();

    /**
     * @return size a memory page in the VM
     */
    Size pageSize();

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
     * @return access to the VM heap.
     */
    MaxHeap heap();

    /**
     * @return access to the VM's cache of compiled code
     */
    MaxCodeCache codeCache();

    /**
     * Gets the manager for locating and managing code related information in the VM.
     * <br>
     * Thread-safe
     *
     * @return the singleton manager for information about code in the VM.
     */
    MaxCodeManager codeManager();

    /**
     * Gets the manager for creating and managing VM breakpoints.
     * <br>
     * Thread-safe
     *
     * @return the singleton manager for creating and managing VM breakpoints
     */
    MaxBreakpointManager breakpointManager();

    /**
     * Gets the manager for creating and managing VM watchpoints; null
     * if watchpoints are not supported on this platform.
     * <br>
     * Thread-safe
     *
     * @return the singleton manager for creating and managing VM watchpoints, or
     * null if watchpoints not supported.
     */
    MaxWatchpointManager watchpointManager();

    /**
     * Gets the manager for locating and managing thread-related information in the VM.
     * <br>
     * Thread-safe
     *
     * @return the singleton manager for information about threads in the VM.
     */
    MaxThreadManager threadManager();

    /**
     * An immutable summary of the VM state as of the most recent state transition.
     * <br>
     * Thread-safe.
     *
     * @return VM state summary
     */
    MaxVMState state();

    /**
     * Adds a VM state listener.
     * <br>
     * Thread-safe.
     *
     * @param listener will be notified of changes to {@link #state()}.
     */
    void addVMStateListener(MaxVMStateListener listener);

    /**
     * Removes a VM state listener.
     * <br>
     * Thread-safe.
     */
    void removeVMStateListener(MaxVMStateListener listener);

    /**
     * Adds a listener for GC starts in the VM.
     *
     * @param listener a listener for GC starts
     * @throws MaxVMBusyException
     */
    void addGCStartedListener(MaxGCStartedListener listener) throws MaxVMBusyException;

    /**
     * Removes a listener for GC starts in the VM.
     *
     * @param listener a listener for GC starts
     * @throws MaxVMBusyException
     */
    void removeGCStartedListener(MaxGCStartedListener listener) throws MaxVMBusyException;

    /**
     * Adds a listener for GC completions in the VM.
     *
     * @param listener a listener for GC completions
     * @throws MaxVMBusyException
     */
    void addGCCompletedListener(MaxGCCompletedListener listener) throws MaxVMBusyException;

    /**
     * Removes a listener for GC completions in the VM.
     *
     * @param listener a listener for GC completions
     * @throws MaxVMBusyException
     */
    void removeGCCompletedListener(MaxGCCompletedListener listener) throws MaxVMBusyException;

    /**
     * Finds the allocated region of memory in the VM, if any, that includes an address.
     *
     * @param address a memory location in the VM
     * @return the allocated {@link MaxMemoryRegion} containing the address, null if not in any known region.
     */
    MaxMemoryRegion findMemoryRegion(Address address);

    /**
     * Determines if the heap and code sections in the boot image have been relocated.
     */
    boolean isBootImageRelocated();

    /**
     * @return start location of the boot image in memory.
     */
    Pointer bootImageStart();

    /**
     * @return access to specific methods in the VM
     */
    TeleMethods teleMethods();

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
     * Low-level read of a word from memory of the VM.
     */
    Word readWord(Address address);

    /**
     * Low-level read of a word from memory of the VM.
     */
    Word readWord(Address address, int offset);

    /**
     * Low-level read of bytes from memory of the VM.
     */
    void readFully(Address address, byte[] bytes);

    /**
     * @param origin current absolute location of the beginning of a heap object's memory in the VM,
     * subject to relocation by GC.
     * @return a {@link Reference} to the object.
     */
    Reference originToReference(final Pointer origin);

    /**
     * @return a reference to the {@link ClassRegistry} in the boot heap of the VM.
     */
    Reference bootClassRegistryReference();

    /**
     * @param origin an absolute memory location in the VM.
     * @return whether there is a heap object with that origin.
     */
    boolean isValidOrigin(Pointer origin);

    /**
     * @param reference a {@link Reference} to memory in the VM.
     * @return whether there is a heap object at that location.
     */
    boolean isValidReference(Reference reference);

    /**
     * @param word contents of a memory word from the VM.
     * @return the word interpreted as a memory location, wrapped in a Reference.
     */
    Reference wordToReference(Word word);

    /**
     * @param reference a {@link Reference} to memory in the VM.
     * @return a {@link Value} that wraps a {@link Reference} to a memory location in the VM.
     */
    ReferenceValue createReferenceValue(Reference reference);

    /**
     * Gets a canonical local {@classActor} corresponding to the type of a heap object in the VM, creating one if
     * needed by loading the class using the {@link HostedBootClassLoader#HOSTED_BOOT_CLASS_LOADER} from either the
     * classpath, or if not found on the classpath, by copying the classfile from the VM.
     *
     * @param objectReference An {@link Object} in  VM heap.
     * @return Local {@link ClassActor} representing the type of the object.
     * @throws InvalidReferenceException
     */
    ClassActor makeClassActorForTypeOf(Reference objectReference)  throws InvalidReferenceException;

    /**
     * Fetches a primitive value from an array in the memory of the VM.
     *
     * @param kind identifies one of the basic VM value types
     * @param reference memory location in the VM of an array origin
     * @param index offset into the array
     * @return a value of the specified kind
     * @throws InvalidReferenceException
     */
    Value getElementValue(Kind kind, Reference reference, int index) throws InvalidReferenceException;

    /**
     * Interesting, predefined method entries that might be useful, for example, for setting breakpoints.
     *
     * @return possibly interesting, predefined methods.
     */
    List<MaxCodeLocation> inspectableMethods();

    /**
     * @param id  Class ID of a {@link ClassActor} in the VM.
     * @return surrogate for the {@link ClassActor} in the VM, null if not known.
     * @see ClassActor
     */
    TeleClassActor findTeleClassActor(int id);

    /**
     * @param typeDescriptor A local {@link TypeDescriptor}.
     * @return surrogate for the equivalent {@link ClassActor} in the VM, null if not known.
     * @see ClassActor
     */
    TeleClassActor findTeleClassActor(TypeDescriptor typeDescriptor);

    /**
     * @param type a local class instance
     * @return surrogate for the equivalent {@link ClassActor} in the VM, null if not known.
     */
    TeleClassActor findTeleClassActor(Class type);

    /**
     * @return  {@link TypeDescriptor}s for all classes loaded in the VM.
     */
    Set<TypeDescriptor> typeDescriptors();

    /**
     * @return an ordered set of {@link TypeDescriptor}s for classes loaded in
     *         the VM, plus classes found on the class path.
     */
    Iterable<TypeDescriptor> loadableTypeDescriptors();

    /**
     * Updates the set of types that are available by scanning the class path. This
     * scan will be performed automatically the first time
     * {@link #loadableTypeDescriptors()} is called. However, it should also be
     * performed any time the set of classes available on the class path may
     * have changed.
     */
    void updateLoadableTypeDescriptorsFromClasspath();

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
    Value interpretMethod(ClassMethodActor classMethodActor, Value... arguments) throws TeleInterpreterException;

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

}

