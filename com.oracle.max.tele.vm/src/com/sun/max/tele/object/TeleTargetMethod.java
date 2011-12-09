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
package com.sun.max.tele.object;

import java.io.*;
import java.util.*;

import com.oracle.max.vm.ext.t1x.*;
import com.sun.cri.ci.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetMethod.FrameAccess;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * Canonical surrogate for a heap object of type {@link TargetMethod} in the VM. That object extends
 * {@link MemoryRegion}, by which it describes the area allocated for it in one of the code cache memory regions. This
 * location can change for compilations that are allocated in managed code cache regions.
 * <p>
 * The {@link TargetMethod} stores the results of a method compilation in its allocated area in the form of three arrays
 * (one or two of which may be omitted) to which the {@link TargetMethod} holds references. These arrays are represented
 * in standard object format.
 * <p>
 * When this surrogate is first created, it records, in addition to the reference to the {@link TargetMethod}, only a
 * bare minimum of information about the compiled code, mainly just its location in code cache memory. This limitation
 * keeps the overhead low, since an instance of this class is eagerly created for every compilation discovered in the
 * VM. It also avoids creating any other instances of {@link TeleObject}, which can lead to infinite regress in the
 * presence of mutually referential objects, notably with instances of {@link TeleClassMethodActor}.
 * <p>
 * The first time this object is refreshed, it gets an instance of {@link TeleClassMethodActor} that refers to the
 * {@link ClassMethodActor} in the VM that owns the compilation represented by this object.
 * <p>
 * The full contents of the compilation (including the three arrays from the code cache) are "loaded" (copied) from the
 * VM, disassembled, and cached locally only when needed. This operation is relatively expensive because a variety of
 * summary information about the compilation are derived and included in the class. The local cache is marked
 * dirty whenever an update determines that the code has been changed (e.g. patched or relocated), and only reloaded as
 * needed.
 * <p>
 * A method compilation is loaded by (restricted) deep copying the {@link TargetMethod} from the VM, and caching the
 * local instance.
 * <p>
 * Clients of target method information should operate with the single thread-safe instance of {@link MachineCodeInfo}
 * provided by this object, since the information is guaranteed to represent a consistent snapshot of some version of
 * the compilation. That snapshot should not be cached by clients, however, because the compilation is subject to
 * change.
 *
 * @see VmCodeCacheAccess
 * @see VmCodeCacheRegion
 * @see TeleClassMethodActor
 */
public final class TeleTargetMethod extends TeleRuntimeMemoryRegion implements TargetMethodAccess {

    private static final int TRACE_VALUE = 2;

    /**
     * The data produced by a compilation is stored into an area of memory allocated from some part of the
     * {@linkplain VmCodeCacheAccess code cache}, where it is stored in standard VM object format. In particular, it is
     * stored as three contiguous arrays in the code cache allocation, although two of them might be omitted if not
     * needed.
     * <p>
     * The {@link TargetMethod} holds standard object {@link Reference}s to these three arrays.
     * <p>
     * No other kinds of objects should ever appear in a {@linkplain VmCodeCacheRegion code cache region}, so this enum
     * completely describes the possibilities.
     */
    public static enum CodeCacheReferenceKind {
        /**
         * Reference possibly held in a {@link TargetMethod} to an instance of {@code byte[]} in the code cache holding
         * scalar literals needed by the target code. This will be null in the following situations:
         * <ul>
         * <li>During the creation of a new method compilation, until the code cache area is allocated, the method is
         * compiled, and the scalar literals stored into the code cache;</li>
         * <li>If there are no scalar literals associated with the target code; and</li>
         * <li>If the compilation has been evicted.</li>
         * </ul>
         *
         * @see TargetMethod#scalarLiterals()
         */
        SCALAR_LITERALS,

        /**
         * Reference possibly held in a {@link TargetMethod} to an instance of {@code Object[]} in the code cache
         * holding reference literals needed by the target code. This will be null in the following situations:
         * <ul>
         * <li>During the creation of a new method compilation, until the code cache area is allocated, the method is
         * compiled, and the reference literals stored into the code cache;</li>
         * <li>If there are no reference literals associated with the target code; and</li>
         * <li>If the compilation has been evicted.</li>
         * </ul>
         *
         * @see TargetMethod#referenceLiterals()
         */
        REFERENCE_LITERALS,

