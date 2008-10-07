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
/*VCSID=d2959727-778e-45c0-bbd2-dc3ecd148010*/
package com.sun.max.asm.gen.cisc.x86;

import com.sun.max.collect.*;

/**
 * @author Bernd Mathiske
 */
public interface ModRMGroup {

    public enum Opcode {
        _0, _1, _2, _3, _4, _5, _6, _7;

        public byte byteValue() {
            return (byte) ordinal();
        }

        public static final IndexedSequence<Opcode> VALUES = new ArraySequence<Opcode>(values());
    }

    ModRMDescription getInstructionDescription(Opcode opcode);
}
