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
package com.sun.max.asm.gen.cisc.x86;

/**
 * Addressing methods for operands. Refer to "Appendix A.1 Opcode-Syntax Notation".
 *
 * @author Bernd Mathiske
 */
public enum AddressingMethodCode  {

    A, // Far pointer is encoded in the instruction
    C, // Control register specified by the ModRM reg field
    D, // Debug register specified by the ModRM reg field
    E, // General purpose register or memory operand specified by the ModRM byte. Memory addresses can be computed from a segment register, SIB byte, and/or displacement.
    F, // rFLAGS register
    G, // General purpose register specified by the ModRM reg field.
    I, // Immediate value
    IC, // we made this one up, it's like I, but with parameter type AMD64XMMComparison
    J, // The instruction includes a relative offset that is added to the rIP.
    M, // A memory operand specified by the ModRM byte.
    N, // we made this one up, it's like G, but with ParameterPlace.OPCODE1/2{_REXB} instead of a ModRM field
    O, // The offset of an operand is encoded in the instruction. There is no ModRM byte in the instruction. Complex addressing using the SIB byte cannot be done.
    P, // 64-bit MMX register specified by the ModRM reg field.
    PR, // 64-bit MMX register specified by the ModRM r/m field. The ModRM mod field must be 11b.
    Q, // 64-bit MMX-register or memory operand specified by the ModRM byte. Memory addresses can be computed from a segment register, SIB byte, and/or displacement.
    R, // General purpose register specified by the ModRM r/m field. The ModRM mod field must be 11b.
    S, // Segment register specified by the ModRM reg field.
    T,
    V, // 128-bit XMM register specified by the ModRM reg field.
    VR, // 128-bit XMM register specified by the ModRM r/m field. The ModRM mod field must be 11b.
    W, // A 128-bit XMM register or memory operand specified by the ModRM byte. Memory addresses can be computed from a segment register, SIB byte, and/or displacement.
    X, // A memory operand addressed by the DS.rSI registers. Used in string instructions.
    Y; // A memory operand addressed by the ES.rDI registers. Used in string instructions.
}
