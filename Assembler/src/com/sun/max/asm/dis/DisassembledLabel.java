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

import com.sun.max.collect.*;

/**
 * A label deduced from one or more disassembled instructions.
 *
 * @author Bernd Mathiske
 */
public class DisassembledLabel {

    private final int _instructionIndex;

    public DisassembledLabel(int instructionIndex) {
        super();
        _instructionIndex = instructionIndex;
    }

    public int instructionIndex() {
        return _instructionIndex;
    }

    public static final String PREFIX = "L";

    private int _serial = -1;

    public void setSerial(int index) {
        _serial = index;
    }

    public String name() {
        return PREFIX + _serial;
    }

    private int _position = -1;

    public void bind(int position) {
        _position = position;
    }

    public int position() {
        return _position;
    }

    public static DisassembledLabel positionToLabel(int position, Sequence<DisassembledLabel> labels) {
        for (DisassembledLabel label : labels) {
            if (label.position() == position) {
                return label;
            }
        }
        return null;
    }
}