        /**
         * Reference possibly held in a {@link TargetMethod} to an instance of {@code byte[]} in the code cache holding
         * the target code. This will be null in the following situations:
         * <ul>
         * <li>During the creation of a new method compilation, until the code cache area is allocated, the method is
         * compiled, and the code stored into the code cache;</li>
         * <li>If the compilation has been evicted.</li>
         * </ul>
         *
         * @see TargetMethod#code()
         */
        CODE;
    }

    /**
     * A specialized message generator for tracing that incurs no runtime cost unless the trace is actually printed.
     */
    private final class EventTracer {
        private final String event;

        public EventTracer(String event) {
            this.event = event;
        }

        @Override
        public String toString() {
            final String  name = teleClassMethodActor == null ? "<?>" : teleClassMethodActor.classMethodActor().format("%H.%n(%p)");
            final String regionName = codeCacheRegion == null ? "<?>" : codeCacheRegion.entityName();
            return tracePrefix() + event + ":  " + name + " in " + regionName;
        }
    }

    /**
     * The location in VM memory of the fixed sentinel assigned to the code field of the target method when the code is
     * evicted. Holds the value {@link Address#zero()} until the sentinel is discovered.
     *
     * @see TargetMethod#wipe()
     */
    private static Address codeWipedSentinelAddress = Address.zero();

    /**
     * A representation of the Java language entity, if any, from which this method was compiled.
     */
    private TeleClassMethodActor teleClassMethodActor = null;

    private Class compilationClass = null;

    /**
     * Absolute origin of an array of scalar literals referred to by target code, allocated (if non-empty) in the code
     * cache allocation for this method The location might change if the code cache allocation is moved, or become
     * specially marked as <em>wiped</em> if the compilation does not survive an eviction cycle. That special marking is
     * done by assignment of a distinguished empty array to the field.
     * <p>
     * This value is null only until the first successful read of their values from the object in the VM.
     *
     * @see CodeCacheReferenceKind#SCALAR_LITERALS
     * @see TargetMethod
     * @see TargetMethod#wipe()
     */
    private Address scalarLiteralArrayOrigin = null;

    /**
     * Absolute origin of an array of reference literals referred to by target code, allocated (if non-empty) in the
     * code cache allocation for this method The location might change if the code cache allocation is moved, or become
     * specially marked as <em>wiped</em> if the compilation does not survive an eviction cycle. That special marking is
     * done by assignment of a distinguished empty array to the field.
     * <p>
     * This value is null only until the first successful read of their values from the object in the VM.
     *
     * @see CodeCacheReferenceKind#REFERENCE_LITERALS
     * @see TargetMethod
     * @see TargetMethod#wipe()
     */
    private Address referenceLiteralArrayOrigin = null;

    /**
     * Absolute origin of a byte array containing target code, allocated in the code cache allocation for this method
     * The location might change if the code cache allocation is moved, or become specially marked as <em>wiped</em> if
     * the compilation does not survive an eviction cycle. That special marking is done by assignment of a distinguished
     * empty array to the field.
     * <p>
     * This value is null only until the first successful read of their values from the object in the VM.
     *
     * @see CodeCacheReferenceKind#CODE
     * @see TargetMethod
     * @see TargetMethod#wipe()
     */
    private Address codeByteArrayOrigin = null;

    /**
     * Absolute location in VM code cache memory of the first byte in this compilation's target code.
     */
    private Address codeStartAddress = null;

    /**
     * Absolute location in VM code cache memory immediately after the final byte in this compilation's target code.
     */
    private Address codeEndAddress = null;

    /**
     * A representation of the the part of the VM's code cache (a code cache region) in which this {@link TargetMetod}'s
     * compilation data is allocated and possibly managed.
     */
    private VmCodeCacheRegion codeCacheRegion = null;

    /**
     * The cache holding the compiled code associated with this {@link TargetMethod}, which can produce a single
     * immutable summary for thread safety. That summary object (an {@link MachineCodeInfo}) encapsulates a local copy of
     * the {@link TargetMethod} along with a collection of derived information.
     * <p>
     * A version number of the cache is kept. The initial state of the cache is version 0, contains no
     * {@link TargetMethod}, and is considered to be "unloaded". The initial state of the VM is considered to be version
     * 1. As soon as the cache object is replaced, the state of the cache is henceforth considered "loaded".
     * <p>
     * The cache is intended to include only the kind of detailed information about the method's machine code that is
     * needed under uncommon circumstances, for example when a user is inspecting the code directly. The cache is not
     * loaded (and after that reloaded) unless the detailed information is needed.
     */
    private final MachineCodeInfoCache machineCodeInfoCache;

