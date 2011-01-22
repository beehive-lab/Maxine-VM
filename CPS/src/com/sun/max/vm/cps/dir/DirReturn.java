/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.vm.cps.dir.transform.*;

/**
 * Non-local exit from the current method.
 *
 * @author Bernd Mathiske
 */
public class DirReturn extends DirInstruction {

    private final DirValue returnValue;

    public DirReturn(DirValue returnValue) {
        this.returnValue = (returnValue == null) ? DirConstant.VOID : returnValue;
    }

    public DirValue returnValue() {
        return returnValue;
    }

    @Override
    public int hashCodeForBlock() {
        return super.hashCodeForBlock() ^ returnValue.hashCodeForBlock();
    }

    @Override
    public boolean isEquivalentTo(DirInstruction other, DirBlockEquivalence dirBlockEquivalence) {
        if (other instanceof DirReturn) {
            final DirReturn dirReturn = (DirReturn) other;
            return returnValue.equals(dirReturn.returnValue);
        }
        return false;
    }

    @Override
    public String toString() {
        return "return " + returnValue;
    }

    @Override
    public void acceptVisitor(DirVisitor visitor) {
        visitor.visitReturn(this);
    }
}

