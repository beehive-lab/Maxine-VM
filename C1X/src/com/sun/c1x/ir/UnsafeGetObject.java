/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ir;

import com.sun.c1x.util.InstructionVisitor;
import com.sun.c1x.value.BasicType;

/**
 * The <code>UnsafeGetObject</code> instruction represents an unsafe read.
 *
 * @author Ben L. Titzer
 */
public class UnsafeGetObject extends UnsafeObjectOp {

    /**
     * Constructs a new UnsafeGetObject operation.
     * @param basicType the basic type of the operation
     * @param object the instruction generating the object
     * @param offset the instruction generating the offset
     * @param isVolatile <code>true</code> if this operation is volatile
     */
    public UnsafeGetObject(BasicType basicType, Instruction object, Instruction offset, boolean isVolatile) {
        super(basicType, object, offset, false, isVolatile);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitUnsafeGetObject(this);
    }
}
