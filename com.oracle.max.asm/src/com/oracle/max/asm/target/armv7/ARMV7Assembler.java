/*
 * Copyright (c) 2017, 2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.asm.target.armv7;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.armv7.ARMV7Label.*;
import com.oracle.max.asm.target.armv7.ARMV7Label.BranchInfo.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

public class ARMV7Assembler extends AbstractAssembler {

    public final CiRegister frameRegister;
    public final CiRegister scratchRegister;
    public final RiRegisterConfig registerConfig;

    public ARMV7Assembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target);
        this.registerConfig = registerConfig;
        this.scratchRegister = registerConfig == null ? ARMV7.r12 : registerConfig.getScratchRegister();
        this.frameRegister = registerConfig == null ? null : registerConfig.getFrameRegister();
    }

    public enum ConditionFlag {
        Equal(0x0, "="), NotEqual(0x1, "!="), CarrySetUnsignedHigherEqual(0x2, "|carry|"), CarryClearUnsignedLower(0x3, "|ncarry|"), Minus(0x4, "|neg|"), Positive(0x5, "|pos|"), SignedOverflow(0x6,
                        ".of."), NoSignedOverflow(0x7, "|nof|"), UnsignedHigher(0x8, "|>|"), UnsignedLowerOrEqual(0x9, "|<=|"), SignedGreaterOrEqual(0xA,
                                        ".>=."), SignedLesser(0xB, ".<."), SignedGreater(0xC, ".>."), SignedLowerOrEqual(0xD, ".<=."), Always(0xE, "al"), NeverUse(0xF, "NEVER");

        public static final ConditionFlag[] values = values();

        private final int value;
        private final String operator;

        ConditionFlag(int value, String operator) {
            this.value = value;
            this.operator = operator;
        }

        public static ConditionFlag which(int test) {
            ConditionFlag tmp = ConditionFlag.NeverUse;
            switch (test) {
                case 0x0:
                    tmp = ConditionFlag.Equal;
                    break;
                case 0x1:
                    tmp = ConditionFlag.NotEqual;
                    break;
                case 0x2:
                    tmp = ConditionFlag.CarrySetUnsignedHigherEqual;
                    break;
                case 0x3:
                    tmp = ConditionFlag.CarryClearUnsignedLower;
                    break;
                case 0x4:
                    tmp = ConditionFlag.Minus;
                    break;
                case 0x5:
                    tmp = ConditionFlag.Positive;
                    break;
                case 0x6:
                    tmp = ConditionFlag.SignedOverflow;
                    break;
                case 0x7:
                    tmp = ConditionFlag.NoSignedOverflow;
                    break;
                case 0x8:
                    tmp = ConditionFlag.UnsignedHigher;
                    break;
                case 0x9:
                    tmp = ConditionFlag.UnsignedLowerOrEqual;
                    break;
                case 0xA:
                    tmp = ConditionFlag.SignedGreaterOrEqual;
                    break;
                case 0xB:
                    tmp = ConditionFlag.SignedLesser;
                    break;
                case 0xC:
                    tmp = ConditionFlag.SignedGreater;
                    break;
                case 0xD:
                    tmp = ConditionFlag.SignedLowerOrEqual;
                    break;
                case 0xE:
                    tmp = ConditionFlag.Always;
                    break;
                case 0xF:
                    tmp = ConditionFlag.NeverUse;
                    break;
            }
            return tmp;
        }

        public ConditionFlag inverse() {
            ConditionFlag tmp = ConditionFlag.Equal;
            switch (this.value) {
                case 0x0:
                    tmp = ConditionFlag.NotEqual;
                    break;
                case 0x1:
                    tmp = ConditionFlag.Equal;
                    break;
                case 0x2:
                    tmp = ConditionFlag.CarryClearUnsignedLower;
                    break;
                case 0x3:
                    tmp = ConditionFlag.CarrySetUnsignedHigherEqual;
                    break;
                case 0x4:
                    tmp = ConditionFlag.Positive;
                    break;
                case 0x5:
                    tmp = ConditionFlag.Minus;
                    break;
                case 0x6:
                    tmp = ConditionFlag.NoSignedOverflow;
                    break;
                case 0x7:
                    tmp = ConditionFlag.SignedOverflow;
                    break;
                case 0x8:
                    tmp = ConditionFlag.UnsignedLowerOrEqual;
                    break;
                case 0x9:
                    tmp = ConditionFlag.UnsignedHigher;
                    break;
                case 0xA:
                    tmp = ConditionFlag.SignedLesser;
                    break;
                case 0xB:
                    tmp = ConditionFlag.SignedGreaterOrEqual;
                    break;
                case 0xC:
                    tmp = ConditionFlag.SignedLowerOrEqual;
                    break;
                case 0xD:
                    tmp = ConditionFlag.SignedGreater;
                    break;
                case 0xE:
                    tmp = ConditionFlag.NeverUse;
                    break;
                case 0xF:
                    tmp = ConditionFlag.Always;
                    break;
            }
            return tmp;
        }

        public int value() {
            return value;
        }

    }

    public void insertDivZeroCheck() {
        ARMV7Label continuation = new ARMV7Label();
        jcc(ConditionFlag.Always, continuation);
        bind(continuation);
        eor(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r12, 0, 0);
    }

    public void insertForeverLoop() {
        ARMV7Label forever = new ARMV7Label();
        bind(forever);
        jcc(ConditionFlag.Always, forever);
    }

    public final void b(ConditionFlag cond, int offset) {
        checkConstraint(-0x800000 <= offset && offset <= 0x7fffff, "branch must be within  a 24bit offset");
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0xA000000;
        instruction |= 0xffffff & offset;
        emitInt(instruction);
    }

    public void branch(ARMV7Label l) {
        if (l.isBound()) {
            int disp = (l.position() - codeBuffer.position() - 8) / 4;
            b(ConditionFlag.Always, disp);
        } else {
            BranchInfo info = new BranchInfo(BranchInfo.BranchType.BRANCH, ConditionFlag.Always);
            l.addPatchAt(codeBuffer.position(), info);
            emitInt(ConditionFlag.Always.value << 28 | 0xd0d0);
        }
    }

    protected void patchJumpTarget(int branch, int target, BranchInfo info) {
        BranchType type = (info.getBranchType() == BranchType.UNKNOWN) ? BranchInfo.fromValue(codeBuffer.getInt(branch) & 0xffff) : info.getBranchType();
        ConditionFlag flag = (info.getBranchType() == BranchType.UNKNOWN) ? ConditionFlag.which((codeBuffer.getInt(branch) >> 28) & 0xf) : info.getConditionFlag();
        checkConstraint(-0x800000 <= (target - branch) && (target - branch) <= 0x7fffff, "branch must be within  a 24bit offset");
        int disp = target - branch;
        int instruction = 0;

        if (type == BranchType.JCC) {
            assert codeBuffer.getInt(branch) == ((flag.value() << 28) | 0xbeef);
            assert codeBuffer.getInt(branch + 4) == getNop();
            assert codeBuffer.getInt(branch + 8) == getNop();
            disp -= 16;
            instruction = movwHelper(flag, ARMV7.r12, disp & 0xffff);
            codeBuffer.emitInt(instruction, branch);
            instruction = movtHelper(flag, ARMV7.r12, (disp >> 16) & 0xffff);
            codeBuffer.emitInt(instruction, branch + 4);
            instruction = addRegistersHelper(flag, false, ARMV7.PC, ARMV7.PC, ARMV7.r12, 0, 0);
            codeBuffer.emitInt(instruction, branch + 8);
        } else if (type == BranchType.JMP) {
            assert codeBuffer.getInt(branch) == ((flag.value() << 28) | 0xdead);
            assert codeBuffer.getInt(branch + 4) == getNop();
            assert codeBuffer.getInt(branch + 8) == getNop();
            assert flag == ConditionFlag.Always;
            checkConstraint(-0x800000 <= (disp + 8) && disp <= 0x7fffff, "patchJumpTarget TWO must be within  a 24bit offset");
            disp -= 16;
            instruction = movwHelper(flag, ARMV7.r12, disp & 0xffff);
            codeBuffer.emitInt(instruction, branch);
            instruction = movtHelper(flag, ARMV7.r12, (disp >> 16) & 0xffff);
            codeBuffer.emitInt(instruction, branch + 4);
            instruction = addRegistersHelper(flag, false, ARMV7.PC, ARMV7.PC, ARMV7.r12, 0, 0);
            codeBuffer.emitInt(instruction, branch + 8);
        } else if (type == BranchType.TABLESWITCH) {
            assert (codeBuffer.getInt(branch) & 0xfff) == 0x0d0;
            assert flag == ConditionFlag.Always;
            checkConstraint(-0x800000 <= disp && disp <= 0x7fffff, "patchJumpTarget three must be within  a 24bit offset");
            disp = (disp - 8) / 4;
            codeBuffer.emitInt(0x0a000000 | (disp & 0xffffff) | ((ConditionFlag.Always.value() & 0xf) << 28), branch);
        } else { // branch
            assert codeBuffer.getInt(branch) == ((flag.value() << 28) | 0xd0d0);
            assert type == BranchType.BRANCH;
            assert flag == ConditionFlag.Always;
            checkConstraint(-0x800000 <= disp && disp <= 0x7fffff, "patchJumpTarget four must be within  a 24bit offset");
            disp = (disp - 8) / 4;
            codeBuffer.emitInt(0x0a000000 | (disp & 0xffffff) | ((ConditionFlag.Always.value() & 0xf) << 28), branch);
        }
    }

    public void sxtb(final ConditionFlag cond, final CiRegister dest, final CiRegister source) {
        int instruction = 0x06af0070;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (dest.getEncoding() & 0xf) << 12;
        instruction |= source.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void sxth(final ConditionFlag cond, final CiRegister dest, final CiRegister source) {
        int instruction = 0x06bf0070;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (dest.getEncoding() & 0xf) << 12;
        instruction |= source.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void addlsl(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final CiRegister rm, final int shitImm) {
        int instruction = 0x800000;
        checkConstraint(0 <= shitImm && shitImm <= 31, "0 <= shitImm && shitImm <= 31");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= rm.getEncoding() & 0xf;
        instruction |= (shitImm & 0x1f) << 7;
        emitInt(instruction);
    }

    public static int blxHelper(final ConditionFlag cond, final CiRegister rm) {
        int instruction = 0x12FFF30;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= rm.getEncoding() & 0xf;
        return instruction;
    }

    public void add12BitImmediate(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final int imm12) {
        int instruction = 0x2800000;
        assert 0 <= imm12 && imm12 < 4096 : "0 <= imm12 && imm12 < 4096";
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= imm12;
        emitInt(instruction);
    }

    public void addRegisters(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final CiRegister rm, final int imm2Type, final int imm5) {
        emitInt(addRegistersHelper(cond, s, rd, rn, rm, imm2Type, imm5));
    }

    public static int addRegistersHelper(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final CiRegister rm, final int imm2Type, final int imm5) {
        int instruction = 0x00800000;
        assert 0 <= imm5 && imm5 <= 3 : "0 <= imm5 && imm5 <= 31";
        assert 0 <= imm2Type && imm2Type <= 3 : "0 <= imm2Type && imm2Type <= 3";
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (imm5 << 7) | (imm2Type << 5);
        instruction |= rm.getEncoding() & 0xf;
        return instruction;
    }

    public void addCRegisters(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final CiRegister rm, final int imm2Type, final int imm5) {
        int instruction = 0xA00000;
        checkConstraint(0 <= imm5 && imm5 <= 31, "0 <= imm5 && imm5 <= 31");
        checkConstraint(0 <= imm2Type && imm2Type <= 3, "0 <= imm2Type && imm2Type <= 3");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (imm5 << 7) | (imm2Type << 5);
        instruction |= rm.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void lsl(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rm, final int imm5) {
        int instruction = 0x1A00000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= rm.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void lsl(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rm, final CiRegister rn) {
        int instruction = 0x1A00010;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (rm.getEncoding() & 0xf) << 8;
        instruction |= rn.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void lsr(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rm, final int imm5) {
        int instruction = 0x1A00020;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= rm.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void lsr(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rm, final CiRegister rn) {
        int instruction = 0x1A00030;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (rm.getEncoding() & 0xf) << 8;
        instruction |= rn.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void lusr(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rm, final int imm5) {
        int instruction = 0x1A00040;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= rm.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void lusr(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rm, final CiRegister rn) {
        int instruction = 0x1A00050;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (rm.getEncoding() & 0xf) << 8;
        instruction |= rn.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void and(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final CiRegister rm, final int imm5, final int imm2) {
        int instruction = 0x0000000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= (imm2 & 0x3) << 5;
        instruction |= rm.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void and(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final int imm12) {
        int instruction = 0x2000000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= imm12 & 0xfff;
        emitInt(instruction);
    }

    public void eor(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final CiRegister rm, final int imm5, final int imm2) {
        int instruction = 0x00200000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= (imm2 & 0x3) << 5;
        instruction |= rm.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void orr(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final CiRegister rm, final int imm5, final int imm2) {
        int instruction = 0x1800000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= (imm2 & 0x3) << 5;
        instruction |= rm.getEncoding() & 0xfff;
        emitInt(instruction);
    }

    public void orsr(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final CiRegister rm, final CiRegister rs, final int type) {
        int instruction = 0x1800010;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (rs.getEncoding() & 0xf) << 8;
        instruction |= (type & 0x3) << 5;
        instruction |= rm.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void or(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final int imm12) {
        int instruction = 0x3800000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= imm12 & 0xf;
        emitInt(instruction);
    }

    public void asr(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rm, final int imm5) {
        int instruction = 0x1A00040;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= rm.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void asrr(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rm, final CiRegister rn) {
        int instruction = 0x1A00050;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (rm.getEncoding() & 0xf) << 8;
        instruction |= rn.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void movror(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rm, final int shitImm) {
        int instruction = 0x01A00060;
        checkConstraint(0 <= shitImm && shitImm <= 31, "0 <= shitImm && shitImm <= 31");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= rm.getEncoding() & 0xf;
        instruction |= (shitImm & 0x1f) << 7;
        emitInt(instruction);
    }

    public void mvn(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rm, final int shitImm) {
        int instruction = 0x1E00000;
        checkConstraint(0 <= shitImm && shitImm <= 31, "0 <= shitImm && shitImm <= 31");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= rm.getEncoding() & 0xf;
        instruction |= (shitImm & 0x1f) << 7;
        emitInt(instruction);
    }

    public void mov(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rm) {
        int instruction = 0x01a00000;
        assert rd.getEncoding() < 16 && rm.getEncoding() < 16;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= rm.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public static int movHelper(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rm) {
        int instruction = 0x01a00000;
        assert rd.getEncoding() < 16 && rm.getEncoding() < 16;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= rm.getEncoding() & 0xf;
        return instruction;
    }

    public void mov(final ConditionFlag cond, final CiRegister rd, final int immed12) {
        int instruction = 0x3A00000;
        assert rd.getEncoding() < 16;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= immed12 & 0xfff;
        emitInt(instruction);
    }

    public void movt(final ConditionFlag cond, final CiRegister rd, final int imm16) {
        int instruction = 0x03400000;
        checkConstraint(0 <= imm16 && imm16 <= 65535, "0<= imm16 && imm16 <= 65535 ");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (imm16 >> 12) << 16;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= imm16 & 0xfff;
        emitInt(instruction);
    }

    public static int movtHelper(final ConditionFlag cond, final CiRegister rd, final int imm16) {
        int instruction = 0x03400000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (imm16 >> 12) << 16;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= imm16 & 0xfff;
        return instruction;
    }

    public static int movwHelper(final ConditionFlag cond, final CiRegister rd, final int imm16) {
        int instruction = 0x03000000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (imm16 >> 12) << 16;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= imm16 & 0xfff;
        return instruction;
    }

    public void movImm12(final ConditionFlag cond, final CiRegister rd, final int imm12) {
        int instruction = 0x03a00000;
        checkConstraint(0 <= imm12 && imm12 <= 4095, "0<= imm12 && imm12 <= 4095 ");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= imm12;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        emitInt(instruction);
    }

    public void mvn(final ConditionFlag cond, final CiRegister rd, final int imm12) {
        int instruction = 0x03800000;
        checkConstraint(0 <= imm12 && imm12 <= 4095, "0<= imm12 && imm12 <= 4095 ");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= imm12;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        emitInt(instruction);
    }

    public void movw(final ConditionFlag cond, final CiRegister rd, final int imm16) {
        int instruction = 0x03000000;
        checkConstraint(0 <= imm16 && imm16 <= 65535, "0<= imm16 && imm16 <= 65535 ");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (imm16 >> 12) << 16;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= imm16 & 0xfff;
        emitInt(instruction);
    }

    public void neg(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final int imm12) {
        int instruction = 0x2600000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= imm12 & 0xfff;
        emitInt(instruction);
    }

    public void nop(final ConditionFlag cond) {
        int instruction = 0x320F000;
        instruction |= (cond.value() & 0xf) << 28;
        emitInt(instruction);
    }

    public int getNop() {
        int instruction = 0x320F000;
        instruction |= (ConditionFlag.Always.value() & 0xf) << 28;
        return instruction;
    }

    public void sub12BitImmediate(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final int imm12) {
        int instruction = 0x2400000;
        checkConstraint(0 <= imm12 && imm12 < 4096, "0 <= imm12 && imm12 < 4096");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= imm12;
        instruction |= rd.getEncoding() << 12;
        instruction |= rn.getEncoding() << 16;
        emitInt(instruction);
    }

    public void sub(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final int imm8, final int rotateAmount) {
        int instruction = 0x02400000;
        checkConstraint(0 <= imm8 && imm8 <= 255, "0 <= imm8 && imm8 <= 255");
        checkConstraint(0 <= rotateAmount && rotateAmount <= 15, "0 <= rotateAmount && rotateAmount  <= 15");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= imm8 & 0xff;
        instruction |= (rotateAmount / 2 & 0xf) << 8;
        emitInt(instruction);
    }

    public void rsb(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final int imm8, final int rotateAmount) {
        int instruction = 0x2600000;
        checkConstraint(0 <= imm8 && imm8 <= 255, "0 <= imm8 && imm8 <= 255");
        checkConstraint(0 <= rotateAmount && rotateAmount <= 15, "0 <= rotateAmount && rotateAmount  <= 15");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= imm8 & 0xff;
        instruction |= (rotateAmount / 2 & 0xf) << 8;
        emitInt(instruction);
    }

    public void rsbRegister(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final CiRegister rm, final int immed5, final int rotateAmount) {
        int instruction = 0x600000;
        checkConstraint(0 <= immed5 && immed5 < 32, "0 <= immed5 && immed5 < 32");
        checkConstraint(0 <= rotateAmount && rotateAmount < 4, "0 <= rotateAmount && rotateAmount  < 4");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= rm.getEncoding() & 0xf;
        instruction |= (immed5 & 0x1f) << 7;
        instruction |= (rotateAmount & 0x3) << 5;
        emitInt(instruction);
    }

    public void rsc(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final CiRegister rm, final int imm5, final int type) {
        int instruction = 0xE00000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= (type & 0x3) << 5;
        instruction |= rm.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void rsc(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final int imm12) {
        int instruction = 0x2E00000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= imm12 & 0xfff;
        emitInt(instruction);
    }

    public void sbc(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final CiRegister rm, final int imm5, final int type) {
        int instruction = 0xC00000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= (type & 0x3) << 5;
        instruction |= rm.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void sub(final ConditionFlag cond, final boolean s, final CiRegister rd, final CiRegister rn, final CiRegister rm, final int imm5, final int imm2Type) {
        int instruction = 0x00400000;
        checkConstraint(0 <= imm5 && imm5 <= 31, "0 <= imm5 && imm5 <= 31");
        checkConstraint(0 <= imm2Type && imm2Type <= 3, "0<= imm2Type && imm2Type <= 3");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= rm.getEncoding() & 0xf;
        instruction |= (imm2Type & 0x3) << 5;
        instruction |= (imm5 & 0x31) << 5;
        emitInt(instruction);
    }

    public void strd(final ConditionFlag cond, int p, int u, int w, final CiRegister rt, final CiRegister rn, final CiRegister rm) {
        int instruction = 0x000000f0;
        instruction |= (p & 0x1) << 24;
        instruction |= (u & 0x1) << 23;
        instruction |= (w & 0x1) << 21;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rt.getEncoding() & 0xf) << 12;
        instruction |= rm.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void str(final ConditionFlag cond, int p, int u, int w, final CiRegister rt, final CiRegister rn, final CiRegister rm, int imm5, int imm2Type) {
        int instruction = 0x06000000;
        instruction |= (p & 0x1) << 24;
        instruction |= (u & 0x1) << 23;
        instruction |= (w & 0x1) << 21;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rt.getEncoding() & 0xf) << 12;
        instruction |= rm.getEncoding() & 0xf;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= (imm2Type & 0x3) << 5;
        emitInt(instruction);
    }

    public static int strHelper(final ConditionFlag cond, int p, int u, int w, final CiRegister rt, final CiRegister rn, final CiRegister rm, int imm5, int imm2Type) {
        int instruction = 0x06000000;
        instruction |= (p & 0x1) << 24;
        instruction |= (u & 0x1) << 23;
        instruction |= (w & 0x1) << 21;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rt.getEncoding() & 0xf) << 12;
        instruction |= rm.getEncoding() & 0xf;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= (imm2Type & 0x3) << 5;
        return instruction;
    }

    public void strImmediate(final ConditionFlag cond, int p, int u, int w, final CiRegister rvalue, final CiRegister rmemory, int imm12) {
        int instruction = 0x04000000;
        checkConstraint(-4095 <= imm12 && imm12 <= 4095, "strImmediate offset greater than +/- 4095 ");
        if (imm12 < 0) {
            assert u == 0;
            imm12 *= -1;
        } else {
            assert u == 1;
        }
        assert rvalue.getEncoding() != rmemory.getEncoding() || !(p == 0 && u == 0 && w == 0);
        instruction |= (p & 0x1) << 24;
        instruction |= (u & 0x1) << 23;
        instruction |= (w & 0x1) << 21;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rmemory.getEncoding() & 0xf) << 16;
        instruction |= (rvalue.getEncoding() & 0xf) << 12;
        instruction |= imm12 & 0xfff;
        emitInt(instruction);
    }

    public void clz(final ConditionFlag cond, final CiRegister rdest, final CiRegister rval) {
        int instruction = 0x016f0f10;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rdest.getEncoding() & 0xf) << 12;
        instruction |= rval.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void rbit(final ConditionFlag cond, final CiRegister rdest, final CiRegister rval) {
        int instruction = 0x06ff0f30;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rdest.getEncoding() & 0xf) << 12;
        instruction |= rval.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void ldrex(final ConditionFlag cond, final CiRegister rdest, final CiRegister raddr) {
        int instruction = 0x01900f9f;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rdest.getEncoding() & 0xf) << 12;
        instruction |= (raddr.getEncoding() & 0xf) << 16;
        emitInt(instruction);
    }

    public void ldrexd(final ConditionFlag cond, final CiRegister rdest, final CiRegister raddr) {
        int instruction = 0x1B00F9F;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (raddr.getEncoding() & 0xf) << 16;
        instruction |= (rdest.getEncoding() & 0xf) << 12;
        emitInt(instruction);
    }

    public void strex(final ConditionFlag cond, final CiRegister rdest, final CiRegister rnewval, final CiRegister raddr) {
        int instruction = 0x01800f90;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rdest.getEncoding() & 0xf) << 12;
        instruction |= (raddr.getEncoding() & 0xf) << 16;
        instruction |= rnewval.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void strexd(final ConditionFlag cond, final CiRegister rd, final CiRegister rt, final CiRegister rn) {
        int instruction = 0x1A00F90;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rd.getEncoding() & 0xf) << 12;
        instruction |= rt.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void ldruhw(final ConditionFlag cond, int p, int u, int w, final CiRegister rt, final CiRegister rn, int imm8) {
        checkConstraint(-255 <= imm8 && imm8 <= 255, "-255 <= offset8 && offset8 <= 255 ldruhw");
        int instruction = 0x005000b0;
        p &= 1;
        u &= 1;
        if (imm8 < 0) {
            u = 0;
            imm8 *= -1;
        }
        w &= 1;
        instruction |= (p << 24) | (u << 23) | (w << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rt.getEncoding() & 0xf) << 12;
        instruction |= (imm8 & 0xf) | ((0xf0 & imm8) << 4);
        emitInt(instruction);
    }

    public void ldrshw(final ConditionFlag cond, int p, int u, int w, final CiRegister rt, final CiRegister rn, int imm8) {
        checkConstraint(-255 <= imm8 && imm8 <= 255, "-255 <= offset8 && offset8 <= 255 ldrshw");
        int instruction = 0x005000f0;
        p &= 1;
        u &= 1;
        if (imm8 < 0) {
            u = 0;
            imm8 *= -1;
        }
        w &= 1;
        instruction |= (p << 24) | (u << 23) | (w << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rt.getEncoding() & 0xf) << 12;
        instruction |= 0xf0 | (imm8 & 0xf) | ((0xf0 & imm8) << 4);
        emitInt(instruction);
    }

    public void ldrsb(final ConditionFlag cond, int p, int u, int w, final CiRegister rt, final CiRegister rn, int imm8) {
        checkConstraint(-255 <= imm8 && imm8 <= 255, "-255 <= offset8 && offset8 <= 255 ldrsb");
        int instruction = 0x005000d0;
        p &= 1;
        u &= 1;
        w &= 1;
        if (imm8 < 0) {
            u = 0;
            imm8 *= -1;
        }
        instruction |= (p << 24) | (u << 23) | (w << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rt.getEncoding() & 0xf) << 12;
        instruction |= 0xf & imm8;
        instruction |= (imm8 >> 4) << 8;
        emitInt(instruction);
    }

    public void ldrb(final ConditionFlag cond, int p, int u, int w, final CiRegister rt, final CiRegister rn, int imm12) {
        int instruction = 0x04500000;
        p &= 1;
        u &= 1;
        w &= 1;
        instruction |= (p << 24) | (u << 23) | (w << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rt.getEncoding() & 0xf) << 12;
        instruction |= 0xfff & imm12;
        emitInt(instruction);
    }

    public void strbImmediate(final ConditionFlag cond, int p, int u, int w, final CiRegister rt, final CiRegister rn, int imm12) {
        int instruction = 0x04400000;
        p &= 1;
        u &= 1;
        w &= 1;
        instruction |= (p << 24) | (u << 23) | (w << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rt.getEncoding() & 0xf) << 12;
        instruction |= 0xfff & imm12;
        emitInt(instruction);
    }

    public void strHImmediate(final ConditionFlag cond, int p, int u, int w, final CiRegister rt, final CiRegister rn, int imm8) {
        int instruction = 0x004000b0;
        p &= 1;
        u &= 1;
        w &= 1;
        if (imm8 < 0) {
            u = 0;
            imm8 *= -1;
        }
        instruction |= (p << 24) | (u << 23) | (w << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rt.getEncoding() & 0xf) << 12;
        instruction |= 0xf & imm8;
        instruction |= ((imm8 & 0xff) >> 4) << 8;
        emitInt(instruction);
    }

    public void ldrImmediate(final ConditionFlag cond, int p, int u, int w, final CiRegister rt, final CiRegister rn, int imm12) {
        int instruction = 0x04100000;
        checkConstraint(-4095 <= imm12 && imm12 <= 4095, "ldrImmediate offset greater than +/- 4095 ");
        if (imm12 < 0) {
            assert u == 0;
            imm12 *= -1;
        } else {
            assert u == 1;
        }
        p &= 1;
        u &= 1;
        w &= 1;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (p << 24) | (u << 23) | (w << 21);
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rt.getEncoding() & 0xf) << 12;
        instruction |= imm12;
        emitInt(instruction);
    }

    public void ldr(final ConditionFlag cond, int p, int u, int w, final CiRegister rt, final CiRegister rn, final CiRegister rm, int imm2Type, int imm5) {
        int instruction = 0x06100000;
        p &= 1;
        u &= 1;
        w &= 1;
        instruction |= (p << 24) | (u << 23) | (w << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rt.getEncoding() & 0xf) << 12;
        instruction |= rm.getEncoding() & 0xf;
        instruction |= (imm2Type & 0x3) << 5;
        instruction |= (imm5 & 0x1f) << 7;
        emitInt(instruction);
    }

    public void movss(final ConditionFlag cond, int p, int u, int w, final CiRegister rn, final CiRegister rt, final CiRegister rm, int imm2Type, int imm5, CiKind destKind, CiKind srcKind) {
        if (destKind.isGeneral()) {
            ldr(cond, p, u, w, rn, rt, rm, imm2Type, imm5);
        } else {
            assert destKind.isFloatOrDouble();
            vldr(cond, rn, rt, 0, destKind, srcKind);
        }
    }

    public void movsd(final ConditionFlag cond, int p, int u, int w, final CiRegister rn, final CiRegister rt, final CiRegister rm, CiKind destKind, CiKind srcKind) {
        if (destKind.isGeneral()) {
            ldrd(cond, p, u, w, rn, rt, rm);
        } else {
            assert destKind.isFloatOrDouble();
            vldr(cond, rn, rt, 0, destKind, srcKind);
        }
    }

    public void ldrd(final ConditionFlag cond, int p, int u, int w, final CiRegister rn, final CiRegister rt, final CiRegister rm) {
        int instruction = 0x000000d0;
        p &= 1;
        u &= 1;
        w &= 1;
        instruction |= (p << 24) | (u << 23) | (w << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (rt.getEncoding() & 0xf) << 12;
        instruction |= rm.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public void swi(final ConditionFlag cond, final int imm24) {
        int instruction = 0x0F000000;
        checkConstraint(0 <= imm24 && imm24 <= 16777215, "0 <= immed_24 && immed_24 <= 16777215");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= imm24 & 0xffffff;
        emitInt(instruction);
    }

    public int getRegisterList(CiRegister... registers) {
        int regList = 0;
        for (CiRegister reg : registers) {
            regList |= 1 << reg.getEncoding();
        }
        return regList;
    }

    public void push(final ConditionFlag flag, CiRegister... registers) {
        push(flag, getRegisterList(registers), false);
    }

    public void push(final ConditionFlag flag, final int registerList) {
        push(flag, registerList, false);
    }

    public void push(final ConditionFlag flag, final int registerList, boolean instrument) {
        int instruction;
        assert registerList > 0;
        assert registerList < 0x10000;
        instruction = (flag.value() & 0xf) << 28;
        instruction |= 0x9 << 24;
        instruction |= 0x2 << 20;
        instruction |= 0xd << 16;
        instruction |= 0xffff & registerList;
        emitInt(instruction);
    }

    // Wee need that method due to limited number of registers when we process long values in 32 bit space.
    public void saveRegister(int reg, int reg2) {
        push(ConditionFlag.Always, (1 << reg) | (1 << reg2), true);
    }

    public void saveInFP(int reg) {
        vmov(ConditionFlag.Always, ARMV7.s28, ARMV7.cpuRegisters[reg], null, CiKind.Float, CiKind.Int);
    }

    public void restoreFromFP(int reg) {
        vmov(ConditionFlag.Always, ARMV7.cpuRegisters[reg], ARMV7.s28, null, CiKind.Int, CiKind.Float);
    }

    public void saveInFP(final ConditionFlag flag, int reg, int reg2) {
        vmov(flag, ARMV7.s28, ARMV7.cpuRegisters[reg], null, CiKind.Float, CiKind.Int);
        vmov(flag, ARMV7.s29, ARMV7.cpuRegisters[reg2], null, CiKind.Float, CiKind.Int);
    }

    public void restoreFromFP(final ConditionFlag flag, int reg, int reg2) {
        vmov(flag, ARMV7.cpuRegisters[reg], ARMV7.s28, null, CiKind.Int, CiKind.Float);
        vmov(flag, ARMV7.cpuRegisters[reg2], ARMV7.s29, null, CiKind.Int, CiKind.Float);
    }

    public void restoreRegister(int reg, int reg2) {
        pop(ConditionFlag.Always, (1 << reg) | (1 << reg2), true);
    }

    public void pop(final ConditionFlag flag, CiRegister... registers) {
        pop(flag, getRegisterList(registers), false);
    }

    public void pop(final ConditionFlag flag, final int registerList) {
        pop(flag, registerList, false);
    }

    public void pop(final ConditionFlag flag, final int registerList, boolean instrument) {
        assert registerList > 0;
        assert registerList < 0x10000;
        int instruction = (flag.value() & 0xf) << 28;
        instruction |= 0x8 << 24;
        instruction |= 0xb << 20;
        instruction |= 0xd << 16;
        instruction |= 0xffff & registerList;
        emitInt(instruction);
    }

    public void ldmea(final ConditionFlag flag, final CiRegister theStack, final int registerList) {
        int instruction = 0x09100000;
        instruction |= (flag.value() & 0xf) << 28;
        instruction |= (theStack.getEncoding() & 0xf) << 16;
        instruction |= 0xffff & registerList;
        emitInt(instruction);
    }

    public void ldrd(final ConditionFlag flag, final CiRegister destReg, final CiRegister baseReg, int offset8) {
        int instruction = 0x004000d0;
        int p = 1;
        int w = 0;
        int u;
        checkConstraint(-255 <= offset8 && offset8 <= 255, "-255 <= offset8 && offset8 <= 255 ldrd");
        if (offset8 < 0) {
            u = 0;
            offset8 *= -1;
        } else {
            u = 1;
        }
        checkConstraint(destReg.getEncoding() % 2 == 0, "ldrd register must be even");
        instruction |= (flag.value() & 0xf) << 28;
        instruction |= p << 24;
        instruction |= u << 23;
        instruction |= w << 21;
        instruction |= (destReg.getEncoding() & 0xf) << 12;
        instruction |= (baseReg.getEncoding() & 0xf) << 16;
        instruction |= (offset8 & 0xf0) << 4;
        instruction |= offset8 & 0xf;
        emitInt(instruction);
    }

    public void strd(final ConditionFlag flag, final CiRegister valueReg, final CiRegister baseReg, int offset8) {
        int instruction;
        instruction = 0x004000f0;
        int p = 1;
        int u;
        int w = 0;
        checkConstraint(valueReg.getEncoding() % 2 == 0, "strd register must be even");
        checkConstraint(-255 <= offset8 && offset8 <= 255, "-255 <= offset8 && offset8 <= 255");
        if (offset8 < 0) {
            u = 0;
            offset8 *= -1;
        } else {
            u = 1;
        }
        instruction |= (flag.value() & 0xf) << 28;
        instruction |= (p << 24) | (u << 23) | (w << 21);
        instruction |= (valueReg.getEncoding() & 0xf) << 12;
        instruction |= (baseReg.getEncoding() & 0xf) << 16;
        instruction |= (offset8 & 0xf0) << 4;
        instruction |= offset8 & 0xf;
        emitInt(instruction);
    }

    public void str(final ConditionFlag flag, final CiRegister valueReg, final CiRegister baseRegister, int offset12) {
        int instruction = 0x05800000;
        int p = 1;
        int u = 1;
        int w = 0;
        if (offset12 < 0) {
            u = 0;
            offset12 *= -1;
        }
        instruction |= (p << 24) | (u << 23) | (w << 21);
        instruction |= (flag.value() & 0xf) << 28;
        instruction |= (valueReg.getEncoding() & 0xf) << 12;
        instruction |= (baseRegister.getEncoding() & 0xf) << 16;
        instruction |= offset12 & 0xfff;
        emitInt(instruction);
    }

    public void ldr(final ConditionFlag flag, final CiRegister destReg, final CiRegister baseRegister, int offset12) {
        checkConstraint(-4095 <= offset12 && offset12 <= 4095, "ldrImmediate offset greater than +/- 4095 ");
        int instruction = 0x05900000;
        int p = 1;
        int u = 1;
        int w = 0;
        if (offset12 < 0) {
            u = 0;
            offset12 *= -1;
        }
        p &= 1;
        u &= 1;
        w &= 1;

        instruction |= (p << 24) | (u << 23) | (w << 21);
        instruction |= (flag.value() & 0xf) << 28;
        instruction |= (destReg.getEncoding() & 0xf) << 12;
        instruction |= (baseRegister.getEncoding() & 0xf) << 16;
        instruction |= offset12 & 0xfff;
        emitInt(instruction);
    }

    public void cmp(final ConditionFlag flag, final CiRegister rn, final CiRegister rm, int imm5, int imm2Type) {
        int instruction = 0x01500000;
        assert !(rn.getEncoding() == 12 && rn.getEncoding() == rm.getEncoding());
        checkConstraint(0 <= imm5 && imm5 <= 31, "0 <= imm5 && imm5 <= 31");
        checkConstraint(0 <= imm2Type && imm2Type <= 3, "0 <= imm2Type && imm2Type <= 3");
        instruction |= (flag.value() & 0xf) << 28;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= imm5 << 7;
        instruction |= imm2Type << 5;
        instruction |= rm.getEncoding() & 0xf;
        emitInt(instruction);
    }

    protected void checkConstraint(boolean passed, String expression) {
        if (!passed) {
            throw new IllegalArgumentException(expression);
        }
    }

    private static int encode(CiRegister r) {
        assert r.getEncoding() < 16 && r.getEncoding() >= 0 : "encoding out of range: " + r.getEncoding();
        return r.getEncoding();
    }



    private CiAddress calculateAddress(CiAddress addr, CiKind kind) {
        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;

        assert addr != CiAddress.Placeholder;
        assert !(base.isValid() && disp == 0 && base.compareTo(ARMV7.LATCH_REGISTER) == 0);
        assert base.isValid() || base.compareTo(CiRegister.Frame) == 0;

        if (base == CiRegister.Frame) {
            base = frameRegister;
        }
        //assert (base.isValid()) && (base == CiRegister.Frame);

        switch (addr.format()) {
            case BASE:
                break;
            case BASE_DISP:
                if (ARMImmediates.isValidDisp(disp, kind)) {
                    break;
                } else if (ARMImmediates.isValidImmediate(Math.abs(disp))) {
                    if (disp >= 0) {
                        add12BitImmediate(ConditionFlag.Always, false, scratchRegister, base, ARMImmediates.calculateShifter(disp));
                    } else {
                        sub12BitImmediate(ConditionFlag.Always, false, scratchRegister, base, ARMImmediates.calculateShifter(disp));
                    }
                    base = scratchRegister;
                    disp = 0;
                } else {
                    movImmediate(ConditionFlag.Always, scratchRegister, disp);
                    addRegisters(ConditionFlag.Always, false, scratchRegister, base, scratchRegister, 0, 0);
                    base = scratchRegister;
                    disp = 0;
                }
                break;
            case BASE_INDEX:
                addlsl(ConditionFlag.Always, false, scratchRegister, base, index, scale.log2);
                base = scratchRegister;
                disp = 0;
                break;
            case BASE_INDEX_DISP:
                if (ARMImmediates.isValidDisp(disp, kind)) {
                    addlsl(ConditionFlag.Always, false, scratchRegister, base, index, scale.log2);
                    base = scratchRegister;
                } else if (ARMImmediates.isValidImmediate(Math.abs(disp))) {
                    if (disp > 0) {
                        add12BitImmediate(ConditionFlag.Always, false, scratchRegister, base, ARMImmediates.calculateShifter(disp));
                    } else {
                        sub12BitImmediate(ConditionFlag.Always, false, scratchRegister, base, ARMImmediates.calculateShifter(disp));
                    }
                    addlsl(ConditionFlag.Always, false, scratchRegister, base, index, scale.log2);
                    base = scratchRegister;
                    disp = 0;
                } else {
                    movImmediate(ConditionFlag.Always, scratchRegister, disp);
                    addRegisters(ConditionFlag.Always, false, scratchRegister, base, scratchRegister, 0, 0);
                    base = scratchRegister;
                    disp = 0;
                }
                break;
            default:
                assert false : "Unknown state!";
        }
        return new CiAddress(addr.kind, new CiRegisterValue(CiKind.Int, base), disp);
    }

    public void load(CiRegister dest, CiAddress addr, CiKind kind) {
        CiAddress address = calculateAddress(addr, kind);
        switch (kind) {
            case Short:
                ldrshw(ConditionFlag.Always, 1, 1, 0, dest, address.base(), address.displacement);
                break;
            case Char:
                ldruhw(ConditionFlag.Always, 1, 1, 0, dest, address.base(), address.displacement);
                break;
            case Boolean:
            case Byte:
                ldrsb(ConditionFlag.Always, 1, 1, 0, dest, address.base(), address.displacement);
                break;
            case Long:
                ldrd(ConditionFlag.Always, dest, address.base(), address.displacement);
                break;
            case Double:
                vldr(ConditionFlag.Always, dest, address.base(), address.displacement, CiKind.Double, CiKind.Int);
                break;
            case Float:
                vldr(ConditionFlag.Always, dest, address.base(), address.displacement, CiKind.Float, CiKind.Int);
                break;
            case Int:
            case Object:
                ldr(ConditionFlag.Always, dest, address.base(), address.displacement);
                break;
            default:
                assert false : "Unknown kind!";
        }
    }

    public void store(CiRegister src, CiAddress addr, CiKind kind) {
        CiAddress address = calculateAddress(addr, kind);
        switch (kind) {
            case Short:
                strHImmediate(ConditionFlag.Always, 1, 0, 0, src, address.base(), address.displacement);
                break;
            case Long:
                strd(ConditionFlag.Always, src, address.base(), address.displacement);
                break;
            case Double:
                vstr(ConditionFlag.Always, src, address.base(), address.displacement, CiKind.Double, CiKind.Int);
                break;
            case Float:
                vstr(ConditionFlag.Always, src, address.base(), address.displacement, CiKind.Float, CiKind.Int);
                break;
            case Int:
            case Object:
                str(ConditionFlag.Always, src, address.base(), address.displacement);
                break;
            case Byte:
                break;
            default:
                assert false : "Unknown kind!";
        }
    }

    public void setUpScratch(CiAddress addr) {
        assert !(addr.base().isValid() && addr.displacement == 0 && addr.base().compareTo(ARMV7.LATCH_REGISTER) == 0);
        setUpRegister(scratchRegister, addr);
    }

    public void setUpRegister(CiRegister dest, CiAddress addr) {
        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;
        if (addr == CiAddress.Placeholder) {
            nop(numInstructions(addr));
            return;
        }

        assert base.isValid() || base.compareTo(CiRegister.Frame) == 0;

        if (base.isValid() || base.compareTo(CiRegister.Frame) == 0) {
            if (base == CiRegister.Frame) {
                base = frameRegister;
            }
            if (disp != 0) {
                movImm32(ConditionFlag.Always, dest, disp);
                addRegisters(ConditionFlag.Always, false, dest, dest, base, 0, 0);
                if (index.isValid()) {
                    addlsl(ConditionFlag.Always, false, dest, dest, index, scale.log2);
                }
            } else {
                if (index.isValid()) {
                    addlsl(ConditionFlag.Always, false, dest, base, index, scale.log2);
                } else {
                    mov(ConditionFlag.Always, false, dest, base);
                }
            }
        }
    }

    private int numInstructions(CiAddress addr) {
        CiRegister index = addr.index();
        int disp = addr.displacement;
        if (disp != 0) {
            if (index.isValid()) {
                return 4;
            } else {
                return 3;
            }
        } else {
            return 2;
        }
    }

    public final void decq(CiRegister dst) {
        assert dst.isValid();
        sub(ConditionFlag.Always, false, dst, dst, 1, 0);
    }

    public final void subq(CiRegister dst, int imm32) {
        assert dst.isValid();
        if (ARMImmediates.isValidImmediate(imm32)) {
            sub12BitImmediate(ConditionFlag.Always, false, dst, dst, ARMImmediates.calculateShifter(imm32));
            return;
        }
        movImmediate(ConditionFlag.Always, scratchRegister, imm32);
        sub(ConditionFlag.Always, false, dst, dst, scratchRegister, 0, 0);
    }

    public final void movImm32(ConditionFlag flag, CiRegister dst, int imm32) {
        if (dst.number < 16) {
            movw(flag, dst, imm32 & 0xffff);
            imm32 = imm32 >>> 16; // use >>> to zero fill left bits (no sign extend)
            movt(flag, dst, imm32);
        } else {
            movImm32(flag, ARMV7.r12, imm32);
            vmov(flag, dst, ARMV7.r12, null, CiKind.Float, CiKind.Int);
        }
    }

    public final void vsqrt(ConditionFlag cond, CiRegister dst, CiRegister src, CiKind kind) {
        int instruction = 0x0eb10ac0;
        instruction |= cond.value() << 28;
        int dp = (kind == CiKind.Double) ? 1 : 0;
        instruction |= dp << 8; // sets the sz bit
        if (dp == 1) {
            instruction |= src.getDoubleEncoding();
            instruction |= dst.getDoubleEncoding() << 12;
        } else {
            instruction |= src.getEncoding() >> 1;
            instruction |= (src.getEncoding() & 1) << 5;
            instruction |= (dst.getEncoding() >> 1) << 12;
            instruction |= (dst.getEncoding() & 1) << 22;
        }
        assert instruction != 0xeeb12bd2;
        emitInt(instruction);
    }

    public final void movImm16(ConditionFlag cond, CiRegister dst, int imm16) {
        movw(cond, dst, imm16);
    }

    public void movImmediate(ConditionFlag flag, CiRegister dest, int imm32) {
        if (dest.number > 15) {
            movImmediate(flag, ARMV7.r12, imm32);
            vmov(flag, dest, ARMV7.r12, null, CiKind.Float, CiKind.Int);
            return;
        }

        if (ARMImmediates.isValidImmediate(imm32)) {
            mov(flag, dest, ARMImmediates.calculateShifter(imm32));
        } else if (imm32 == 0) {
            eor(flag, false, dest, dest, dest, 0, 0);
        } else if (imm32 >>> 16 == 0) { // use >>> to zero fill left bits (no sign extend)
            movImm16(flag, dest, imm32);
        } else {
            movImm32(flag, dest, imm32);
        }
    }

    public final void movImm64(ConditionFlag flag, CiRegister dstLow, CiRegister dstUpper, long imm64) {
        int low32 = (int) (imm64 & 0xffffffffL);
        movImmediate(flag, dstLow, low32);
        int high32 = (int) ((imm64 >> 32) & 0xffffffffL);
        movImmediate(flag, dstUpper, high32);
    }

    public final void alignForPatchableDirectCall() {
        int dispStart = codeBuffer.position() + 1;
        int mask = target.wordSize - 1;
        if ((dispStart & ~mask) != ((dispStart + 3) & ~mask)) {
            nop(target.wordSize - (dispStart & mask));
        }
    }

    public final void call() {
        nop(4);
    }

    public final void blx(CiRegister target) {
        int instruction = blxHelper(ConditionFlag.Always, target);
        emitInt(instruction);
    }

    public final void call(CiRegister target) {
        if (ARMV7.r8 != target) {
            mov(ConditionFlag.Always, false, ARMV7.r8, target);
        }
        int instruction = blxHelper(ConditionFlag.Always, ARMV7.r8);
        emitInt(instruction);
    }

    public final void leaq(CiRegister dest, CiAddress addr) {
        if (addr == CiAddress.Placeholder) {
            nop(4);
        } else {
            setUpScratch(addr);
            mov(ConditionFlag.Always, false, dest, ARMV7.r12);
        }
    }

    public final void leave() {
        ret();
    }

    public final void movslq(CiAddress dst, int imm32) {
        movImmediate(ConditionFlag.Always, ARMV7.r8, imm32);
        store(ARMV7.r8, dst, CiKind.Int);
    }

    public final void cmpl(CiRegister src, int imm32) {
        assert src.isValid();
        if (imm32 <= 255 && imm32 >= 0) {
            cmpImmediate(ConditionFlag.Always, src, imm32);
            return;
        }
        movImmediate(ConditionFlag.Always, scratchRegister, imm32);
        cmp(ConditionFlag.Always, src, scratchRegister, 0, 0);
    }

    public final void cmpImmediate(ConditionFlag condition, CiRegister src, int imm12) {
        int instruction = 0x3500000;
        imm12 = imm12 & 0xfff;
        instruction |= condition.value() << 28;
        instruction |= imm12;
        instruction |= src.getEncoding() << 16;
        emitInt(instruction);
    }

    public final void cmpl(CiRegister src1, CiAddress src2) {
        assert src1.isValid();
        setUpScratch(src2);
        ldr(ConditionFlag.Always, ARMV7.r12, ARMV7.r12, 0);
        cmp(ConditionFlag.Always, src1, scratchRegister, 0, 0);
    }

    public final void cmpl(CiRegister src1, CiRegister src2) {
        cmp(ConditionFlag.Always, src1, src2, 0, 0);
    }

    public final void lcmpl(ConditionFlag condition, CiRegister src1, CiRegister src2) {
        if (condition == ConditionFlag.UnsignedHigher) {
            cmp(ConditionFlag.Always, ARMV7.cpuRegisters[src1.number + 1], ARMV7.cpuRegisters[src2.number + 1], 0, 0);
            cmp(ConditionFlag.Equal, src1, src2, 0, 0);
            ARMV7Label isFalse = new ARMV7Label();
            ARMV7Label isEnd = new ARMV7Label();
            jcc(ConditionFlag.UnsignedLowerOrEqual, isFalse);
            movImmediate(ConditionFlag.Always, ARMV7.r12, 1);
            cmpImmediate(ConditionFlag.Always, ARMV7.r12, 0);
            jcc(ConditionFlag.Always, isEnd);
            bind(isFalse);
            movImmediate(ConditionFlag.Always, ARMV7.r12, 0);
            cmpImmediate(ConditionFlag.Always, ARMV7.r12, 1);
            bind(isEnd);
            return;
        }

        if (condition == ConditionFlag.SignedGreater) {
            CiRegister tmp = src1;
            src1 = src2;
            src2 = tmp;
        }
        cmp(ConditionFlag.Always, src1, src2, 0, 0);
        if (condition == ConditionFlag.Equal || condition == ConditionFlag.NotEqual) {
            cmp(ConditionFlag.Equal, ARMV7.cpuRegisters[src1.number + 1], ARMV7.cpuRegisters[src2.number + 1], 0, 0);
        } else {
            sbc(ConditionFlag.Always, true, scratchRegister, ARMV7.cpuRegisters[src1.number + 1], ARMV7.cpuRegisters[src2.number + 1], 0, 0);
            if (condition == ConditionFlag.SignedGreater) {
                movw(ConditionFlag.SignedLesser, ARMV7.r12, 1);
                movw(ConditionFlag.SignedGreaterOrEqual, ARMV7.r12, 0);
                cmpImmediate(ConditionFlag.Always, ARMV7.r12, 0);
            }
        }
    }

    public final void xchgq(CiRegister src1, CiRegister src2) {
        CiRegister tmp = ARMV7.r8;
        assert src1 != tmp && src2 != tmp;
        mov(ConditionFlag.Always, false, tmp, src1);
        mov(ConditionFlag.Always, false, src1, src2);
        mov(ConditionFlag.Always, false, src2, tmp);
    }

    public final void incq(CiRegister dst) {
        assert dst.isValid();
        add12BitImmediate(ConditionFlag.Always, false, dst, dst, 1);
    }

    public final void addq(CiRegister dst, int imm32) {
        assert dst.isValid();
        if (ARMImmediates.isValidImmediate(imm32)) {
            add12BitImmediate(ConditionFlag.Always, false, dst, dst, ARMImmediates.calculateShifter(imm32));
            return;
        }
        movImmediate(ConditionFlag.Always, scratchRegister, imm32);
        addRegisters(ConditionFlag.Always, false, dst, dst, scratchRegister, 0, 0);
    }

    public final void addLong(CiRegister dst, CiRegister src1, CiRegister src2) {
        addRegisters(ConditionFlag.Always, true, dst, src1, src2, 0, 0);
        addCRegisters(ConditionFlag.Always, false, ARMV7.cpuRegisters[dst.number + 1], ARMV7.cpuRegisters[src1.number + 1], ARMV7.cpuRegisters[src2.number + 1], 0, 0);
    }

    public final void subLong(CiRegister dst, CiRegister src1, CiRegister src2) {
        sub(ConditionFlag.Always, true, dst, src1, src2, 0, 0);
        sbc(ConditionFlag.Always, false, ARMV7.cpuRegisters[dst.number + 1], ARMV7.cpuRegisters[src1.number + 1], ARMV7.cpuRegisters[src2.number + 1], 0, 0);
    }

    public final void mulLong(CiRegister dst, CiRegister src1, CiRegister src2) {
        assert src1 == dst;
        saveInFP(ConditionFlag.Always, src2.number, src2.number + 1);
        mov(ConditionFlag.Always, false, ARMV7.r12, src2); // save src2
        mul(ConditionFlag.Always, false, src2, src2, ARMV7.cpuRegisters[src1.number + 1]);
        mul(ConditionFlag.Always, false, ARMV7.cpuRegisters[src2.number + 1], src1, ARMV7.cpuRegisters[src2.number + 1]);
        addRegisters(ConditionFlag.Always, false, ARMV7.cpuRegisters[src2.number + 1], src2, ARMV7.cpuRegisters[src2.number + 1], 0, 0);
        umull(ConditionFlag.Always, false, ARMV7.cpuRegisters[src2.number], dst, ARMV7.cpuRegisters[scratchRegister.number], src1);
        addRegisters(ConditionFlag.Always, false, scratchRegister, ARMV7.cpuRegisters[src2.number + 1], src2, 0, 0);
        mov(ConditionFlag.Always, false, ARMV7.cpuRegisters[dst.number + 1], scratchRegister);
        restoreFromFP(ConditionFlag.Always, src2.number, src2.number + 1);
    }

    public void xorq(CiRegister dest, CiAddress src) {
        assert dest.isValid();
        setUpScratch(src);
        eor(ConditionFlag.Always, false, dest, dest, scratchRegister, 0, 0);
    }

    public void xorq(CiRegister dest, CiRegister src) {
        assert dest.isValid();
        assert src.isValid();
        eor(ConditionFlag.Always, false, dest, dest, src, 0, 0);
    }

    public final void vcmp(boolean nanExceptions, ConditionFlag cond, CiRegister src1, CiRegister src2, CiKind src1Kind, CiKind src2Kind) {
        assert src1.isFpu() && src2.isFpu();
        int instruction = 0x0eb40a40;
        int dpOperation = 0;
        int quietNAN = nanExceptions ? 0 : 1;
        assert src1Kind == src2Kind;
        assert (src1Kind.isFloat() && src2Kind.isFloat()) || (src1Kind.isDouble() && src2Kind.isDouble());
        if (src1Kind.isDouble()) {
            assert src2Kind.isDouble();
            dpOperation = 1;
            instruction |= (src1.getDoubleEncoding() & 0xf) << 12;
            instruction |= (src1.getDoubleEncoding() >> 4) << 22;
            instruction |= src2.getDoubleEncoding() & 0xf;
            instruction |= (src2.getDoubleEncoding() >> 4) << 5;
        } else {
            assert src2Kind.isFloat();
            instruction |= (src1.getEncoding() >> 1) << 12;
            instruction |= (src1.getEncoding() & 0x1) << 22;
            instruction |= src2.getEncoding() >> 1;
            instruction |= (src2.getEncoding() & 0x1) << 5;
        }
        instruction |= cond.value() << 28;
        instruction |= dpOperation << 8;
        instruction |= quietNAN << 7;
        emitInt(instruction);
    }

    public final void ucomisd(CiRegister dst, CiRegister src, CiKind destKind, CiKind srcKind) {
        assert destKind.isFloatOrDouble();
        assert srcKind.isFloatOrDouble();
        vcmp(true, ConditionFlag.Always, dst, src, destKind, srcKind);
        vmrs(ConditionFlag.Always, ARMV7.r15);
    }

    public void align(int modulus) {
        if (codeBuffer.position() % modulus != 0) {
            assert modulus % 4 == 0;
            nop((modulus - (codeBuffer.position() % modulus)) / 4);
        }
    }

    public final void nop() {
        nop(1);
    }

    public final void pause() {
        push(ConditionFlag.Always, 1 | 2 | 4 | 8 | 128, true);
        movImmediate(ConditionFlag.Always, ARMV7.r7, 158); // sched_yield
        emitInt(0xef000000); // replaced with svc 0
        pop(ConditionFlag.Always, 1 | 2 | 4 | 8 | 128, true);
    }

    public final void crashme() {
        eor(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r12, 0, 0);
        insertForeverLoop();
        ldr(ConditionFlag.Always, ARMV7.r12, ARMV7.r12, 0);
    }

    /**
     * Emits an instruction that causes an interrupt. Since ARMv7 has no special interrupts for debuggers this method
     * emits an undefined instruction which will eventually cause an interrupt.
     */
    public final void int3() {
        emitInt(0xFEDEFFE7);
    }

    public final void hlt() {
        nop(4);
        int3();
    }

    public final void nop(int times) {
        assert times > 0 : "Cannot emit a -v number of noops";
        for (int i = 0; i < times; i++) {
            nop(ConditionFlag.Always);
        }
    }

    public final void enterExceptionHandler() {
        int instruction = 0xf8ed0500;
        instruction |= 1 << 4;
        emitInt(instruction);
    }

    public final void returnFromExceptionHandler() {
        int instruction = 0x8f08000;
        instruction |= 13 << 16;
        instruction |= 0xe << 28;
        emitInt(instruction);
    }

    public final void ret() {
        pop(ConditionFlag.Always, 1 << 15);
    }

    public final void ret(int imm16) {
        if (imm16 == 0) {
            ret();
        } else {
            addq(ARMV7.r13, imm16);
            ret();
        }
    }

    public void enter(short imm16, byte imm8) {
        assert false : "Enter not implemented";
        push(ConditionFlag.Always, 1 << 14, true);
        movImmediate(ConditionFlag.Always, ARMV7.r12, imm16);
        sub(ConditionFlag.Always, false, ARMV7.r13, ARMV7.r13, ARMV7.r12, 0, 0);
    }

    public final void lock() {
        nop();
    }

    public void nullCheck(CiRegister r) {
        if (r == ARMV7.r8) {
            emitInt((0xe << 28) | (0x3 << 24) | (0x5 << 20) | (r.getEncoding() << 16) | 0);
            ldr(ConditionFlag.Equal, ARMV7.r12, r, 0);
        } else {
            emitInt((0xe << 28) | (0x3 << 24) | (0x5 << 20) | (r.getEncoding() << 16) | 0);
            ldr(ConditionFlag.Equal, ARMV7.r8, r, 0);
        }
    }

    public void clearex() {
        int instruction = 0xf57ff01f;
        emitInt(instruction);
    }

    public void barrierDMB() {
        int instruction = 0xF57FF050;
        emitInt(instruction);
    }

    public void membar(int barriers) {
        int instruction = 0xF57FF040 | 0xf;
        emitInt(instruction);
        instruction = 0xf57ff06f;
        emitInt(instruction);
    }

    public void enter(short imm16) {
    }

    public final void jcc(ConditionFlag cc, int target, boolean forceDisp32) {
        int disp = target - codeBuffer.position();
        if (Math.abs(disp) <= 16777214 && !forceDisp32) {
            disp = (disp - 8) / 4;
            emitInt((cc.value & 0xf) << 28 | (0xa << 24) | (disp & 0xffffff));
        } else {
            disp -= 16;
            movImm32(cc, scratchRegister, disp);
            addRegisters(cc, false, ARMV7.PC, ARMV7.PC, scratchRegister, 0, 0);
        }
    }

    public final void jcc(ConditionFlag cc, ARMV7Label l) {
        assert (0 <= cc.value) && (cc.value < 16) : "illegal cc";
        if (l.isBound()) {
            jcc(cc, l.position(), false);
        } else {
            BranchInfo info = new BranchInfo(BranchType.JCC, cc);
            l.addPatchAt(codeBuffer.position(), info);
            emitInt(cc.value() << 28 | 0xbeef);
            nop(2);
        }
    }

    public final void jmp(CiRegister absoluteAddress) {
        bx(ConditionFlag.Always, absoluteAddress);
    }

    public final void bx(ConditionFlag cond, CiRegister target) {
        int instruction = 0x012fff10;
        instruction |= cond.value() << 28;
        instruction |= target.getEncoding();
        emitInt(instruction);
    }

    public final void jmp(ARMV7Label l) {
        if (l.isBound()) {
            jmp(l.position(), false);
        } else {
            BranchInfo info = new BranchInfo(BranchType.JMP, ConditionFlag.Always);
            l.addPatchAt(codeBuffer.position(), info);
            emitInt(ConditionFlag.Always.value << 28 | 0xdead);
            nop(2);
        }
    }

    public final void jmp(int target, boolean forceDisp32) {
        int disp = target - codeBuffer.position();
        if (disp <= 16777215 && !forceDisp32) {
            disp = (disp - 8) / 4;
            emitInt((0xe << 28) | (0xa << 24) | (disp & 0xffffff));
        } else {
            disp -= 16;
            movImm32(ConditionFlag.Always, scratchRegister, disp);
            addRegisters(ConditionFlag.Always, false, ARMV7.PC, ARMV7.PC, scratchRegister, 0, 0);
        }
    }

    // mrs r0, CPSR
    public final void mrsReadAPSR(ConditionFlag cond, CiRegister reg) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x10f0000;
        instruction |= reg.getEncoding() << 12;
        emitInt(instruction);
    }

    // msr CPSR_fs, r0, lsl r0
    public void msrWriteAPSR(ConditionFlag cond, CiRegister reg) {
        int bits = 3;
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x120f000;
        instruction |= reg.getEncoding();
        instruction |= bits << 18;
        emitInt(instruction);
    }

    //  vmrs dest, fpscr
    public final void vmrs(ConditionFlag cond, CiRegister dest) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0ef10a10;
        instruction |= dest.getEncoding() << 12;
        emitInt(instruction);
    }

    public final void vmsr(ConditionFlag cond, CiRegister dest) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0ee10a10;
        instruction |= dest.getEncoding() << 12;
        emitInt(instruction);
    }

    public final void vmul(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm, CiKind destKind) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0e200a00;
        int sz = 0;
        checkConstraint(dest.number >= 16 && rn.number >= 16 && rm.number >= 16, "vmul ALL  FP/DP regs");
        if (destKind.isDouble()) {
            sz = 1;
        }
        if (sz == 1) { // bit rest of bits
            instruction |= (dest.getDoubleEncoding() & 0xf) << 12;
            instruction |= (rn.getDoubleEncoding() & 0xf) << 16;
            instruction |= rm.getDoubleEncoding() & 0xf;
            instruction |= (dest.getDoubleEncoding() >> 4) << 22;
            instruction |= (rn.getDoubleEncoding() >> 4) << 7;
            instruction |= (rm.getDoubleEncoding() >> 4) << 5;
        } else {
            instruction |= (dest.getEncoding() >> 1) << 12;
            instruction |= (rn.getEncoding() >> 1) << 16;
            instruction |= rm.getEncoding() >> 1;
            instruction |= (dest.getEncoding() & 0x1) << 22;
            instruction |= (rn.getEncoding() & 0x1) << 7;
            instruction |= (rm.getEncoding() & 0x1) << 5;
        }
        instruction |= sz << 8;
        emitInt(instruction);
    }

    public final void vcvt(ConditionFlag cond, CiRegister dest, boolean toInt, boolean signed, CiRegister src, CiKind destKind, CiKind srcKind) {
        int instruction = (cond.value() & 0xf) << 28;
        int sz = 0;
        int op = 0;
        int opc2 = 0;
        boolean double2Float = false;
        boolean int2Float = false;
        boolean int2Double = false;
        boolean floatConversion = false;

        if (!toInt) {
            checkConstraint(!dest.isGeneral() && !src.isGeneral(), "vcvt must be FP/DP regs");
            if (destKind.isFloat() && srcKind.isDouble()) {
                double2Float = true;
            } else if (destKind.isFloat() && srcKind.isGeneral()) {
                int2Float = true;
            } else if (destKind.isDouble() && srcKind.isGeneral()) {
                int2Double = true;
            }
            if (destKind.isFloat()) {
                floatConversion = true;
            }
        }

        if (destKind.isDouble() || srcKind.isDouble()) {
            sz = 1;
        }

        if (signed) {
            op = 1;
            if (toInt) {
                opc2 = 5;
            } else {
                opc2 = 0;
            }
        } else {
            if (toInt) {
                opc2 = 4;
                op = 1;
            } else {
                opc2 = 0;
            }
        }
        if (!floatConversion) {
            if (toInt) { // FD2I
                instruction |= 0xeb80a40;
                instruction |= (dest.getEncoding() >> 1) << 12; // LSB in bit 22
                instruction |= (dest.getEncoding() & 0x1) << 22;
                if (sz == 1) {
                    instruction |= src.getDoubleEncoding() & 0xf;
                    instruction |= (src.getDoubleEncoding() >> 4) << 5;
                } else {
                    instruction |= src.getEncoding() >> 1;
                    instruction |= (src.getEncoding() & 0x1) << 5;
                }
                instruction |= opc2 << 16;
                instruction |= op << 7;
                instruction |= sz << 8;
            } else if (int2Double) { // I2D
                instruction |= 0xEB80A40;
                instruction |= sz << 8;
                instruction |= (dest.getDoubleEncoding() >> 4) << 22;
                instruction |= (dest.getDoubleEncoding() & 0xf) << 12;
                instruction |= (src.getEncoding() & 1) << 5;
                instruction |= src.getEncoding() >> 1;
                instruction |= opc2 << 16;
                instruction |= op << 7;
            } else { // F2D
                instruction |= 0xEB70AC0;
                instruction |= (src.getEncoding() & 0x1) << 5;
                instruction |= src.getEncoding() >> 1;
                instruction |= (dest.getDoubleEncoding() >> 4) << 22;
                instruction |= (dest.getDoubleEncoding() & 0xf) << 12;
            }
        } else {
            instruction |= sz << 8;
            if (double2Float) { // D2FI
                instruction |= 0xEB70AC0;
                instruction |= (dest.getEncoding() >> 1) << 12;
                instruction |= (dest.getEncoding() & 1) << 22;
                instruction |= src.getDoubleEncoding() & 0xf;
                instruction |= (src.getDoubleEncoding() >> 4) << 5;
            } else if (int2Float) { // I2F
                instruction |= 0xEB80A40;
                instruction |= (dest.getEncoding() >> 1) << 12;
                instruction |= (dest.getEncoding() & 1) << 22;
                instruction |= (src.getEncoding() & 1) << 5;
                instruction |= src.getEncoding() >> 1;
                instruction |= opc2 << 16;
                instruction |= op << 7;
            }
        }
        emitInt(instruction);
    }

    public final void vstr(ConditionFlag cond, CiRegister dest, CiRegister src, int imm8, CiKind destKind, CiKind srcKind) {
        if (src == CiRegister.Frame) {
            src = frameRegister;
        }

        int instruction = (cond.value() & 0xf) << 28;
        checkConstraint(dest.isFpu(), "vstr dest must be a FP/DP reg");
        checkConstraint(-1020 <= imm8 && imm8 <= 1020, "vmov offset greater than +/- 255 ");
        checkConstraint(imm8 % 4 == 0, " imm8 is not a multiple of 4");
        checkConstraint(src.isCpu(), "vstr base src address register must be core");
        if (imm8 >= 0) {
            instruction |= 1 << 23;
        } else {
            imm8 *= -1;
        }
        imm8 = imm8 >> 2;
        assert src.getEncoding() >= 0 && src.getEncoding() <= 15;
        instruction |= imm8 & 0xff;
        instruction |= (src.getEncoding() & 0xf) << 16;
        if (destKind.isDouble()) {
            instruction |= 0x0d000b00;
            instruction |= (dest.getDoubleEncoding() & 0xf) << 12;
            instruction |= (dest.getDoubleEncoding() >> 4) << 22;
        } else {
            instruction |= 0xd000a00;
            instruction |= (dest.getEncoding() >> 1) << 12;
            instruction |= (dest.getEncoding() & 0x1) << 22;
        }
        emitInt(instruction);
    }

    public final void vneg(ConditionFlag cond, CiRegister dest, CiRegister src, CiKind destKind) {
        int instruction = (cond.value() & 0xf) << 28;
        assert destKind.isFloat() || destKind.isDouble() : " Dest register must be FP/DP reg";
        int sz = 0;
        int src4bits = 0;
        int dest4bits = 0;
        int src1bit = 0;
        int dest1bit = 0;
        if (destKind.isDouble()) {
            sz = 1;
            src4bits = src.getDoubleEncoding() & 0xf;
            src1bit = (src.getDoubleEncoding() >> 4) & 0x1;
            dest4bits = dest.getDoubleEncoding() & 0xf;
            dest1bit = (dest.getDoubleEncoding() >> 4) & 0x1;
        } else {
            src4bits = (src.getEncoding() >> 1) & 0xf;
            src1bit = (src.getEncoding()) & 0x1;
            dest4bits = (dest.getEncoding() >> 1) & 0xf;
            dest1bit = (dest.getEncoding()) & 0x1;
        }
        instruction |= 0x0eb10a40;
        instruction |= src4bits;
        instruction |= src1bit << 5;
        instruction |= dest4bits << 12;
        instruction |= dest1bit << 22;
        instruction |= sz << 8;
        emitInt(instruction);
    }

    public final void vldr(ConditionFlag cond, CiRegister dest, CiRegister src, int imm8, CiKind destKind, CiKind srcKind) {
        int add = 0;
        int instruction = (cond.value() & 0xf) << 28;
        if (src == CiRegister.Frame) {
            src = frameRegister;
        }
        checkConstraint(dest.isFpu(), "vldr dest must be a FP/DP reg");
        checkConstraint(-1020 <= imm8 && imm8 <= 1020, "vmov offset greater than +/- 1020 (scaled by 4) ");
        checkConstraint(imm8 % 4 == 0, "imm8 not a multiple of 4 ");

        if (imm8 >= 0) {
            add = 1;
            instruction |= 1 << 23;
        } else {
            add = 0;
            imm8 *= -1;
        }
        imm8 >>= 2;
        assert src.getEncoding() <= 0xf;
        assert src.getEncoding() >= 0;
        instruction |= imm8 & 0xff;
        instruction |= add << 23;
        instruction |= src.getEncoding() << 16;
        if (destKind.isDouble()) {
            instruction |= 0x0d100b00;
            instruction |= (dest.getDoubleEncoding() >> 4) << 22;
            instruction |= (dest.getDoubleEncoding() & 0xf) << 12;
        } else {
            instruction |= 0xd100a00;
            instruction |= (dest.getEncoding() >> 1) << 12;
            instruction |= (dest.getEncoding() & 0x1) << 22;
        }
        emitInt(instruction);
    }

    public final void vadd(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm, CiKind kind) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0e300a00;
        checkConstraint(dest.number >= 16 && rn.number >= 16 && rm.number >= 16, "vadd no core registers allowed");
        checkConstraint((dest.number <= 47 && rn.number <= 47 && rm.number <= 47) || (dest.number <= 47 && rn.number <= 47 && rm.number <= 47), "vadd register overflow");
        int sz = 0;
        if (kind.isDouble()) {
            sz = 1;
        }
        instruction |= sz << 8;
        if (sz == 1) {
            instruction |= (rn.getDoubleEncoding() & 0xf) << 16;
            instruction |= (dest.getDoubleEncoding() & 0xf) << 12;
            instruction |= rm.getDoubleEncoding() & 0xf;
            instruction |= (rn.getDoubleEncoding() >> 4) << 7;
            instruction |= (dest.getDoubleEncoding() >> 4) << 22;
            instruction |= (rm.getDoubleEncoding() >> 4) << 5;
        } else {
            instruction |= (rn.getEncoding() & 1) << 7;
            instruction |= (rn.getEncoding() >> 1) << 16;
            instruction |= (rm.getEncoding() & 1) << 5;
            instruction |= rm.getEncoding() >> 1;
            instruction |= (dest.getEncoding() & 1) << 22;
            instruction |= (dest.getEncoding() >> 1) << 12;
        }
        emitInt(instruction);
    }

    public final void vpop(ConditionFlag cond, CiRegister first, CiRegister last, CiKind firstKind, CiKind lastKind) {
        int instruction = (cond.value() & 0xf) << 28;
        checkConstraint(!first.isCpu() && !last.isCpu(), "vpop No core regs allowed");
        checkConstraint(!(firstKind.isDouble() && !lastKind.isDouble()) && !(!firstKind.isDouble() && lastKind.isDouble()), "vpush no mix of FP/DP allowed");
        checkConstraint(last.number >= first.number, "vpop at least ONE register!!");
        int sz = 0;
        if (firstKind.isDouble()) {
            sz = 1;
        }
        if (sz == 1) {
            instruction |= 0x0cbd0b00;
            instruction |= (first.getDoubleEncoding() & 0xf) << 12;
            instruction |= (last.getDoubleEncoding() - first.getDoubleEncoding() + 1) << 1;
        } else {
            instruction |= 0x0cbd0a00;
            instruction |= (first.getEncoding() & 0x1) << 22;
            instruction |= (first.getEncoding() >> 1) << 12;
            instruction |= last.getEncoding() - first.getEncoding() + 1;
        }
        emitInt(instruction);
    }

    public final void vpush(ConditionFlag cond, CiRegister first, CiRegister last, CiKind firstKind, CiKind lastKind) {
        int instruction = (cond.value() & 0xf) << 28;
        checkConstraint(!first.isCpu() && !last.isCpu(), "vpush No core regs allowed");
        checkConstraint(!(firstKind.isDouble() && !lastKind.isDouble()) && !(!firstKind.isDouble() && lastKind.isDouble()), "vpush no mix of FP/DP allowed");
        checkConstraint(last.number >= first.number, "vpush at least one register!!");
        int sz = 0;
        if (firstKind.isDouble()) {
            sz = 1;
        }
        if (sz == 1) {
            instruction |= 0x0d2d0b00;
            instruction |= (first.getDoubleEncoding() & 0xf) << 12;
            instruction |= (last.getDoubleEncoding() - first.getDoubleEncoding() + 1) << 1;
        } else {
            instruction |= 0x0d2d0a00;
            instruction |= (first.getEncoding() & 0x1) << 22;
            instruction |= (first.getEncoding() >> 1) << 12;
            instruction |= last.getEncoding() - first.getEncoding() + 1;
        }
        emitInt(instruction);
    }

    public final void mul(ConditionFlag cond, boolean setFlags, CiRegister dest, CiRegister rn, CiRegister rm) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x00000090;
        if (setFlags) {
            instruction |= 1 << 20;
        }
        instruction |= (rm.getEncoding() & 0xf) << 8;
        instruction |= (dest.getEncoding() & 0xf) << 16;
        instruction |= rn.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public final void umull(ConditionFlag cond, boolean s, CiRegister rdHigh, CiRegister rdLow, CiRegister rm, CiRegister rn) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x800090;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rdHigh.getEncoding() & 0xf) << 16;
        instruction |= (rdLow.getEncoding() & 0xf) << 12;
        instruction |= (rm.getEncoding() & 0xf) << 8;
        instruction |= rn.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public final void sdiv(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm) {
        if (!target.hasIDivider) {
            floatDiv(true, cond, dest, rn, rm);
            return;
        }
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0710f010;
        instruction |= (rm.getEncoding() & 0xf) << 8;
        instruction |= (dest.getEncoding() & 0xf) << 16;
        instruction |= rn.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public final void floatDiv(boolean signed, ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm) {
        assert dest != ARMV7.r12;
        assert rn != ARMV7.r12;
        assert rm != ARMV7.r12;
        assert dest != ARMV7.r8;
        assert rn != ARMV7.r8;
        assert rm != ARMV7.r8;
        vpush(ConditionFlag.Always, ARMV7.s28, ARMV7.s30, CiKind.Double, CiKind.Double);
        push(ConditionFlag.Always, 1 << 8, true);
        vmrs(ConditionFlag.Always, ARMV7.r8); // save rounding mode
        movImmediate(ConditionFlag.Always, ARMV7.r12, 0xc00000);
        orr(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r8, ARMV7.r12, 0, 0);
        vmsr(ConditionFlag.Always, ARMV7.r12); // set round to zero mode
        vmov(ConditionFlag.Always, ARMV7.s28, rn, null, CiKind.Float, CiKind.Int);
        vmov(ConditionFlag.Always, ARMV7.s30, rm, null, CiKind.Float, CiKind.Int);
        vcvt(ConditionFlag.Always, ARMV7.s28, false, signed, ARMV7.s28, CiKind.Double, CiKind.Int);
        vcvt(ConditionFlag.Always, ARMV7.s30, false, signed, ARMV7.s30, CiKind.Double, CiKind.Int);
        vdiv(ConditionFlag.Always, ARMV7.s28, ARMV7.s28, ARMV7.s30, CiKind.Double);
        vcvt(ConditionFlag.Always, ARMV7.s28, true, signed, ARMV7.s28, CiKind.Int, CiKind.Double);
        vmsr(ConditionFlag.Always, ARMV7.r8); // restore default rounding mode
        pop(ConditionFlag.Always, 1 << 8, true);
        vmov(cond, dest, ARMV7.s28, null, CiKind.Int, CiKind.Float);
        vpop(ConditionFlag.Always, ARMV7.s28, ARMV7.s30, CiKind.Double, CiKind.Double);
    }

    public final void udiv(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm) {
        if (!target.hasIDivider) {
            floatDiv(false, cond, dest, rn, rm);
            return;
        }
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0730f010;
        instruction |= (rm.getEncoding() & 0xf) << 8;
        instruction |= (dest.getEncoding() & 0xf) << 16;
        instruction |= rn.getEncoding() & 0xf;
        emitInt(instruction);
    }

    public final void vdiv(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm, CiKind destKind) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0e800a00;
        checkConstraint(dest.number >= 16 && rn.number >= 16 && rm.number >= 16, "vdiv no core registers allowed");
        int sz = 0;
        if (destKind.isDouble()) {
            sz = 1;
        }
        instruction |= sz << 8;
        if (sz == 1) {
            instruction |= (rn.getDoubleEncoding() & 0xf) << 16;
            instruction |= (dest.getDoubleEncoding() & 0xf) << 12;
            instruction |= rm.getDoubleEncoding() & 0xf;
            instruction |= (dest.getDoubleEncoding() >> 4) << 22;
            instruction |= (rn.getDoubleEncoding() >> 4) << 7;
            instruction |= (rm.getDoubleEncoding() >> 4) << 5;
        } else {
            instruction |= (dest.getEncoding() >> 1) << 12;
            instruction |= (dest.getEncoding() & 1) << 22;
            instruction |= (rn.getEncoding() & 1) << 7;
            instruction |= (rn.getEncoding() >> 1) << 16;
            instruction |= (rm.getEncoding() & 1) << 5;
            instruction |= rm.getEncoding() >> 1;
        }
        emitInt(instruction);
    }

    public final void vsub(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm, CiKind destKind) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0e300a40;
        checkConstraint(dest.number >= 16 && rn.number >= 16 && rm.number >= 16, "vsub NO CORE REGISTErs ALLOWED");
        int sz = 0;
        if (destKind.isDouble()) {
            sz = 1;
        }
        instruction |= sz << 8;
        if (sz == 1) {
            instruction |= (rn.getDoubleEncoding() & 0xf) << 16;
            instruction |= (dest.getDoubleEncoding() & 0xf) << 12;
            instruction |= rm.getDoubleEncoding() & 0xf;
        } else {
            instruction |= (rm.getEncoding() & 0x1) << 5;
            instruction |= rm.getEncoding() >> 1;
            instruction |= (rn.getEncoding() >> 1) << 16;
            instruction |= (rn.getEncoding() & 0x1) << 7;
            instruction |= (dest.getEncoding() >> 1) << 12;
            instruction |= (dest.getEncoding() & 0x1) << 22;
        }
        emitInt(instruction);
    }

    public final void vmov(ConditionFlag cond, CiRegister dest, CiRegister src, CiRegister src2, CiKind destKind, CiKind srcKind) {
        int instruction = (cond.value() & 0xf) << 28;
        int vmovSameType = 0x0eb00a40;
        int vmovSingleCore = 0x0e000a10;
        int vmovDoubleCore = 0x0c400b10;
        if (srcKind.isDouble() && destKind.isLong()) {
            assert src2 == null;
        }
        if (srcKind.isDouble() && destKind.isDouble()) {
            instruction |= (1 << 8) | vmovSameType;
            instruction |= (dest.getDoubleEncoding() >> 4) << 22;
            instruction |= (dest.getDoubleEncoding() & 0xf) << 12;
            instruction |= (src.getDoubleEncoding() >> 4) << 5;
            instruction |= src.getDoubleEncoding() & 0xf;
        } else if (srcKind.isFloat() && destKind.isFloat()) {
            instruction |= vmovSameType;
            instruction |= ((dest.getEncoding() >> 1) << 12) | ((dest.getEncoding() & 0x1) << 22);
            instruction |= (src.getEncoding() >> 1) | ((src.getEncoding() & 0x1) << 5);
        } else if ((destKind.isGeneral() || srcKind.isGeneral()) && (srcKind.isFloat() || destKind.isFloat())) {
            instruction |= vmovSingleCore;
            if (dest.number <= 15) {
                instruction |= (1 << 20) | ((src.getEncoding() & 1) << 7) | (dest.getEncoding() << 12) | ((src.getEncoding() >> 1) << 16);
            } else {
                instruction |= (src.getEncoding() << 12) | ((dest.getEncoding() >> 1) << 16) | ((dest.getEncoding() & 0x1) << 7);
            }
        } else if ((srcKind.isDouble() && destKind.isGeneral()) || (destKind.isDouble() && srcKind.isGeneral())) {
            instruction |= vmovDoubleCore;
            if (dest.isGeneral()) { // to Arm
                checkConstraint((dest.getEncoding()) <= 14, "vmov doubleword to core destination register > 14");
                instruction |= 1 << 20;
                instruction |= dest.getEncoding() << 12;
                instruction |= (dest.getEncoding() + 1) << 16;
                instruction |= src.getDoubleEncoding();
                instruction |= ((src.getDoubleEncoding() >> 4) & 0x1) << 5;
            } else {
                assert src2 != null;
                checkConstraint((src.getEncoding()) <= 14, "vmov core to doubleword core register > 14");
                instruction |= src2.getEncoding() << 16;
                instruction |= src.getEncoding() << 12;
                instruction |= (dest.getDoubleEncoding() >> 4) << 5;
                instruction |= dest.getDoubleEncoding() & 0xf;
            }
        }
        emitInt(instruction);
    }

    public final void vmovImm(ConditionFlag cond, CiRegister dst, int imm, CiKind dstKind) {
        int instruction = 0xEB00A00;
        assert imm == (long) imm : "Immediate must be size int";
        instruction |= (cond.value() & 0xf) << 28;
        int size = 0;
        if (dstKind.isDouble()) {
            size = 1;
        }
        instruction |= size << 8;
        instruction |= (imm >> 4) << 16;
        instruction |= imm & 0xf;
        if (size == 1) {
            instruction |= (dst.getDoubleEncoding() >> 4) << 22;
            instruction |= (dst.getDoubleEncoding() & 0xf) << 12;
        } else {
            instruction |= (dst.getEncoding() >> 1) << 12;
            instruction |= (dst.getEncoding() << 4) << 22;
        }
        emitInt(instruction);
    }

    public final void teq(ConditionFlag cond, CiRegister rn, CiRegister rm, int imm5) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x13 << 20;
        instruction |= (rn.getEncoding() & 0xf) << 16;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= (rm.getEncoding() & 0xf) << 16;
        emitInt(instruction);
    }

    @Override
    protected void patchJumpTarget(int branch, int target) {
        BranchInfo info = new BranchInfo(BranchType.UNKNOWN, null);
        patchJumpTarget(branch, target, info);
    }
}
