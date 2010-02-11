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
package com.sun.max.tele.method;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;

// TODO (mlvdv) extend code location to represent source code
// TODO (mlvdv) extend code location to represent ranges of instructions
// TODO (mlvdv) complete instruction mapping code bytecode <-> target code

/**
 * Describes a location in VM code:  compiled target code, bytecode, source code, or some
 * combination that represents an equivalent location in each.  An instance may
 * not have all kinds of information. Some kinds of additional information cannot be
 * determined until an initial reading of the VM state has been completed.  Some kinds
 * of information cannot be determined until a specified class is loaded into the VM.
 * <br>
 * A location with an address (subclasses of {@link CompiledCodeLocation}  is assumed
 * to refer uniquely to a single compilation of a method,
 * where the actual method may or (rarely) not be known.
 * <br>
 * A location originally specified without an address (subclasses of {@link MethodCodeLocation})
 * is assumed to refer only to the method, and not to any particular compilation.
 *
 * @author Michael Van De Vanter
 */
public abstract class CodeLocation extends AbstractTeleVMHolder implements MaxCodeLocation {

    /**
     * Creates a code location in the VM specified only by an abstract description a method, which may not
     * even have been loaded yet into the VM.  No explicit position information is given, so the implied position
     * is instruction 0, the method entry. When requested, attempts will be made to locate the surrogate
     * for the {@link ClassMethodActor} in the VM that identifies the method, once the class has been loaded.
     * <br>
     * Thread-safe
     *
     * @param teleVM the VM
     * @param methodKey an abstract description of a method
     * @param description a human-readable description, suitable for a menu or for debugging
     * @return a new location
     */
    public static MethodCodeLocation createMethodLocation(TeleVM teleVM, MethodKey methodKey, String description) {
        return new MethodKeyLocation(teleVM, methodKey, description);
    }

    /**
     * Creates a code location in the VM specified as a position in the bytecodes representation of a method
     * in a class loaded in the VM.
     * <br>
     * Thread-safe
     *
     * @param teleVM the VM
     * @param teleClassMethodActor surrogate for a {@link ClassMethodActor} in the VM that identifies a method.
     * @param position offset into the method's bytecodes of a bytecode instruction
     * @param description a human-readable description, suitable for a menu or for debugging
     * @return a new location
     */
    public static MethodCodeLocation createMethodLocation(TeleVM teleVM, TeleClassMethodActor teleClassMethodActor, int position, String description) {
        return new ClassMethodActorLocation(teleVM, teleClassMethodActor, position, description);
    }

    /**
     * Creates a code location in VM specified as the memory address of a compiled target code instruction.
     * <br>
     * Thread-safe
     *
     * @param teleVM the VM
     * @param address an address in VM memory that represents the beginning of a compiled target code instruction
     * @param description a human-readable description, suitable for a menu or for debugging
     * @return a newly created location
     * @throws ProgramError if the address is null or zero
     */
    public static AddressCodeLocation createCompiledLocation(TeleVM teleVM, Address address, String description) throws ProgramError {
        ProgramError.check(address != null && !address.isZero());
        return new AddressCodeLocation(teleVM, address, description);
    }

    /**
     * Creates a code location in the VM based on both a classfile and compiled code description:
     * a position in the bytecodes representation of a method in a class loaded in the VM, in addition
     * to the memory address of the corresponding target code instruction in a specific compilation
     * of the method.
     * <br>
     * Thread-safe
     *
     * @param teleVM the VM
     * @param address an address in VM memory that represents the beginning of a compiled target code instruction
     * @param teleClassMethodActor surrogate for a {@link ClassMethodActor} in the VM that identifies a method.
     * @param position offset into the method's bytecodes of a bytecode instruction
     * @param description a human-readable description, suitable for a menu or for debugging
     * @return a new location
     * @throws ProgramError  if the address is null or zero
     */
    public static CompiledCodeLocation createCompiledLocation(TeleVM teleVM, Address address, TeleClassMethodActor teleClassMethodActor, int position, String description) throws ProgramError {
        ProgramError.check(address != null && !address.isZero());
        return new ClassMethodActorAddressLocation(teleVM, address, teleClassMethodActor, position, description);
    }

