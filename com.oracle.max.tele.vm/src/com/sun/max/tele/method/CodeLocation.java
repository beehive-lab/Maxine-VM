/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.method;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.*;
import com.sun.max.tele.*;
import com.sun.max.tele.MaxMachineCodeRoutine.InstructionMap;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;

// TODO (mlvdv) extend code location to represent source code
// TODO (mlvdv) extend code location to represent ranges of instructions
// TODO (mlvdv) complete instruction mapping code bytecode <-> machine code, see VmBytecodeBreakpoint

/**
 * Location of a code instruction in the VM.  This might be
 * specified in terms of one or more representations:
 * compiled machine code, bytecode, source code, or some
 * combination that represents an equivalent location in each.  An instance may
 * not have all kinds of information. Some kinds of additional information cannot be
 * determined until an initial reading of the VM state has been completed.  Some kinds
 * of information cannot be determined until a specified class is loaded into the VM.
 * <p>
 * A location with an address (subclasses of {@link MachineCodeLocation} is assumed
 * to refer uniquely to a single compilation of a method,
 * where the actual method may or (in rare cases) may not be known.
 * <p>
 * A location originally specified without an address (subclasses of {@link BytecodeLocation})
 * is assumed to refer to the method in general, and by implication to every machine code
 * compilation.
 * <p>
 * This class is intended to encapsulate as many techniques for mapping among code locations
 * as possible.
 */
public abstract class CodeLocation extends AbstractVmHolder implements MaxCodeLocation {

    private final String description;

    private TeleCompilation compiledCode;
    private CiDebugInfo debugInfo = null;

    private CodeLocation(TeleVM vm, String description) {
        super(vm);
        this.description = description;
    }

    public final TeleCompilation compiledCode() {
        if (compiledCode == null && hasAddress()) {
            compiledCode = addressToCompiledCode(address());
        }
        return compiledCode;
    }

    public final CiDebugInfo debugInfo() {
        if (debugInfo == null && hasAddress()) {
            debugInfo = addressToDebugInfo(address());
        }
        return debugInfo;
    }

    public int bci() {
        CiDebugInfo info = debugInfo();
        if (info == null) {
            return -1;
        }
        CiCodePos codePos = info.codePos;
        if (codePos == null) {
            return -1;
        }
        while (codePos.caller != null) {
            codePos = codePos.caller;
        }
        return codePos.bci;
    }