    /**
     * A flag that permanently becomes {@code true} when an update detects that the code for this compilation has not
     * survived an eviction cycle. The actual test depends in the management being used for the code cache region
     * managing this compilation.
     */
    private boolean isCodeEvicted = false;

    /**
     * Starting location of the compilation's allocation in the code cache, the last time we checked.  Used to
     * detect when the code cache allocation has been relocated, but only used when allocated in a managed code cache region.
     */
    private Address previousAllocationStart = Address.zero();

    private final Object patchedTracer = new EventTracer("PATCHED");

    private final Object relocatedTracer = new EventTracer("RELOCATED");

    private final Object evictedTracer = new EventTracer("EVICTED");

    protected TeleTargetMethod(TeleVM vm, Reference targetMethodReference) {
        super(vm, targetMethodReference);
        machineCodeInfoCache = new MachineCodeInfoCache(vm, this);

        // Delay initialization of classMethodActor because of circularity:
        // the compilation history of the classMethodActor refers back to this.

        // Register every method compilation, so that they can be located by code address.
        // Note that this depends on the basic location information already being read by
        // superclass constructors.
        vm.machineCode().registerCompilation(this);
        previousAllocationStart = getRegionStart();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Compiled machine code generally doesn't change, so the code and disassembled instructions (once needed) are
     * cached. This update checks for cases where the code has changed since last seen, i.e. has been patched, and marks
     * the cache as dirty when this is observed.
     */
    @Override
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        if (!super.updateObjectCache(epoch, statsPrinter)) {
            return false;
        }
        if (isCodeEvicted) {
            // Once the compilation has been evicted from the code cache it is dead; the cache has been nulled
            // and no more updates are needed.
            return true;
        }
        try {
            // Start with some basic attributes we need to capture.
            if (teleClassMethodActor == null) {
                // Assumed not to change, once set.
                final Reference classMethodActorReference = fields().TargetMethod_classMethodActor.readReference(reference());
                teleClassMethodActor = (TeleClassMethodActor) objects().makeTeleObject(classMethodActorReference);
            }
            if (codeWipedSentinelAddress.isZero()) {
                // Static, assumed not to change, once set
                codeWipedSentinelAddress = fields().TargetMethod_WIPED_CODE.readWord(vm()).asAddress();
            }

            // See if the code cache allocation has been relocated
            boolean isRelocated = false;
            if (isRelocatable()) {
                final Address newAllocationStart = getRegionStart();
                if (previousAllocationStart.isNotZero() && !previousAllocationStart.equals(newAllocationStart)) {
                    machineCodeInfoCache.markDirty();
                    isRelocated = true;
                    previousAllocationStart = newAllocationStart;
                    Trace.line(TRACE_VALUE, relocatedTracer);
                }
            }
            // Read (or re-read if needed) the three fields that might point into the compilation's code cache allocation, in particular at the
            // arrays in which compilation data is stored. Use low level machinery to avoid circularity with Reference creation.
            if (scalarLiteralArrayOrigin == null || isRelocated) {
                scalarLiteralArrayOrigin = reference().readWord(fields().TargetMethod_scalarLiterals.fieldActor().offset()).asAddress();
            }
            if (referenceLiteralArrayOrigin == null || isRelocated) {
                referenceLiteralArrayOrigin = reference().readWord(fields().TargetMethod_referenceLiterals.fieldActor().offset()).asAddress();
            }
            if (codeByteArrayOrigin == null || isRelocatable()) {
                // We have to check this one even if not relocated, since we test it to see if the method has been evicted.
                codeByteArrayOrigin = reference().readWord(fields().TargetMethod_code.fieldActor().offset()).asAddress();
                // Get the absolute location of all target code bytes.
                // Use low level machinery; we don't want to create a {@link TeleObject} for every one of them.
                final RemoteTeleReference codeByteArrayRef = referenceManager().makeTemporaryRemoteReference(codeByteArrayOrigin);
                final int length = objects().unsafeReadArrayLength(codeByteArrayRef);
                codeStartAddress = objects().unsafeArrayIndexToAddress(Kind.BYTE, codeByteArrayOrigin, 0);
                codeEndAddress = objects().unsafeArrayIndexToAddress(Kind.BYTE, codeByteArrayOrigin, length);
            }
        } catch (DataIOError dataIOError) {
            // If something goes wrong, delay the cache update until next time.
        }
        // See if we have been evicted since last cycle by checking if the code pointer has been "wiped".
        if (isRelocatable() && codeWipedSentinelAddress.isNotZero() && codeByteArrayOrigin != null && codeByteArrayOrigin.equals(codeWipedSentinelAddress)) {
            markCodeEvicted();
            Trace.line(TRACE_VALUE, evictedTracer);
            return true;
        }
        if (!machineCodeInfoCache.isLoaded()) {
            // Don't update if we've never loaded the code; delay that until actually needed.
            return true;
        }
        if (machineCodeInfoCache.isDirty()) {
            // If we've already discovered that the loaded copy is not current, don't bother to
            // check again. It won't be reloaded until needed.
            return true;
        }
        try {
            // Test for a patch to the target code since the last time we looked.
            final Reference byteArrayReference = fields().TargetMethod_code.readReference(reference());
            final TeleArrayObject teleByteArrayObject = (TeleArrayObject) objects().makeTeleObject(byteArrayReference);
            final byte[] codeInVM = (byte[]) teleByteArrayObject.shallowCopy();
            if (!Arrays.equals(codeInVM, machineCodeInfoCache.machineCodeInfo().code())) {
                // The code in the VM is different than in the cache; record that it has changed.
                machineCodeInfoCache.markDirty();
                Trace.line(TRACE_VALUE, patchedTracer);
            }
        } catch (DataIOError dataIOError) {
            // If something goes wrong, delay the cache update until next time.
            return false;
        }
        return true;
    }