    /**
     * Creates a code location in the VM specified by a predefined method accessor for methods in
     * the boot image.  Resolution of the accessor into other VM-related information is delayed, so that
     * these location instances can be created without any other VM-related services, early in the
     * startup cycle.
     *
     * @param teleVM the VM
     * @param teleMethodAccess a statically defined accessor for a specially marked method in VM code
     * @param description a human-readable description, suitable for a menu or for debugging
     * @return a new location
     */
    public static CompiledCodeLocation create(TeleVM teleVM, TeleMethodAccess teleMethodAccess, String description) {
        return new MethodAccessLocation(teleVM, teleMethodAccess, description);
    }

    private final String description;

    protected CodeLocation(TeleVM teleVM, String description) {
        super(teleVM);
        this.description = description;
    }

    public final String description() {
        return description;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + (hasAddress() ? (" 0x" + address().toHexString()) : "")
                        + (hasBytecodeLocation() ? (" " + bytecodeLocation()) : "") + " }";
    }

    /**
     * Attempt to create a method methodKey for a method located in the VM.  Null if
     * the information cannot be determined at present, for example if the VM is busy.
     *
     * @param bytecodeLocation location in a method's bytecode, specified by loaded
     * method description in the VM and a position offset in bytes.
     * @return a new method methodKey describing the method
     */
    protected MethodKey bytecodeLocationToKey(MaxBytecodeLocation bytecodeLocation) {
        assert bytecodeLocation != null;
        if (!teleVM().tryLock()) {
            return null;
        }
        try {
            return new MethodKey.DefaultMethodKey(bytecodeLocation.teleClassMethodActor().methodActor());
        } finally {
            teleVM().unlock();
        }
    }

    /**
     * Attempt to determine the target code address of the method entry in the first
     * compilation of the specified method.  Always identifies the method entry,
     * ignoring any position information in the specified location.
     *
     * @param location in a method's bytecode, specified by loaded
     * method description in the VM; any position is ignored, treated as 0.
     * @return a non-zero address, null if not available
     */
    protected Address bytecodeLocationToFirstCompiledEntry(MaxBytecodeLocation bytecodeLocation) {
        assert bytecodeLocation != null;
        if (!teleVM().tryLock()) {
            return null;
        }
        try {
            final TeleTargetMethod javaTargetMethod = bytecodeLocation.teleClassMethodActor().getJavaTargetMethod(0);
            if (javaTargetMethod != null) {
                return javaTargetMethod.callEntryPoint();
            }
        } finally {
            teleVM().unlock();
        }
        return null;
    }

    /**
     * Attempt to locate bytecode location information for a method loaded in the VM, based
     * on an abstraction description of the method.  Null if the method is not known to be loaded,
     * or if the information cannot be determined at present, for example if the VM is busy.
     *
     * @param methodKey an abstract specification of a method, with a bytecode offset
     * @return a description of the location expressed in terms of the method loaded in the VM
     */
    protected MaxBytecodeLocation keyToBytecodeLocation(MethodKey methodKey) {
        if (!teleVM().tryLock()) {
            return null;
        }
        try {
            final TeleClassActor teleClassActor = teleVM().findTeleClassActor(methodKey.holder());
            if (teleClassActor != null) {
                // find a matching method
                final String methodKeyString = methodKey.signature().toJavaString(true, true);
                for (TeleMethodActor teleMethodActor : teleClassActor.getTeleMethodActors()) {
                    if (teleMethodActor instanceof TeleClassMethodActor) {
                        if (teleMethodActor.methodActor().descriptor().toJavaString(true, true).equals(methodKeyString)) {
                            final TeleClassMethodActor teleClassMethodActor = (TeleClassMethodActor) teleMethodActor;
                            return new TeleBytecodeLocation(teleClassMethodActor, 0);
                        }
                    }
                }
            }
            // TODO (mlvdv) when the class registry is complete, this should not be necessary
            // Try to locate TeleClassMethodActor via compiled methods in the tele VM.
            final Sequence<TeleTargetMethod> teleTargetMethods = TeleTargetMethod.get(teleVM(), methodKey);
            if (teleTargetMethods.length() > 0) {
                final TeleClassMethodActor teleClassMethodActor = teleTargetMethods.first().getTeleClassMethodActor();
                return new TeleBytecodeLocation(teleClassMethodActor, 0);
            }
            return null;
        } finally {
            teleVM().unlock();
        }
    }

