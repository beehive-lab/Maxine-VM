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
 * Describes a location in the code:  target code, bytecode, source code, or some
 * combination that represents an equivalent location in each.  An instance may
 * not have all kinds of information.
 * A selection may be a contiguous sequence of instructions.
 *
 * @author Michael Van De Vanter
 *
 */
public class TeleCodeLocation extends TeleVMHolder {
    // TODO (mlvdv) TeleCodeLocation is a crude place holder; replace with subclasses
    // TODO (mlvdv) TeleCodeLocation: map among location kinds; handle ranges; handle source locations

    private Address _targetCodeInstructionAddress;

    private TeleBytecodeLocation _teleBytecodeLocation; // Describes position in a loaded method

    private Key _key;  // Describes a location intentionally, not necessarily loaded

    /**
     * A null code location.
     */
    public TeleCodeLocation(TeleVM teleVM) {
        super(teleVM);
        _targetCodeInstructionAddress = Address.zero();
        _teleBytecodeLocation = null;
        _key = null;
    }

    /**
     * Location expressed only in terms of a target code address.
     */
    public TeleCodeLocation(TeleVM teleVM, Address targetCodeInstructionAddress) {
        super(teleVM);
        _targetCodeInstructionAddress = targetCodeInstructionAddress;
        _teleBytecodeLocation = null;
        _key = null;
    }

    /**
     * Location expressed only in terms of a bytecode location in the tele VM.
     */
    public TeleCodeLocation(TeleVM teleVM, TeleClassMethodActor teleClassMethodActor, int position) {
        super(teleVM);
        _targetCodeInstructionAddress = Address.zero();
        _teleBytecodeLocation = new TeleBytecodeLocation(teleClassMethodActor, position);
        _key = new Key(_teleBytecodeLocation.bytecodeLocation());
    }

    /**
     * Location expressed in terms of both a target code address and a bytecode location.
     */
    public TeleCodeLocation(TeleVM teleVM, Address targetCodeInstructionAddress, TeleClassMethodActor teleClassMethodActor, int position) {
        super(teleVM);
        _targetCodeInstructionAddress = targetCodeInstructionAddress;
        _teleBytecodeLocation = new TeleBytecodeLocation(teleClassMethodActor, position);
        _key = new Key(_teleBytecodeLocation.bytecodeLocation());
    }

    /**
     * Location expressed in terms of an abstract method key.
     */
    public TeleCodeLocation(TeleVM teleVM, Key key) {
        super(teleVM);
        _targetCodeInstructionAddress = Address.zero();
        _teleBytecodeLocation = null;
        _key = key;
    }

   /**
     * Is there a target code representation for the code location.
     */
    public boolean hasTargetCodeLocation() {
        return !_targetCodeInstructionAddress.isZero();
    }

    /**
     * Location relative to the target code representation;
     * zero if no target code information about the location available.
     */
    public Address targetCodeInstructionAddresss() {
        return _targetCodeInstructionAddress;
    }

    /**
     * Is there a bytecode representation for the code location.
     */
    public boolean hasBytecodeLocation() {
        resolveBytecodeLocation();
        return _teleBytecodeLocation != null;
    }

    public TeleBytecodeLocation teleBytecodeLocation() {
        return _teleBytecodeLocation;
    }

    /**
     * Location relative to the bytecode representation in a loaded class
     * null if no bytecode information about the location is available or if the class hasn't been loaded.
     */
    public BytecodeLocation bytecodeLocation() {
        resolveBytecodeLocation();
        return _teleBytecodeLocation.bytecodeLocation();
    }

    private void resolveBytecodeLocation() {
        if (_teleBytecodeLocation == null && _key != null) {
            // Location was specified by description; see if the referred to class has since been loaded
            // so that we can talk about bytecodes.
            // Look first in the class registry
            final TypeDescriptor holderTypeDescriptor = key().holder();
            final TeleClassActor teleClassActor = teleVM().teleClassRegistry().findTeleClassActor(holderTypeDescriptor);
            if (teleClassActor != null) {
                // find a matching method
                final String methodKeyString = _key.signature().toJavaString(true, true);
                for (TeleMethodActor teleMethodActor : teleClassActor.getTeleMethodActors()) {
                    if (teleMethodActor instanceof TeleClassMethodActor) {
                        if (teleMethodActor.methodActor().descriptor().toJavaString(true, true).equals(methodKeyString)) {
                            final TeleClassMethodActor teleClassMethodActor = (TeleClassMethodActor) teleMethodActor;
                            _teleBytecodeLocation = new TeleBytecodeLocation(teleClassMethodActor, _key.position());
                        }
                    }
                }
            }
            // TODO (mlvdv) when the class registry is complete, this should not be necessary
            if (_teleBytecodeLocation == null) {
                // Try to locate TeleClassMethodActor via compiled methods in the tele VM.
                final Sequence<TeleTargetMethod> teleTargetMethods = TeleTargetMethod.get(teleVM(), _key);
                if (teleTargetMethods.length() > 0) {
                    final TeleClassMethodActor teleClassMethodActor = teleTargetMethods.first().getTeleClassMethodActor();
                    _teleBytecodeLocation = new TeleBytecodeLocation(teleClassMethodActor, _key.position());
                }
            }
        }
    }

    public boolean hasKey() {
        return _key != null;
    }

    /**
     * @return a key describing the method and bytecode location, independent of the loaded class.
     */
    public Key key() {
        return _key;
    }

    @Override
    public String toString() {
        return "TeleCodeLocation{" + (hasTargetCodeLocation() ? (" 0x" + _targetCodeInstructionAddress.toHexString()) : "")
                        + (hasBytecodeLocation() ? (" " + _teleBytecodeLocation) : "") + " }";
    }

    // Crude notion of equality for now; revisit when things get more insteresting.
    @Override
    public boolean equals(Object o) {
        if (o instanceof TeleCodeLocation) {
            final TeleCodeLocation other = (TeleCodeLocation) o;
            if (hasTargetCodeLocation()) {
                return _targetCodeInstructionAddress.equals(other._targetCodeInstructionAddress);
            } else if (hasBytecodeLocation()) {
                return _teleBytecodeLocation.equals(other._teleBytecodeLocation);
            } else if (hasKey()) {
                return _key.equals(other._key);
            } else {
                // Must be a zero target code location
                return _targetCodeInstructionAddress.equals(other._targetCodeInstructionAddress);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hasTargetCodeLocation()) {
            return _targetCodeInstructionAddress.hashCode();
        } else if (hasBytecodeLocation()) {
            return _teleBytecodeLocation.hashCode();
        } else if (hasKey()) {
            return _key.hashCode();
        } else {
            // Must be a zero target code location
            return _targetCodeInstructionAddress.hashCode();
        }
    }


}