    /**
     * Assigns to this representation of a VM {@link TargetMethod} the representation of the code cache region in which
     * it has been discovered to have been allocated.
     * <p>
     * It is assumed that this is only set once, as target methods are assumed not to move among code cache regions.
     *
     * @param codeCacheRegion a code cache region in the VM
     */
    public void setCodeCacheRegion(VmCodeCacheRegion codeCacheRegion) {
        assert this.codeCacheRegion == null;
        this.codeCacheRegion = codeCacheRegion;
    }

    /**
     * Records (permanently) the observation that the code formerly associated with this compilation has been evicted by
     * the VM's code cache and is no longer used by the VM.
     */
    private void markCodeEvicted() {
        assert !isCodeEvicted;
        isCodeEvicted = true;
        machineCodeInfoCache.update(null, null);
    }

    /**
     * Has the code associated with this compilation has been evicted by the VM's code cache and therefore become
     * permanently unused by the VM.
     */
    public boolean isCodeEvicted() {
        return isCodeEvicted;
    }

    /**
     * Determines whether there is machine code in this compilation at a specified memory location in the VM, always
     * {@code false} if this compilation has been evicted.
     *
     * @param address an absolute memory location in the VM.
     * @return whether there is machine code at the address
     * @throws IllegalArgumentException if the location is not within the code cache memory allocated for this
     *             compilation.
     */
    public boolean isValidCodeLocation(Address address) throws IllegalArgumentException {
        if (isCodeEvicted()) {
            return false;
        }
        if (!contains(address)) {
            throw new IllegalArgumentException("Address " + address.to0xHexString() + " not in code cache allocation");
        }
        return address.greaterEqual(codeStartAddress) && address.lessThan(codeEndAddress);
    }

    /**
     * Return the absolute origin location of each of the arrays holding compilation data that are stored in the
     * compilation's code cache allocation.
     * <p>
     * In the current implementation, these pointers in the {@link TargetMethod} are set to special statically allocated
     * sentinels ("wiped") when the compilation is evicted.
     */
    public Address codeCacheObjectOrigin(CodeCacheReferenceKind kind) {
        switch (kind) {
            case SCALAR_LITERALS:
                return scalarLiteralArrayOrigin;
            case REFERENCE_LITERALS:
                return referenceLiteralArrayOrigin;
            case CODE:
                return codeByteArrayOrigin;
            default:
                TeleError.unknownCase();
                return null;
        }
    }

    /**
     * Determines whether we have ever copied information about the {@link TargetMethod} from the VM, which is done only
     * on demand.
     *
     * @return whether a copy of the {@link TargetMethod} in the VM has been created and cached.
     */
    public boolean isCacheLoaded() {
        return machineCodeInfoCache.isLoaded();
    }

    /**
     * Counter for the versions of the code held by a{@link TargetMethod} during its lifetime in the VM, starting with
     * its original version, which we call 1. Note that the initial (null) instance of the cache is set to version 0,
     * which means that the cache begins life as dirty, which is how it will remain until detailed information is
     * needed and the contents of the machine code is loaded into the cache.
     * <p>
     * The version can be incremented for different reasons:
     * <ul>
     * <li>The machine code has been "patched"</li>
     * <li>The code has been relocated</li>
     * </ul>
     * Note that this number relates only to the <em>observed</em> changes, not necessarily the actual changes that
     * might have taken place in the VM.
     */
    public int codeVersion() {
        return machineCodeInfoCache.machineCodeInfo().codeVersion();
    }

