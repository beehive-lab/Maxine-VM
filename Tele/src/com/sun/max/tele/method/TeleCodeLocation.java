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
import com.sun.max.tele.*;
import com.sun.max.tele.debug.TeleBytecodeBreakpoint.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.type.*;

/**
 * Describes a location in the code in the {@link TeleVM}:  target code, bytecode, source code, or some
 * combination that represents an equivalent location in each.  An instance may
 * not have all kinds of information.
 * A selection may be a contiguous sequence of instructions.
 *
 * @author Michael Van De Vanter
 *
 */
public class TeleCodeLocation extends AbstractTeleVMHolder {
    // TODO (mlvdv) TeleCodeLocation is a crude place holder; replace with subclasses
    // TODO (mlvdv) TeleCodeLocation: map among location kinds; handle ranges; handle source locations

    private Address targetCodeInstructionAddress;

    private TeleBytecodeLocation teleBytecodeLocation; // Describes position in a loaded method

    private Key key;  // Describes a location intentionally, not necessarily loaded

    /**
     * Location expressed only in terms of a target code address.
     */
    public TeleCodeLocation(TeleVM teleVM, Address targetCodeInstructionAddress) {
        super(teleVM);
        this.targetCodeInstructionAddress = targetCodeInstructionAddress;
        this.teleBytecodeLocation = null;
        this.key = null;
    }

    /**
     * Location expressed only in terms of a bytecode location in the {@link TeleVM}.
     */
    public TeleCodeLocation(TeleVM teleVM, TeleClassMethodActor teleClassMethodActor, int position) {
        super(teleVM);
        this.targetCodeInstructionAddress = Address.zero();
        this.teleBytecodeLocation = new TeleBytecodeLocation(teleClassMethodActor, position);
        this.key = new Key(teleBytecodeLocation.bytecodeLocation());
    }

    /**
     * Location expressed in terms of both a target code address  in the {@link TeleVM} and a bytecode location.
     */
    public TeleCodeLocation(TeleVM teleVM, Address targetCodeInstructionAddress, TeleClassMethodActor teleClassMethodActor, int position) {
        super(teleVM);
        this.targetCodeInstructionAddress = targetCodeInstructionAddress;
        this.teleBytecodeLocation = new TeleBytecodeLocation(teleClassMethodActor, position);
        this.key = new Key(teleBytecodeLocation.bytecodeLocation());
    }

    /**
     * Location expressed in terms of an abstract method key.
     */
    public TeleCodeLocation(TeleVM teleVM, Key key) {
        super(teleVM);
        this.targetCodeInstructionAddress = Address.zero();
        this.teleBytecodeLocation = null;
        this.key = key;
    }

   /**
     * Is there a target code representation for the code location in the {@link TeleVM}.
     */
    public boolean hasTargetCodeLocation() {
        return !targetCodeInstructionAddress.isZero();
    }

    /**
     * Target code instruction pointer;
     * zero if no target code information about the location available.
     */
    public Address targetCodeInstructionAddress() {
        return targetCodeInstructionAddress;
    }

    /**
     * Does this location have a specific target code location?
     */
    public boolean isAtAddress(Address address) {
        return targetCodeInstructionAddress == null ? false : targetCodeInstructionAddress.equals(address);
    }

    /**
     * Is there a bytecode representation for the code location.
     */
    public boolean hasBytecodeLocation() {
        resolveBytecodeLocation();
        return teleBytecodeLocation != null;
    }

    public TeleBytecodeLocation teleBytecodeLocation() {
        return teleBytecodeLocation;
    }

    /**
     * Location relative to the bytecode representation in a loaded class
     * null if no bytecode information about the location is available or if the class hasn't been loaded.
     */
    public BytecodeLocation bytecodeLocation() {
        resolveBytecodeLocation();
        if (teleBytecodeLocation == null) {
            return null;
        }
        return teleBytecodeLocation.bytecodeLocation();
    }

    private void resolveBytecodeLocation() {
        if (teleBytecodeLocation == null && key != null) {
            // Location was specified by description; see if the referred to class has since been loaded
            // so that we can talk about bytecodes.
            // Look first in the class registry
            final TypeDescriptor holderTypeDescriptor = key().holder();
            final TeleClassActor teleClassActor = teleVM().findTeleClassActor(holderTypeDescriptor);
            if (teleClassActor != null) {
                // find a matching method
                final String methodKeyString = key.signature().toJavaString(true, true);
                for (TeleMethodActor teleMethodActor : teleClassActor.getTeleMethodActors()) {
                    if (teleMethodActor instanceof TeleClassMethodActor) {
                        if (teleMethodActor.methodActor().descriptor().toJavaString(true, true).equals(methodKeyString)) {
                            final TeleClassMethodActor teleClassMethodActor = (TeleClassMethodActor) teleMethodActor;
                            teleBytecodeLocation = new TeleBytecodeLocation(teleClassMethodActor, key.position());
                        }
                    }
                }
            }
            // TODO (mlvdv) when the class registry is complete, this should not be necessary
            if (teleBytecodeLocation == null) {
                // Try to locate TeleClassMethodActor via compiled methods in the tele VM.
                final Sequence<TeleTargetMethod> teleTargetMethods = TeleTargetMethod.get(teleVM(), key);
                if (teleTargetMethods.length() > 0) {
                    final TeleClassMethodActor teleClassMethodActor = teleTargetMethods.first().getTeleClassMethodActor();
                    teleBytecodeLocation = new TeleBytecodeLocation(teleClassMethodActor, key.position());
                }
            }
        }
    }

    public boolean hasKey() {
        return key != null;
    }

    /**
     * @return a key describing the method and bytecode location, independent of the loaded class.
     */
    public Key key() {
        return key;
    }

    @Override
    public String toString() {
        return "TeleCodeLocation{" + (hasTargetCodeLocation() ? (" 0x" + targetCodeInstructionAddress.toHexString()) : "")
                        + (hasBytecodeLocation() ? (" " + teleBytecodeLocation) : "") + " }";
    }

    // Crude notion of equality for now; revisit when things get more interesting.
    @Override
    public boolean equals(Object o) {
        if (o instanceof TeleCodeLocation) {
            final TeleCodeLocation other = (TeleCodeLocation) o;
            if (hasTargetCodeLocation()) {
                return targetCodeInstructionAddress.equals(other.targetCodeInstructionAddress);
            } else if (hasBytecodeLocation()) {
                return teleBytecodeLocation.equals(other.teleBytecodeLocation);
            } else if (hasKey()) {
                return key.equals(other.key);
            } else {
                // Must be a zero target code location
                return targetCodeInstructionAddress.equals(other.targetCodeInstructionAddress);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hasTargetCodeLocation()) {
            return targetCodeInstructionAddress.hashCode();
        } else if (hasBytecodeLocation()) {
            return teleBytecodeLocation.hashCode();
        } else if (hasKey()) {
            return key.hashCode();
        } else {
            // Must be a zero target code location
            return targetCodeInstructionAddress.hashCode();
        }
    }


}
