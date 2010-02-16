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

import com.sun.max.asm.gen.*;

/**
 * A label for an absolute address.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class DisassembledLabel {

    private final DisassembledObject disassembledObject;
    private final ImmediateArgument address;

    public DisassembledLabel(Object addressOrDisassembledObject, String name) {
        if (addressOrDisassembledObject instanceof ImmediateArgument) {
            address = (ImmediateArgument) addressOrDisassembledObject;
            disassembledObject = null;
        } else {
            address = null;
            disassembledObject = (DisassembledObject) addressOrDisassembledObject;
        }
        this.name = name;
    }

    private final String name;

    public String name() {
        return name;
    }

    /**
     * Gets the disassembled object (if any) denoted by this label.
     */
    public DisassembledObject target() {
        return disassembledObject;
    }

    public ImmediateArgument address() {
        return disassembledObject != null ? disassembledObject.startAddress() : address;
    }
}