    /**
     * Determines whether this is a baseline compilation; if not, it can be assumed to be an optimized compilation.
     */
    public boolean isBaseline() {
        if (compilationClass == null) {
            compilationClass = classActorForObjectType().javaClass();
        }
        return compilationClass == T1XTargetMethod.class;
    }

    /**
     * @return a local copy of the {@link TargetMethod} in the VM, the most recently observed version.
     */
    public TargetMethod targetMethod() {
        return machineCodeInfoCache.machineCodeInfo().targetMethod();
    }

    /**
     * @return surrogate for the {@link ClassMethodActor} in the VM for which this code was compiled.
     */
    public TeleClassMethodActor getTeleClassMethodActor() {
        return teleClassMethodActor;
    }

    public MaxMachineCodeInfo getMachineCodeInfo() {
        return machineCodeInfoCache.machineCodeInfo();
    }

    /**
     * Gets VM memory location of the first instruction in the method.
     *
     * @see TargetMethod#codeStart()
     */
    public Pointer getCodeStart() {
        return machineCodeInfoCache.machineCodeInfo().codeStart();
    }

    /**
     * Gets the call entry memory location in the VM for this method.
     *
     * @return {@link Address#zero()} if this target method has not yet been compiled
     */
    public Address callEntryPoint() {
        return machineCodeInfoCache.machineCodeInfo().callEntryPoint();
    }

    /**
     * Gets the local mirror of the class method actor associated with this target method. This may be null.
     */
    public ClassMethodActor classMethodActor() {
        return teleClassMethodActor == null ? null : teleClassMethodActor.classMethodActor();
    }

    public int[] bciToPosMap() {
        return machineCodeInfoCache.machineCodeInfo().bciToPosMap();
    }

    /**
     * Gets the debug info available for a given safepoint index.
     *
     * @param safepointIndex a safepoint index
     * @return the debug info available for {@code safepointIndex} or null if there is none
     * @see TargetMethod#debugInfoAt(int, FrameAccess)
     */
    public CiDebugInfo getDebugInfoAtSafepointIndex(final int safepointIndex) {
        return machineCodeInfoCache.machineCodeInfo().getDebugInfoAtSafepointIndex(safepointIndex);
    }

    /**
     * Gets the name of the source variable corresponding to a stack slot, if any.
     *
     * @param slot a stack slot
     * @return the Java source name for the frame slot, null if not available.
     */
    public String sourceVariableName(MaxStackFrame.Compiled javaStackFrame, int slot) {
        return null;
    }

    // [tw] Warning: duplicated code!
    public MachineCodeInstructionArray getTargetCodeInstructions() {
        final List<TargetCodeInstruction> instructions = machineCodeInfoCache.machineCodeInfo().instructions();
        final MachineCodeInstruction[] result = new MachineCodeInstruction[instructions.size()];
        for (int i = 0; i < result.length; i++) {
            final TargetCodeInstruction ins = instructions.get(i);
            result[i] = new MachineCodeInstruction(ins.mnemonic, ins.position, ins.address.toLong(), ins.label, ins.bytes, ins.operands, ins.getTargetAddressAsLong());
        }
        return new MachineCodeInstructionArray(result);
    }

    public MethodProvider getMethodProvider() {
        return this.teleClassMethodActor;
    }

    @Override
    public ReferenceTypeProvider getReferenceType() {
        return vm().vmAccess().getReferenceType(getClass());
    }

    /**
     * {@inheritDoc}
     * <p>
     * A {@link TargetMethod} (method compilation) region does not move (see class comment) unless it is in a managed
     * code cache region; we might not know, however, until we have been told the region in which the code is allocated.
     */
    @Override
    public boolean isRelocatable() {
        // This test could in principle be refined so that a relocation check is only needed when the cache region is in an eviction cycle
        // or an eviction cycle has completed since the last update.  But care is needed.  This predicate is called by the
        // superclass to decide whether to update the start and size of the region, and that call happens before this class
        // has the opportunity to check the status of evictions.
        if (codeCacheRegion != null && !codeCacheRegion.isManaged()) {
            // In an unmanaged code region, code is assumed to never move.
            return false;
        }
        return true;
    }

    public void writeSummary(PrintStream printStream) {
        machineCodeInfoCache.writeSummary(printStream);
    }

}