    // TODO (mlvdv)  attempt to map target code to bytecode location, at least for JIT methods.
    /**
     * Attempt to locate bytecode location information for the method whose compilation
     * includes a target code instruction at a given address.
     * <br>
     * Note that in many cases it will not be possible to determine the actual corresponding
     * bytecode instruction, in which case the position will be 0 (method entry).
     * <br>
     * <strong>Unimplemented:</strong> No attempt is made at present to compute a
     * bytecode instruction position corresponding to the target code location, although this
     * could be done for JIT methods.
     *
     * @param address an address in the VM, possibly the location of a target code instruction
     * in the compilation of a method.
     * @return  a description of the location expressed in terms of the method loaded in the VM
     */
    protected MaxBytecodeLocation addressToBytecodeLocation(Address address) {
        if (!teleVM().tryLock()) {
            return null;
        }
        try {
            final TeleTargetMethod teleTargetMethod = teleVM().makeTeleTargetMethod(address);
            if (teleTargetMethod != null) {
                final TeleClassMethodActor teleClassMethodActor = teleTargetMethod.getTeleClassMethodActor();
                if (teleClassMethodActor != null) {
                    return new TeleBytecodeLocation(teleClassMethodActor, 0);
                }
            }
        } finally {
            teleVM().unlock();
        }
        return null;
    }

    /**
     * Attempt to locate bytecode location information for a method known to be loaded and compiled
     * into the boot image.  This should always succeed if not called too early in the startup cycle, and if
     * the VM is not busy.
     *
     * @param teleMethodAccess a static method accessor produced by annotation of distinguished VM methods.
     * @return
     */
    protected MaxBytecodeLocation methodAccessToBytecodeLocation(TeleMethodAccess teleMethodAccess) {
        if (!teleVM().tryLock()) {
            return null;
        }
        try {
            final TeleClassMethodActor teleClassMethodActor = teleMethodAccess.teleClassMethodActor();
            if (teleClassMethodActor != null) {
                return new TeleBytecodeLocation(teleClassMethodActor, 0);
            }
        } finally {
            teleVM().unlock();
        }
        return null;
    }

    /**
     * A code location that refers to a method in general, and not to any specific compilation.
     *
     * @author Michael Van De Vanter
     */
    public abstract static class MethodCodeLocation extends CodeLocation {

        protected MethodCodeLocation(TeleVM teleVM, String description) {
            super(teleVM, description);
        }

