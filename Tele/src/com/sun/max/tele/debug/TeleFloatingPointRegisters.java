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
package com.sun.max.tele.debug;

import com.sun.max.asm.amd64.*;
import com.sun.max.program.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;

/**
 * Encapsulates the values of the floating point registers for a tele native thread.
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class TeleFloatingPointRegisters extends TeleRegisters {

    public TeleFloatingPointRegisters(VMConfiguration vmConfiguration) {
        super(symbolizer(vmConfiguration), vmConfiguration);
    }

    /**
     * The Inspector's register panel only supports displaying a set of registers with values that are monotonically
     * increasing and/or that can have a width different from the word width. This doesn't work well with SPARC floating
     * pointer registers where single and double register partially overlap, and single precision floating point
     * register have a width smaller than the word width. For now, we simplify by having this class representing the
     * double-precision floating point registers with "value" that are monotonically increasing..
     *
     * @author Laurent Daynes
     */
    private enum SparcFloatingPointRegister implements Enumerable<SparcFloatingPointRegister> {
        F0, F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, F15, F16;

        public int value() {
            return ordinal();
        }
        
        @Override
        public String toString() {
        	final int o = ordinal();
        	final int regNum = o > 32 ? 32 + ((o-32) << 1) : o;
            return "F" + regNum;
        }

        static final Enumerator<SparcFloatingPointRegister>  ENUMERATOR = new Enumerator<SparcFloatingPointRegister>(SparcFloatingPointRegister.class);
        public Enumerator<SparcFloatingPointRegister> enumerator() {
            return ENUMERATOR;
        }

    }

    /**
     * Gets the symbols representing all the floating point registers of the instruction set denoted by a given VM
     * configuration.
     */
    public static Symbolizer<? extends Symbol> symbolizer(VMConfiguration vmConfiguration) {
        switch (vmConfiguration.platform().processorKind().instructionSet()) {
            case AMD64:
                return AMD64XMMRegister.ENUMERATOR;
            case SPARC:
                return SparcFloatingPointRegister.ENUMERATOR;
            default:
                Problem.unimplemented();
                return null;
        }
    }
}
