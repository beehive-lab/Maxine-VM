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