    public final String description() {
        return description;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");
        if (hasAddress()) {
            sb.append(" 0x").append(address().toHexString()).append(", ");
        }
        if (hasTeleClassMethodActor()) {
            sb.append(teleClassMethodActor().getName()).append("(), bci=").append(bci());
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Attempt to create an abstract key for a method located in the VM.  Null if
     * the information cannot be determined at present, for example if the VM is busy.
     *
     * @param teleClassMethodActor location in a method's bytecode, specified by loaded
     * method description in the VM.
     * @return a new key describing the method
     */
    protected MethodKey teleClassMethodActorToMethodKey(TeleClassMethodActor teleClassMethodActor) {
        assert teleClassMethodActor != null;
        if (vm().tryLock()) {
            try {
                return new MethodKey.DefaultMethodKey(teleClassMethodActor().methodActor());
            } finally {
                vm().unlock();
            }
        }
        return null;
    }

    /**
     * Attempt to locate information for a method loaded in the VM, based
     * on an abstract description of the method.  Null if the method is not known to be loaded,
     * or if the information cannot be determined at present, for example if the VM is busy.
     *
     * @param methodKey an abstract specification of a method
     * @return a description of the method loaded in the VM
     */
    protected TeleClassMethodActor methodKeyToTeleClassMethodActor(MethodKey methodKey) {
        if (vm().tryLock()) {
            try {
                final TeleClassActor teleClassActor = classes().findTeleClassActor(methodKey.holder());
                if (teleClassActor != null) {
                    // find a matching method
                    final String methodKeyString = methodKey.signature().toJavaString(true, true);
                    for (TeleMethodActor teleMethodActor : teleClassActor.getTeleMethodActors()) {
                        if (teleMethodActor instanceof TeleClassMethodActor) {
                            if (teleMethodActor.methodActor().descriptor().toJavaString(true, true).equals(methodKeyString)) {
                                return (TeleClassMethodActor) teleMethodActor;
                            }
                        }
                    }
                }
                // TODO (mlvdv) when the class registry is complete, this should not be necessary
                // Try to locate TeleClassMethodActor via compiled methods in the VM.
                final List<TeleTargetMethod> teleTargetMethods = TeleTargetMethod.get(vm(), methodKey);
                if (teleTargetMethods.size() > 0) {
                    return Utils.first(teleTargetMethods).getTeleClassMethodActor();
                }
            } finally {
                vm().unlock();
            }
        }
        return null;
    }

    /**
     * Attempt to locate bytecode location information for the method whose compilation
     * includes a machine code instruction at a given address.
     * <p>
     * Note that in many cases it will not be possible to determine the actual corresponding
     * bytecode instruction, in which case the position will be 0 (method entry).
     * <p>
     * <strong>Unimplemented:</strong> No attempt is made at present to compute a
     * bytecode instruction position corresponding to the machine code location, although this
     * could be done for JIT methods.
     *
     * @param address an address in the VM, possibly the location of a machine code instruction
     * in the compilation of a method.
     * @return  a description of the location expressed in terms of the method loaded in the VM
     */
    protected TeleClassMethodActor addressToTeleClassMethodActor(Address address) {
        if (vm().tryLock()) {
            try {
                final TeleCompilation compiledCode = vm().codeCache().findCompiledCode(address);
                if (compiledCode != null) {
                    return compiledCode.getTeleClassMethodActor();
                }
            } finally {
                vm().unlock();
            }
        }
        return null;
    }

    protected TeleCompilation addressToCompiledCode(Address address) {
        if (address != null && !address.isZero()) {
            return vm().codeCache().findCompiledCode(address);
        }
        return null;
    }

    /**
     * Attempt to locate debug info associated with the machine
     * code instruction, if any, at the specified memory location.
     *
     * @param address an address in the VM, possibly the location of a machine code instruction
     * in the compilation of a method.
     * @return the debug info for the machine code instruction, null if not available
     */
    protected CiDebugInfo addressToDebugInfo(Address address) {
        if (address != null && !address.isZero()) {
            if (compiledCode() != null) {
                final InstructionMap instructionMap = compiledCode().getInstructionMap();
                final int instructionIndex = instructionMap.findInstructionIndex(address);
                if (instructionIndex >= 0) {
                    return instructionMap.debugInfoAt(instructionIndex);
                }
            }
        }
        return null;
    }

    /**
     * Attempt to locate bytecode location information for a method known to be loaded and compiled
     * into the boot image.  This should always succeed if not called too early in the startup cycle, and if
     * the VM is not busy.
     *
     * @param teleMethodAccess a static method accessor produced by annotation of distinguished VM methods.
     * @return a new location
     */
    protected TeleClassMethodActor methodAccessToTeleClassMethodActor(TeleMethodAccess teleMethodAccess) {
        if (vm().tryLock()) {
            try {
                return teleMethodAccess.teleClassMethodActor();
            } finally {
                vm().unlock();
            }
        }
        return null;
    }

    /**
     * Attempt to determine the first machine code instruction address of the current
     * compilation of the specified method.  This is the first machine code instruction
     * of the method prologue, which is equivalent to a bci of -1.
     *
     * @param teleClassMethodActor description of the method in the VM
     * @return a non-zero address, null if not available
     */
    protected Address teleClassMethodActorToFirstCompilationCallEntry(TeleClassMethodActor teleClassMethodActor) {
        if (vm().tryLock()) {
            try {
                final TeleTargetMethod javaTargetMethod = teleClassMethodActor.getCurrentCompilation();
                if (javaTargetMethod != null) {
                    return javaTargetMethod.callEntryPoint();
                }
            } finally {
                vm().unlock();
            }
        }
        return null;
    }

    /**
     * A code location that refers to a method in general, and not to any specific compilation.
     *
     */
    public abstract static class BytecodeLocation extends CodeLocation {

        private BytecodeLocation(TeleVM vm, String description) {
            super(vm, description);
        }

        public boolean isSameAs(MaxCodeLocation codeLocation) {
            if (this.hasMethodKey() && codeLocation instanceof BytecodeLocation) {
                return methodKey().equals(codeLocation.methodKey());
            }
            return false;
        }

    }

    /**
     * A code location that refers to an instruction in a specific compilation of a method, even in the
     * (occasional) case where details of the method are not yet known, or in the rare
     * cases where the address isn't yet known because not enough of the VM state
     * has been modeled to determine it.
     *
     */
    public abstract static class MachineCodeLocation extends CodeLocation {

        private MachineCodeLocation(TeleVM vm, String description) {
            super(vm, description);
        }

        public boolean isSameAs(MaxCodeLocation codeLocation) {
            if (this.hasAddress() && codeLocation instanceof MachineCodeLocation) {
                return address().equals(codeLocation.address());
            }
            return false;
        }
    }

    /**
     * A code location in the VM specified by an abstract description of a method, which may
     * not yet be loaded in the VM.  The implied bytecode position in the method is -1; this refers
     * to the first bytecode instruction and to the beginning of the prologue in any machine code compilation.
    * It is not bound to any particular compilation, and so never had a machine code address.
     *
     * @see MaxCodeLocationFactory#createBytecodeLocation(MethodKey, String)
     */
    private static final class MethodKeyLocation extends BytecodeLocation {

        private final MethodKey methodKey;
        private volatile TeleClassMethodActor teleClassMethodActor = null;

        private MethodKeyLocation(TeleVM vm, MethodKey methodKey, String description) {
            super(vm, description);
            TeleError.check(methodKey != null);
            this.methodKey = methodKey;
        }

        public boolean hasAddress() {
            return false;
        }

        public Address address() {
            return Address.zero();
        }

        public boolean hasTeleClassMethodActor() {
            tryLocateTeleClassMethodActor();
            return teleClassMethodActor != null;
        }

        public TeleClassMethodActor teleClassMethodActor() {
            tryLocateTeleClassMethodActor();
            return teleClassMethodActor;
        }

        public boolean hasMethodKey() {
            return true;
        }

        public MethodKey methodKey() {
            return methodKey;
        }

        private void tryLocateTeleClassMethodActor() {
            if (teleClassMethodActor == null) {
                teleClassMethodActor = methodKeyToTeleClassMethodActor(methodKey);
            }
        }
    }

    /**
     * A code location in the VM specified only as a bytecode position in a loaded classfile method.
     * It is not bound to any particular compilation, and so never had a machine code address.
     *
     * @see MaxCodeLocationFactory#createBytecodeLocation(TeleClassMethodActor, int, String)
     */
    private static final class ClassMethodActorLocation extends BytecodeLocation {

        private final TeleClassMethodActor teleClassMethodActor;
        private final int bci;
        private volatile MethodKey methodKey = null;

        private ClassMethodActorLocation(TeleVM vm, TeleClassMethodActor teleClassMethodActor, int bci, String description) {
            super(vm, description);
            TeleError.check(teleClassMethodActor != null);
            TeleError.check(bci >= -1);
            this.teleClassMethodActor = teleClassMethodActor;
            this.bci = bci;
        }

        public boolean hasAddress() {
            return false;
        }

        public Address address() {
            return Address.zero();
        }

        public boolean hasTeleClassMethodActor() {
            return true;
        }

        public TeleClassMethodActor teleClassMethodActor() {
            return teleClassMethodActor;
        }

        @Override
        public int bci() {
            return bci;
        }

        public boolean hasMethodKey() {
            tryCreateKey();
            return methodKey != null;
        }

        public MethodKey methodKey() {
            tryCreateKey();
            return methodKey;
        }

        private void tryCreateKey() {
            if (methodKey == null) {
                methodKey = teleClassMethodActorToMethodKey(teleClassMethodActor);
            }
        }

    }

    /**
     * A code location in the VM specified only as an address in compiled code.
     * <p>
     * Additional information about the compilation, the method, and an equivalent
     * bytecode location will be discovered when possible.
     *
     * @see MaxCodeLocationFactory#createMachineCodeLocation(Address, String)
     */
    private static final class AddressCodeLocation extends MachineCodeLocation {

        // TODO (mlvdv) distinguish between cases where we are able to locate
        // a method compilation and those where we are not.  The latter would
        // be typical for external code locations, but there still seems to be
        // the possibility in the startup cycle of having an address for which
        // we locate the compilation only somewhat later.

        private final Address address;
        private volatile TeleClassMethodActor teleClassMethodActor = null;
        private volatile MethodKey methodKey = null;

        private AddressCodeLocation(TeleVM vm, Address address, String description) {
            super(vm, description);
            TeleError.check(address != null && !address.isZero());
            this.address = address;
        }

        public boolean hasAddress() {
            return true;
        }

        public Address address() {
            return address;
        }

        public boolean hasTeleClassMethodActor() {
            tryLocateTeleClassMethodActor();
            return teleClassMethodActor != null;
        }

        public TeleClassMethodActor teleClassMethodActor() {
            tryLocateTeleClassMethodActor();
            return teleClassMethodActor;
        }

        public boolean hasMethodKey() {
            tryCreateMethodKey();
            return methodKey != null;
        }

        public MethodKey methodKey() {
            tryCreateMethodKey();
            return methodKey;
        }

        private void tryLocateTeleClassMethodActor() {
            if (teleClassMethodActor == null) {
                teleClassMethodActor = addressToTeleClassMethodActor(address);
            }
        }

        private void tryCreateMethodKey() {
            if (methodKey == null) {
                tryLocateTeleClassMethodActor();
                if (teleClassMethodActor != null) {
                    methodKey = teleClassMethodActorToMethodKey(teleClassMethodActor);
                }
            }
        }

    }

    /**
     * A code location in the VM specified both as a bytecode position in a loaded classfile and the
     * memory location of the corresponding machine code instruction in a compilation of the method.
     *
     * @see MaxCodeLocationFactory#createMachineCodeLocation(Address, TeleClassMethodActor, int, String)
     */
    private static final class ClassMethodActorAddressLocation extends MachineCodeLocation {

        private final Address address;
        private final TeleClassMethodActor teleClassMethodActor;
        private final int bci;
        private volatile MethodKey methodKey = null;

        private ClassMethodActorAddressLocation(TeleVM vm, Address address, TeleClassMethodActor teleClassMethodActor, int bci, String description) {
            super(vm, description);
            TeleError.check(address != null && !address.isZero());
            TeleError.check(teleClassMethodActor != null);
            TeleError.check(bci >= -1);
            this.address = address;
            this.teleClassMethodActor = teleClassMethodActor;
            this.bci = bci;
        }

        public boolean hasAddress() {
            return true;
        }

        public Address address() {
            return address;
        }

        public boolean hasTeleClassMethodActor() {
            return true;
        }

        public TeleClassMethodActor teleClassMethodActor() {
            return teleClassMethodActor;
        }

        public boolean hasMethodKey() {
            tryCreateMethodKey();
            return methodKey != null;
        }

        public MethodKey methodKey() {
            tryCreateMethodKey();
            return methodKey;
        }

        private void tryCreateMethodKey() {
            if (methodKey == null) {
                methodKey = teleClassMethodActorToMethodKey(teleClassMethodActor);
            }
        }
    }

    /**
     * A code location specified as the entry of a method known to be compiled into the
     * boot image, identified by an annotation in the VM source code and made available
     * with static accessors.
     * <p>
     * The location corresponds to the beginning of the compiled method prologue, which
     * is equivalent to a bytecode position specification of -1.
     *
     */
    private static final class MethodAccessLocation extends MachineCodeLocation {

        private final TeleMethodAccess teleMethodAccess;
        private volatile Address address = null;
        private volatile TeleClassMethodActor teleClassMethodActor = null;
        private volatile MethodKey methodKey = null;

        private MethodAccessLocation(TeleVM vm, TeleMethodAccess teleMethodAccess, String description) {
            super(vm, description);
            TeleError.check(teleMethodAccess != null);
            this.teleMethodAccess = teleMethodAccess;
        }

        public boolean hasAddress() {
            tryCreateAddress();
            return address != null;
        }

        public Address address() {
            tryCreateAddress();
            return address;
        }

        public boolean hasTeleClassMethodActor() {
            tryLocateTeleClassMethodActor();
            return teleClassMethodActor != null;
        }

        public TeleClassMethodActor teleClassMethodActor() {
            tryLocateTeleClassMethodActor();
            return teleClassMethodActor;
        }

        public boolean hasMethodKey() {
            tryCreateMethodKey();
            return methodKey != null;
        }

        public MethodKey methodKey() {
            tryCreateMethodKey();
            return methodKey;
        }

        // TODO (mlvdv)  check both known compilations?
        private void tryCreateAddress() {
            if (address == null) {
                tryLocateTeleClassMethodActor();
                if (teleClassMethodActor != null) {
                    address = teleClassMethodActorToFirstCompilationCallEntry(teleClassMethodActor);
                }
            }
        }

        private void tryLocateTeleClassMethodActor() {
            if (teleClassMethodActor == null) {
                teleClassMethodActor = methodAccessToTeleClassMethodActor(teleMethodAccess);
            }
        }

        private void tryCreateMethodKey() {
            if (methodKey == null) {
                tryLocateTeleClassMethodActor();
                if (teleClassMethodActor != null) {
                    methodKey = teleClassMethodActorToMethodKey(teleClassMethodActor);
                }
            }
        }

    }

    // TODO (mlvdv)  to be replaced
    /**
     * Singleton for creating representations of code locations in the VM.
     */
    public static final class CodeLocationFactory extends AbstractVmHolder implements MaxCodeLocationFactory {

        private static final int TRACE_VALUE = 1;

        private static CodeLocationFactory codeLocationFactory;

        /**
         * Create a factory for creating code locations in VM memory.
         * <p>
         * <strong>Note:</strong> this legacy implementation is being replaced.
         */
        public static CodeLocationFactory make(TeleVM vm) {
            if (codeLocationFactory == null) {
                codeLocationFactory = new CodeLocationFactory(vm);
            }
            return codeLocationFactory;
        }

        private CodeLocationFactory(TeleVM vm) {
            super(vm);
        }

        public BytecodeLocation createBytecodeLocation(MethodKey methodKey, String description) throws TeleError {
            return new MethodKeyLocation(vm(), methodKey, description);
        }

        public  BytecodeLocation createBytecodeLocation(TeleClassMethodActor teleClassMethodActor, int bci, String description) {
            return new ClassMethodActorLocation(vm(), teleClassMethodActor, bci, description);
        }

        public MachineCodeLocation createMachineCodeLocation(Address address, String description) throws TeleError {
            return new AddressCodeLocation(vm(), address, description);
        }

        public MachineCodeLocation createMachineCodeLocation(Address address, TeleClassMethodActor teleClassMethodActor, int bci, String description) throws TeleError {
            return new ClassMethodActorAddressLocation(vm(), address, teleClassMethodActor, bci, description);
        }

        /**
         * Creates a code location in the VM specified by a predefined method accessor for compiled methods in
         * the boot image.  Resolution of the accessor into other VM-related information is delayed, so that
         * these location instances can be created without any other VM-related services, early in the
         * startup cycle.
         *
         * @param vm the VM
         * @param teleMethodAccess a statically defined accessor for a specially marked method in VM code
         * @param description a human-readable description, suitable for a menu or for debugging
         * @return a new location
         * @throws TeleError if teleMethodAccess is null
         */
        public MachineCodeLocation createMachineCodeLocation(TeleMethodAccess teleMethodAccess, String description) throws TeleError {
            return new MethodAccessLocation(vm(), teleMethodAccess, description);
        }

    }
}
