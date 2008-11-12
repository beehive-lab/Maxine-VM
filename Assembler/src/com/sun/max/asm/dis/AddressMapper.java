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
package com.sun.max.asm.dis;

import java.util.*;

import com.sun.max.asm.gen.*;
import com.sun.max.collect.*;

/**
 * A facility for mapping addresses to {@linkplain DisassembledLabel labels} and {@linkplain DisassembledObject
 * disassembled objects}. Labels are only used to identify addresses that are a) explicitly targeted by an operand of a
 * disassembled object {@linkplain #add(DisassembledObject) added} to the map or b) explicitly
 * {@linkplain #add(ImmediateArgument, String) added}.
 *
 * @author Doug Simon
 */
public class AddressMapper {

    private int _serial;
    private final Map<ImmediateArgument, DisassembledObject> _objectMap = new HashMap<ImmediateArgument, DisassembledObject>();
    private final Map<ImmediateArgument, DisassembledLabel> _labelMap = new HashMap<ImmediateArgument, DisassembledLabel>();

    /**
     * Gets the label for a given address.
     *
     * @param address the address for which a label is requested
     * @return the label for {@code address} or null if no label exists for that address
     */
    public DisassembledLabel labelAt(ImmediateArgument address) {
        return _labelMap.get(address);
    }

    /**
     * Gets the label for a given disassembled object.
     *
     * @param disassembledObject the disassembled object for which a label is requested
     * @return the label corresponding to {@code disassembledObject}'s start address or null if no label exists for that address
     */
    public DisassembledLabel labelAt(DisassembledObject disassembledObject) {
        return labelAt(disassembledObject.startAddress());
    }

    /**
     * Adds a mapping from a given address to a given name.
     *
     * @return the previous mapping (if any) for {@code address}
     */
    public DisassembledLabel add(ImmediateArgument address, String name) {
        return _labelMap.put(address, new DisassembledLabel(address, name));
    }

    /**
     * Adds a mapping for the address {@linkplain DisassembledObject#targetAddress() targeted} by a given disassembled object.
     * If {@code disassembledObject} does not target another object, then no update is made to this mapping.
     *
     * @param disassembledObject the disassembled object to consider
     */
    public void add(DisassembledObject disassembledObject) {
        add(new ArraySequence<DisassembledObject>(new DisassembledObject[] {disassembledObject}));
    }

    /**
     * Adds a mapping for the addresses {@linkplain DisassembledObject#targetAddress() targeted} by a given sequence of
     * disassembled objects. If none of {@code disassembledObjects} target other objects, then no update is made to
     * this mapping.
     *
     * @param disassembledObjects the disassembled objects to consider
     */
    public void add(Sequence<DisassembledObject> disassembledObjects) {
        for (DisassembledObject disassembledObject : disassembledObjects) {
            final ImmediateArgument address = disassembledObject.startAddress();
            _objectMap.put(address, disassembledObject);
        }
        for (DisassembledObject disassembledObject : disassembledObjects) {
            final ImmediateArgument targetAddress = disassembledObject.targetAddress();
            if (targetAddress != null) {
                final DisassembledObject targetDisassembledObject = _objectMap.get(targetAddress);
                if (targetDisassembledObject == null) {
                    _labelMap.remove(targetAddress);
                } else {
                    DisassembledLabel label = _labelMap.get(targetAddress);
                    if (label == null || label.target() != targetDisassembledObject) {
                        label = new DisassembledLabel(targetDisassembledObject, "L" + _serial++);
                        _labelMap.put(targetAddress, label);
                    }
                }
            }
        }
    }

    /**
     * Computes the maximum length of any label name in this mapping.
     */
    public int maximumLabelNameLength() {
        int max = 0;
        for (DisassembledLabel label : _labelMap.values()) {
            final String name = label.name();
            if (name.length() > max) {
                max = name.length();
            }
        }
        return max;
    }
}
