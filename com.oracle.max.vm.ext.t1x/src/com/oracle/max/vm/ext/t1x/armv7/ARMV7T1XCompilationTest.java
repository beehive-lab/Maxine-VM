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
 */
package com.oracle.max.vm.ext.t1x.armv7;

import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.asm.target.armv7.ARMV7Assembler.*;
import com.oracle.max.vm.ext.t1x.*;
import com.sun.cri.ci.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.type.*;

public class ARMV7T1XCompilationTest extends ARMV7T1XCompilation {

    public ARMV7T1XCompilationTest(T1X compiler) {
        super(compiler);
    }

    public void emitPrologueTests() {
        emitPrologue();
        emitUnprotectMethod();
        do_methodTraceEntry();
    }

    public void emitEpilogueTests() {
        emitEpilogue();
    }

    public void do_iaddTests() {
        peekInt(ARMV7.r0, 0);
        decStack(1);
        peekInt(ARMV7.r1, 0);
        decStack(1);
        asm.addRegisters(ConditionFlag.Always, false, ARMV7.r0, ARMV7.r0, ARMV7.r1, 0, 0);
        incStack(1);
        pokeInt(ARMV7.r0, 0);
    }

    public void do_imulTests() {
        peekInt(ARMV7.r0, 0);
        decStack(1);
        peekInt(ARMV7.r1, 0);
        decStack(1);
        asm.mul(ConditionFlag.Always, false, ARMV7.r0, ARMV7.r0, ARMV7.r1);
        incStack(1);
        pokeInt(ARMV7.r0, 0);
    }

    public void do_initFrameTests(ClassMethodActor method, CodeAttribute codeAttribute) {
        initFrame(method, codeAttribute);
    }

    public void do_storeTests(int index, Kind kind) {
        do_store(index, kind);
    }

    public void do_loadTests(int index, Kind kind) {
        do_load(index, kind);
    }

    public void do_fconstTests(float value) {
        do_fconst(value);
    }

    public void do_dconstTests(double value) {
        do_dconst(value);
    }

    public void do_iconstTests(int value) {
        do_iconst(value);
    }

    public void do_lconstTests(long value) {
        do_lconst(value);
    }

    public void assignmentTests(CiRegister reg, long value) {
        assignLong(reg, value);
    }

    public void assignDoubleTest(CiRegister reg, double value) {
        assignDouble(reg, value);
    }
}
