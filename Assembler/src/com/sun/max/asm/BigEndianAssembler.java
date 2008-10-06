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
/*VCSID=a6a14461-c5a6-4700-b7c4-c989a9f9ad7e*/
package com.sun.max.asm;


/**
 * @author Bernd Mathiske
 */
public abstract class BigEndianAssembler extends Assembler {

    protected BigEndianAssembler() {
        super();
    }

    @Override
    protected void emitShort(short shortValue) {
        emitByte((byte) (shortValue >> 8));
        emitByte((byte) (shortValue & 0xff));
    }

    @Override
    protected void emitInt(int intValue) {
        emitShort((short) (intValue >> 16));
        emitShort((short) (intValue & 0xffff));
    }

    @Override
    protected void emitLong(long longValue) {
        emitInt((int) (longValue >> 32));
        emitInt((int) (longValue & 0xffffffffL));
    }
}
