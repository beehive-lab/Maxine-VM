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
package com.sun.max.vm.cps.dir;

import com.sun.cri.bytecode.*;
import com.sun.max.vm.cps.dir.transform.*;

/**
 * An instruction location with Java frame descriptor.
 *
 * @author Bernd Mathiske
 */
public final class DirInfopoint extends DirInstruction {

    public final DirJavaFrameDescriptor javaFrameDescriptor;
    public final int opcode;
    public final DirVariable destination;

    public DirJavaFrameDescriptor javaFrameDescriptor() {
        return javaFrameDescriptor;
    }

    public DirInfopoint(DirVariable dst, DirJavaFrameDescriptor javaFrameDescriptor, int opcode) {
        this.destination = dst;
        this.javaFrameDescriptor = javaFrameDescriptor;
        this.opcode = opcode;
    }

    @Override
    public boolean isEquivalentTo(DirInstruction other, DirBlockEquivalence dirBlockEquivalence) {
        if (other instanceof DirInfopoint) {
            final DirInfopoint dirInfopoint = (DirInfopoint) other;
            if (javaFrameDescriptor == null) {
                return dirInfopoint.javaFrameDescriptor == null;
            }
            return javaFrameDescriptor.equals(dirInfopoint.javaFrameDescriptor) && opcode == dirInfopoint.opcode && destination == dirInfopoint.destination;
        }
        return false;
    }

    @Override
    public String toString() {
        return (destination == null ? "" : destination + " := ") + "infopoint[" + Bytecodes.nameOf(opcode) + "] " + javaFrameDescriptor;
    }

    @Override
    public void acceptVisitor(DirVisitor visitor) {
        visitor.visitInfopoint(this);
    }
}
