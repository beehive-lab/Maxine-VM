/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
package com.sun.max.vm.runtime.aarch64;

import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;


public class Aarch64TrapFrameAccess extends TrapFrameAccess {

    public static final int TRAP_NUMBER_OFFSET = 0;

    @Override
    public Pointer getPCPointer(Pointer trapFrame) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pointer getSP(Pointer trapFrame) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pointer getFP(Pointer trapFrame) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pointer getSafepointLatch(Pointer trapFrame) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSafepointLatch(Pointer trapFrame, Pointer value) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getTrapNumber(Pointer trapFrame) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Pointer getCalleeSaveArea(Pointer trapFrame) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setTrapNumber(Pointer trapFrame, int trapNumber) {
        // TODO Auto-generated method stub

    }

    @Override
    public void logTrapFrame(Pointer trapFrame) {
        // TODO Auto-generated method stub

    }

}
