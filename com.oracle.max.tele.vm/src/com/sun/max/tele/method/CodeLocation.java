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

import com.sun.cri.ci.*;
import com.sun.max.tele.*;
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

    private TeleCompilation compilation;
    private CiDebugInfo debugInfo = null;

    private CodeLocation(TeleVM vm, String description) {
        super(vm);
        this.description = description;
    }

    public final boolean hasAddress() {
        final RemoteCodePointer codePointer = codePointer();
        return codePointer != null && codePointer.isCodeLive();
    }

    public final Address address() {
        return codePointer() == null ? Address.zero() : codePointer().getAddress();
    }

    public final TeleCompilation compilation() {
        if (compilation == null && hasAddress()) {
            compilation = findCompilation(codePointer());
        }
        return compilation;
    }

    public final CiDebugInfo debugInfo() {
        if (debugInfo == null && codePointer() != null && compilation() != null) {
            final MaxMachineCodeInfo machineCodeInfo = compilation().getMachineCodeInfo();
            final int instructionIndex = machineCodeInfo.findInstructionIndex(codePointer().getAddress());
            if (instructionIndex >= 0) {
                debugInfo = machineCodeInfo.debugInfoAt(instructionIndex);
            }
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

    public abstract RemoteCodePointer codePointer();

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

    protected TeleCompilation findCompilation(RemoteCodePointer codePointer) {
        if (codePointer != null) {
            return vm().machineCode().findCompilation(codePointer);
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

        @Override
        public RemoteCodePointer codePointer() {
            return null;
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

        private volatile MethodKey methodKey = null;

        private MachineCodeLocation(TeleVM vm, String description) {
            super(vm, description);
        }

        public final boolean isSameAs(MaxCodeLocation codeLocation) {
            if (this.hasAddress() && codeLocation instanceof MachineCodeLocation) {
                return address().equals(codeLocation.address());
            }
            return false;
        }

        public final boolean hasMethodKey() {
            return methodKey() != null;
        }

        public final MethodKey methodKey() {
            if (methodKey == null && teleClassMethodActor() != null) {
                methodKey = teleClassMethodActorToMethodKey(teleClassMethodActor());
            }
            return methodKey;
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

        public boolean hasTeleClassMethodActor() {
            return teleClassMethodActor() != null;
        }

        public TeleClassMethodActor teleClassMethodActor() {
            if (teleClassMethodActor == null) {
                teleClassMethodActor = vm().methods().findClassMathodActor(methodKey);
            }
            return teleClassMethodActor;
        }

        public boolean hasMethodKey() {
            return true;
        }

        public MethodKey methodKey() {
            return methodKey;
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
            return methodKey() != null;
        }

        public MethodKey methodKey() {
            if (methodKey == null) {
                methodKey = teleClassMethodActorToMethodKey(teleClassMethodActor);
            }
            return methodKey;
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

        private final RemoteCodePointer codePointer;
        private volatile TeleClassMethodActor teleClassMethodActor = null;

        private AddressCodeLocation(TeleVM vm, RemoteCodePointer codePointer, String description) {
            super(vm, description);
            TeleError.check(codePointer != null);
            this.codePointer = codePointer;
        }

        public boolean hasTeleClassMethodActor() {
            return teleClassMethodActor() != null;
        }

        public TeleClassMethodActor teleClassMethodActor() {
            if (teleClassMethodActor == null) {
                if (vm().tryLock()) {
                    try {
                        final TeleCompilation compilation = findCompilation(codePointer);
                        if (compilation != null) {
                            teleClassMethodActor = compilation.getTeleClassMethodActor();
                        }
                    } finally {
                        vm().unlock();
                    }
                }
            }
            return teleClassMethodActor;
        }

        @Override
        public RemoteCodePointer codePointer() {
            return codePointer;
        }

    }

    /**
     * A code location in the VM specified both as a bytecode position in a loaded classfile and the
     * memory location of the corresponding machine code instruction in a compilation of the method.
     *
     * @see MaxCodeLocationFactory#createMachineCodeLocation(Address, TeleClassMethodActor, int, String)
     */
    private static final class ClassMethodActorAddressLocation extends MachineCodeLocation {

        private final RemoteCodePointer codePointer;
        private final TeleClassMethodActor teleClassMethodActor;
        private final int bci;

        private ClassMethodActorAddressLocation(TeleVM vm, RemoteCodePointer codePointer, TeleClassMethodActor teleClassMethodActor, int bci, String description) {
            super(vm, description);
            TeleError.check(codePointer != null);
            TeleError.check(teleClassMethodActor != null);
            TeleError.check(bci >= -1);
            this.codePointer = codePointer;
            this.teleClassMethodActor = teleClassMethodActor;
            this.bci = bci;
        }

        public boolean hasTeleClassMethodActor() {
            return true;
        }

        public TeleClassMethodActor teleClassMethodActor() {
            return teleClassMethodActor;
        }

        @Override
        public RemoteCodePointer codePointer() {
            return codePointer;
        }

    }

    /**
     * A code location specified as the entry of a method known to be compiled into the
     * boot image, identified by an annotation in the VM source code and made available
     * with static accessors.
     * <p>
     * The location corresponds to the beginning of the compiled method prologue, which
     * is equivalent to a bytecode position specification of -1.
     */
    static final class MethodAccessLocation extends MachineCodeLocation {

        private final TeleMethodAccess teleMethodAccess;
        private volatile RemoteCodePointer codePointer = null;
        private volatile TeleClassMethodActor teleClassMethodActor = null;

        private MethodAccessLocation(TeleVM vm, TeleMethodAccess teleMethodAccess, String description) {
            super(vm, description);
            TeleError.check(teleMethodAccess != null);
            this.teleMethodAccess = teleMethodAccess;
        }

        public boolean hasTeleClassMethodActor() {
            return teleClassMethodActor() != null;
        }

        public TeleClassMethodActor teleClassMethodActor() {
            if (teleClassMethodActor == null) {
                if (vm().tryLock()) {
                    try {
                        teleClassMethodActor = teleMethodAccess.teleClassMethodActor();
                    } finally {
                        vm().unlock();
                    }
                }
            }
            return teleClassMethodActor;
        }

        @Override
        public RemoteCodePointer codePointer() {
            if (codePointer == null && teleClassMethodActor() != null) {
                if (vm().tryLock()) {
                    try {
                        final TeleTargetMethod javaTargetMethod = teleClassMethodActor().getCurrentCompilation();
                        if (javaTargetMethod != null) {
                            final Address callEntryPoint = javaTargetMethod.callEntryPoint();
                            if (callEntryPoint != null && callEntryPoint.isNotZero()) {
                                codePointer = vm().machineCode().makeCodePointer(callEntryPoint);
                            }
                        }
                    } finally {
                        vm().unlock();
                    }
                }
            }
            return codePointer;
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
            final RemoteCodePointer codePointer = vm().machineCode().makeCodePointer(address);
            return new AddressCodeLocation(vm(), codePointer, description);
        }

        public MachineCodeLocation createMachineCodeLocation(Address address, TeleClassMethodActor teleClassMethodActor, int bci, String description) throws TeleError {
            final RemoteCodePointer codePointer = vm().machineCode().makeCodePointer(address);
            return new ClassMethodActorAddressLocation(vm(), codePointer, teleClassMethodActor, bci, description);
        }

        public MachineCodeLocation createMachineCodeLocation(RemoteCodePointer codePointer, String description) throws TeleError {
            return new AddressCodeLocation(vm(), codePointer, description);
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
//
//    private static HashMap<RemoteCodePointer, WeakReference<MachineCodeLocation> > pointerToLocation = new HashMap<RemoteCodePointer, WeakReference<MachineCodeLocation> >();
//
//
//    private static class Count implements Comparable<Count> {
//        String string;
//        int value;
//
//        @Override
//        public int compareTo(Count o) {
//            return value - o.value;
//        }
//    }
//
//    static HashMap<String, Count> locationsPerString = new HashMap<String, Count>() {
//
//        @Override
//        public Count get(Object key) {
//            Count count = super.get(key);
//            if (count == null) {
//                count = new Count();
//                count.string = (String) key;
//                put((String) key, count);
//            }
//            return count;
//        }
//    };
//
//    static {
//        if (Trace.hasLevel(1)) {
//            Runtime.getRuntime().addShutdownHook(new Thread("LocationsPerDescriptionPrinter") {
//
//                @Override
//                public void run() {
//                    SortedSet<Count> set = new TreeSet<Count>(locationsPerString.values());
//                    System.out.println("Machine code locations created (by description):");
//                    for (Count c : set) {
//                        System.out.println("    " + c.value + "\t" + c.string);
//                    }
//
//
//                    int count = 0;
//                    for (WeakReference<MachineCodeLocation> weakRef : pointerToLocation.values()) {
//                        if (weakRef.get() != null) {
//                            count++;
//                        }
//                    }
//                    System.out.println("Total Locations=" + pointerToLocation.size() + ", active=" + count);
//                }
//            });
//        }
//    }

    }
}
