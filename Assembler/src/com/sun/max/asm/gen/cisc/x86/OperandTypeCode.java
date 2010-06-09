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
 * Refer to "Appendix A.1 Opcode-Syntax Notation".
 *
 * @author Bernd Mathiske
 */
public enum OperandTypeCode {

    /**
     * Two 16-bit or 32-bit memory operands, depending on the effective operand size. Used in the BOUND instruction.
     */
    a,

    /**
     * A byte, irrespective of the effective operand size.
     */
    b,

    /**
     * A doubleword (32 bits), irrespective of the effective operand size.
     */
    d,

    /**
     * A double-quadword (128 bits), irrespective of the effective operand size.
     */
    dq,

    /**
     * ???
     */
    d_q,

    /**
     * A 32-bit or 48-bit far pointer, depending on the effective operand size.
     */
    p,

    /**
     * A 128-bit double-precision floating-point vector operand (packed double).
     */
    pd,

    /**
     * A 128-bit single-precision floating-point vector operand (packed single).
     */
    ps,

    /**
     * A quadword, irrespective of the effective operand size.
     */
    q,

    /**
     * A 6-byte or 10-byte pseudo-descriptor.
     */
    s,

    /**
     * A scalar double-precision floating-point operand (scalar double).
     */
    sd,

    /**
     * A scalar single-precision floating-point operand (scalar single).
     */
    ss,

    /**
     * A word, doubleword, or quadword, depending on the effective operand size.
     */
    v,

    /**
     * A word, irrespective of the effective operand size.
     */
    w,

    /**
     * A double word if operand size 32, a quad word if 64, undefined if 16.
     */
    y,

    /**
     * A word if the effective operand size is 16 bits, or a doubleword if the effective operand size is 32 or 64 bits.
     */
    z;

}