        public boolean isSameAs(MaxCodeLocation codeLocation) {
            if (codeLocation instanceof MethodCodeLocation) {
                if (hasKey()) {
                    return methodKey().equals(codeLocation.methodKey());
                }
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
     * @author Michael Van De Vanter
     */
    public abstract static class CompiledCodeLocation extends CodeLocation {

        /**
         * A code location that refers to a method in general, and not to any specific compilation.
         *
         * @author Michael Van De Vanter
         */
        public abstract static class MethodCodeLocation extends CodeLocation {

            protected MethodCodeLocation(TeleVM teleVM, String description) {
                super(teleVM, description);
            }

            public boolean isSameAs(MaxCodeLocation codeLocation) {
                if (codeLocation instanceof MethodCodeLocation) {
                    if (hasKey()) {
                        return methodKey().equals(codeLocation.methodKey());
                    }
                }
                return false;
            }

        }

        protected CompiledCodeLocation(TeleVM teleVM, String description) {
            super(teleVM, description);
        }

        public boolean isSameAs(MaxCodeLocation codeLocation) {
            if (codeLocation instanceof CompiledCodeLocation) {
                if (hasAddress()) {
                    return address().equals(codeLocation.address());
                }
            }
            return false;
        }
    }

    /**
     * A code location in the VM specified by an abstract description of a method, which may
     * not be loaded in the VM.  The implied position in the method is 0, the method entry.
    * It is not bound to any particular compilation, and so never had a target code address.
     *
     * @see CodeLocation#createMethodLocation(TeleVM, MethodKey, String)
     * @author Michael Van De Vanter
     */
    private static final class MethodKeyLocation extends MethodCodeLocation {

        private final MethodKey methodKey;
        private volatile MaxBytecodeLocation bytecodeLocation = null;

        public MethodKeyLocation(TeleVM teleVM, MethodKey methodKey, String description) {
            super(teleVM, description);
            assert methodKey != null;
            this.methodKey = methodKey;
        }

        public boolean hasAddress() {
            return false;
        }

        public Address address() {
            return Address.zero();
        }

        public boolean hasBytecodeLocation() {
            tryCreateBytecodeLocation();
            return bytecodeLocation != null;
        }

        public MaxBytecodeLocation bytecodeLocation() {
            tryCreateBytecodeLocation();
            return bytecodeLocation;
        }

        public boolean hasKey() {
            return true;
        }

        public MethodKey methodKey() {
            return methodKey;
        }

        private void tryCreateBytecodeLocation() {
            if (bytecodeLocation == null) {
                bytecodeLocation = keyToBytecodeLocation(methodKey);
            }
        }
    }

    /**
     * A code location in the VM specified only as a bytecode position in a loaded classfile.
     * It is not bound to any particular compilation, and so never had a target code address.
     *
     * @see #createMethodLocation(TeleVM, TeleClassMethodActor, int, String)
     * @author Michael Van De Vanter
     */
    private static final class ClassMethodActorLocation extends MethodCodeLocation {

        private final TeleBytecodeLocation bytecodeLocation;
        private volatile MethodKey methodKey = null;

        public ClassMethodActorLocation(TeleVM teleVM, TeleClassMethodActor teleClassMethodActor, int position, String description) {
            super(teleVM, description);
            assert teleClassMethodActor != null;
            assert position >= 0;
            this.bytecodeLocation = new TeleBytecodeLocation(teleClassMethodActor, position);
        }

        public boolean hasAddress() {
            return false;
        }

        public Address address() {
            return Address.zero();
        }

        public boolean hasBytecodeLocation() {
            return true;
        }

        public MaxBytecodeLocation bytecodeLocation() {
            return bytecodeLocation;
        }

        public boolean hasKey() {
            tryCreateKey();
            return methodKey != null;
        }

        public MethodKey methodKey() {
            tryCreateKey();
            return methodKey;
        }

        private void tryCreateKey() {
            if (methodKey == null) {
                methodKey = bytecodeLocationToKey(bytecodeLocation);
            }
        }

    }

    /**
     * A code location in the VM specified only as an address in compiled code.
     *
     * @see #createCompiledLocation(TeleVM, Address, String)
     * @author Michael Van De Vanter
     */
    private static final class AddressCodeLocation extends CompiledCodeLocation {

        private final Address address;
        private volatile MaxBytecodeLocation bytecodeLocation = null;
        private volatile MethodKey methodKey = null;

        public AddressCodeLocation(TeleVM teleVM, Address address, String description) {
            super(teleVM, description);
            this.address = address;
        }

        public boolean hasAddress() {
            return true;
        }

        public Address address() {
            return address;
        }

        public boolean hasBytecodeLocation() {
            tryCreateBytecodeLocation();
            return bytecodeLocation != null;
        }

        public MaxBytecodeLocation bytecodeLocation() {
            tryCreateBytecodeLocation();
            return bytecodeLocation;
        }

        public boolean hasKey() {
            tryCreateKey();
            return methodKey != null;
        }

        public MethodKey methodKey() {
            tryCreateKey();
            return methodKey;
        }

        private void tryCreateBytecodeLocation() {
            if (bytecodeLocation == null) {
                bytecodeLocation = addressToBytecodeLocation(address);
            }
        }

        private void tryCreateKey() {
            if (methodKey == null) {
                tryCreateBytecodeLocation();
                if (bytecodeLocation != null) {
                    methodKey = bytecodeLocationToKey(bytecodeLocation);
                }
            }
        }

    }

    /**
     * A code location in the VM specified both as a bytecode position in a loaded classfile and the
     * memory location of the corresponding target code instruction in a compilation of the method.
     *
     * @see #createCompiledLocation(TeleVM, Address, TeleClassMethodActor, int, String)
     * @author Michael Van De Vanter
     */
    private static final class ClassMethodActorAddressLocation extends CompiledCodeLocation {

        private final Address address;
        private final TeleBytecodeLocation bytecodeLocation;
        private volatile MethodKey methodKey = null;

        public ClassMethodActorAddressLocation(TeleVM teleVM, Address address, TeleClassMethodActor teleClassMethodActor, int position, String description) {
            super(teleVM, description);
            assert address != null;
            this.address = address;
            assert teleClassMethodActor != null;
            assert position >= 0;
            this.bytecodeLocation = new TeleBytecodeLocation(teleClassMethodActor, position);
        }

        public boolean hasAddress() {
            return true;
        }

        public Address address() {
            return address;
        }

        public boolean hasBytecodeLocation() {
            return true;
        }

        public MaxBytecodeLocation bytecodeLocation() {
            return bytecodeLocation;
        }

        public boolean hasKey() {
            tryCreateKey();
            return methodKey != null;
        }

        public MethodKey methodKey() {
            tryCreateKey();
            return methodKey;
        }

        private void tryCreateKey() {
            if (methodKey == null) {
                methodKey = bytecodeLocationToKey(bytecodeLocation);
            }
        }
    }

    /**
     * A code location specified as the entry of a method known to be compiled into the
     * boot image, identified by an annotation in the VM source code and made available
     * with static accessors.
     *
     * @author Michael Van De Vanter
     */
    private static final class MethodAccessLocation extends CompiledCodeLocation {

        private final TeleMethodAccess teleMethodAccess;
        private volatile MaxBytecodeLocation bytecodeLocation = null;
        private volatile Address address = null;
        private volatile MethodKey methodKey = null;

        public MethodAccessLocation(TeleVM teleVM, TeleMethodAccess teleMethodAccess, String description) {
            super(teleVM, description);
            assert teleMethodAccess != null;
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

        public boolean hasBytecodeLocation() {
            tryCreateBytecodeLocation();
            return bytecodeLocation != null;
        }

        public MaxBytecodeLocation bytecodeLocation() {
            tryCreateBytecodeLocation();
            return bytecodeLocation;
        }

        public boolean hasKey() {
            tryCreateKey();
            return methodKey != null;
        }

        public MethodKey methodKey() {
            tryCreateKey();
            return methodKey;
        }

        private void tryCreateAddress() {
            if (address == null) {
                tryCreateBytecodeLocation();
                if (bytecodeLocation != null) {
                    address = bytecodeLocationToFirstCompiledEntry(bytecodeLocation);
                }
            }
        }

        private void tryCreateBytecodeLocation() {
            if (bytecodeLocation == null) {
                bytecodeLocation = methodAccessToBytecodeLocation(teleMethodAccess);
            }
        }

        private void tryCreateKey() {
            if (methodKey == null) {
                tryCreateBytecodeLocation();
                if (bytecodeLocation != null) {
                    methodKey = bytecodeLocationToKey(bytecodeLocation);
                }
            }
        }

    }

}
