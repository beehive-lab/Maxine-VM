/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
