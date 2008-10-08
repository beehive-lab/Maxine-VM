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
package com.sun.max.asm.gen.cisc.amd64;

import static com.sun.max.asm.amd64.AMD64GeneralRegister16.*;
import static com.sun.max.asm.amd64.AMD64GeneralRegister32.*;
import static com.sun.max.asm.amd64.AMD64GeneralRegister64.*;
import static com.sun.max.asm.amd64.AMD64GeneralRegister8.*;
import static com.sun.max.asm.gen.cisc.x86.AddressingMethodCode.*;
import static com.sun.max.asm.gen.cisc.x86.FloatingPointOperandCode.*;
import static com.sun.max.asm.gen.cisc.x86.OperandCode.*;
import static com.sun.max.asm.gen.cisc.x86.OperandTypeCode.*;

import com.sun.max.asm.amd64.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.cisc.x86.*;
import com.sun.max.collect.*;


/**
 * See A-7 in the book.
 * 
 * @see com.sun.max.asm.x86
 * 
 * @author Bernd Mathiske
 */
public enum AMD64ModRMGroup implements ModRMGroup {

    GROUP_1(
        modRM(ModRMGroup.Opcode._0, "ADD"),
        modRM(ModRMGroup.Opcode._1, "OR"),
        modRM(ModRMGroup.Opcode._2, "ADC"),
        modRM(ModRMGroup.Opcode._3, "SBB"),
        modRM(ModRMGroup.Opcode._4, "AND"),
        modRM(ModRMGroup.Opcode._5, "SUB"),
        modRM(ModRMGroup.Opcode._6, "XOR"),
        modRM(ModRMGroup.Opcode._7, "CMP")
    ),
    GROUP_2(
        modRM(ModRMGroup.Opcode._0, "ROL"),
        modRM(ModRMGroup.Opcode._1, "ROR"),
        modRM(ModRMGroup.Opcode._2, "RCL"),
        modRM(ModRMGroup.Opcode._3, "RCR"),
        modRM(ModRMGroup.Opcode._4, "SHL"),
        modRM(ModRMGroup.Opcode._5, "SHR"),
        modRM(ModRMGroup.Opcode._6, "SHL"),
        modRM(ModRMGroup.Opcode._7, "SAR")
    ),
    GROUP_3b(
        modRM(ModRMGroup.Opcode._0, "TEST", Eb.excludeExternalTestArguments(AL), Ib),
        modRM(ModRMGroup.Opcode._1, "TEST", Eb.excludeExternalTestArguments(AL), Ib),
        modRM(ModRMGroup.Opcode._2, "NOT", Eb),
        modRM(ModRMGroup.Opcode._3, "NEG", Eb),
        modRM(ModRMGroup.Opcode._4, "MUL", Eb, new ExternalOmission(AL)),
        modRM(ModRMGroup.Opcode._5, "IMUL", Eb, new ExternalOmission(AL)),
        modRM(ModRMGroup.Opcode._6, "DIV", Eb, new ExternalOmission(AL)),
        modRM(ModRMGroup.Opcode._7, "IDIV", Eb, new ExternalOmission(AL))
    ),
    GROUP_3v(
        modRM(ModRMGroup.Opcode._0, "TEST", Ev.excludeExternalTestArguments(AX, EAX, RAX), Iz.externalRange(0, Integer.MAX_VALUE)),
        modRM(ModRMGroup.Opcode._1, "TEST", Ev.excludeExternalTestArguments(AX, EAX, RAX), Iz.externalRange(0, Integer.MAX_VALUE)),
        modRM(ModRMGroup.Opcode._2, "NOT", Ev),
        modRM(ModRMGroup.Opcode._3, "NEG", Ev),
        modRM(ModRMGroup.Opcode._4, "MUL", Ev),
        modRM(ModRMGroup.Opcode._5, "IMUL", Ev),
        modRM(ModRMGroup.Opcode._6, "DIV", Ev),
        modRM(ModRMGroup.Opcode._7, "IDIV", Ev)
    ),
    GROUP_4(
        modRM(ModRMGroup.Opcode._0, "INC"),
        modRM(ModRMGroup.Opcode._1, "DEC")
    ),
    GROUP_5a(
        modRM(ModRMGroup.Opcode._0, "INC", v, Ev),
        modRM(ModRMGroup.Opcode._1, "DEC", v, Ev)
        // modRM(ModRMGroup.Opcode._3, "CALL", Mp), // legacy mode instruction
        // modRM(ModRMGroup.Opcode._5, "JMP", Mp) // legacy mode instruction
    ),
    GROUP_5b(
        modRM(ModRMGroup.Opcode._2, "CALL", Ev),
        modRM(ModRMGroup.Opcode._4, "JMP", Ev),
        modRM(ModRMGroup.Opcode._6, "PUSH", Ev.excludeExternalTestArguments(AMD64GeneralRegister16.ENUMERATOR, AMD64GeneralRegister32.ENUMERATOR, AMD64GeneralRegister64.ENUMERATOR))
    ),
    GROUP_6a(
        modRM(ModRMGroup.Opcode._0, "SLDT", Mw),
        modRM(ModRMGroup.Opcode._1, "STR", Mw),
        modRM(ModRMGroup.Opcode._2, "LLDT", Ew),
        modRM(ModRMGroup.Opcode._3, "LTR", Ew),
        modRM(ModRMGroup.Opcode._4, "VERR", Ew),
        modRM(ModRMGroup.Opcode._5, "VERW", Ew)
    ),
    GROUP_6b(
        modRM(ModRMGroup.Opcode._0, "SLDT", Rv),
        modRM(ModRMGroup.Opcode._1, "STR", Rv)
    ),
    GROUP_7a(
        modRM(ModRMGroup.Opcode._0, "SGDT", Ms),
        modRM(ModRMGroup.Opcode._1, "SIDT", Ms),
        modRM(ModRMGroup.Opcode._2, "LGDT", Ms),
        modRM(ModRMGroup.Opcode._3, "LIDT", Ms),
        modRM(ModRMGroup.Opcode._4, "SMSW", Mw),
        modRM(ModRMGroup.Opcode._6, "LMSW", Ew),
        modRM(ModRMGroup.Opcode._7, "INVLPG", M)
    ),
    GROUP_7b(
        modRM(ModRMGroup.Opcode._4, "SMSW", Rv),
        modRM(ModRMGroup.Opcode._7, "SWAPGS", X86TemplateContext.ModCase.MOD_3)    // r/m field == 0
        // modRM(ModRMGroup.Opcode._7, "RDTSCP", X86TemplateContext.ModCase.MOD_3) // r/m field == 1
    ),
    GROUP_8(
        modRM(ModRMGroup.Opcode._4, "BT"),
        modRM(ModRMGroup.Opcode._5, "BTS"),
        modRM(ModRMGroup.Opcode._6, "BTR"),
        modRM(ModRMGroup.Opcode._7, "BTC")
    ),
    GROUP_9a(
        modRM(ModRMGroup.Opcode._1, "CMPXCHG8B", Mq)
    ),
    GROUP_9b(
        modRM(ModRMGroup.Opcode._1, "CMPXCHG16B", Mdq)
    ),
    GROUP_10(
    ),
    GROUP_11(
        modRM(ModRMGroup.Opcode._0, "MOV", Eb, Ib),
        modRM(ModRMGroup.Opcode._1, "MOV", Ev, Iz)
    ),
    GROUP_12a(
        modRM(ModRMGroup.Opcode._2, "PSRLW", PRq, Ib),
        modRM(ModRMGroup.Opcode._4, "PSRAW", PRq, Ib),
        modRM(ModRMGroup.Opcode._6, "PSLLW", PRq, Ib)
    ),
    GROUP_12b(
        modRM(ModRMGroup.Opcode._2, "PSRLW", VRdq, Ib),
        modRM(ModRMGroup.Opcode._4, "PSRAW", VRdq, Ib),
        modRM(ModRMGroup.Opcode._6, "PSLLW", VRdq, Ib)
    ),
    GROUP_13a(
        modRM(ModRMGroup.Opcode._2, "PSRLD", PRq, Ib),
        modRM(ModRMGroup.Opcode._4, "PSRAD", PRq, Ib),
        modRM(ModRMGroup.Opcode._6, "PSLLD", PRq, Ib)
    ),
    GROUP_13b(
        modRM(ModRMGroup.Opcode._2, "PSRLD", VRdq, Ib),
        modRM(ModRMGroup.Opcode._4, "PSRAD", VRdq, Ib),
        modRM(ModRMGroup.Opcode._6, "PSLLD", VRdq, Ib)
    ),
    GROUP_14a(
        modRM(ModRMGroup.Opcode._2, "PSRLQ", PRq, Ib),
        modRM(ModRMGroup.Opcode._6, "PSLLQ", PRq, Ib)
    ),
    GROUP_14b(
        modRM(ModRMGroup.Opcode._2, "PSRLQ", VRdq, Ib),
        modRM(ModRMGroup.Opcode._3, "PSRLDQ", VRdq, Ib),
        modRM(ModRMGroup.Opcode._6, "PSLLQ", VRdq, Ib),
        modRM(ModRMGroup.Opcode._7, "PSLLDQ", VRdq, Ib)
    ),
    GROUP_15a(
        modRM(ModRMGroup.Opcode._0, "FXSAVE", M),
        modRM(ModRMGroup.Opcode._1, "FXRSTOR", M),
        modRM(ModRMGroup.Opcode._2, "LDMXCSR", Md),
        modRM(ModRMGroup.Opcode._3, "STMXCSR", Md),
        modRM(ModRMGroup.Opcode._7, "CLFLUSH", Mb)
    ),
    GROUP_15b(
        modRM(ModRMGroup.Opcode._5, "LFENCE"),
        modRM(ModRMGroup.Opcode._6, "MFENCE"),
        modRM(ModRMGroup.Opcode._7, "SFENCE")
    ),
    GROUP_16(
        modRM(ModRMGroup.Opcode._0, "PREFETCHNTA"),
        modRM(ModRMGroup.Opcode._1, "PREFETCHT0"),
        modRM(ModRMGroup.Opcode._2, "PREFETCHT1"),
        modRM(ModRMGroup.Opcode._3, "PREFETCHT2")
    ),
    GROUP_P(
        modRM(ModRMGroup.Opcode._0, "PREFETCH"),
        modRM(ModRMGroup.Opcode._1, "PREFETCHW"),
        modRM(ModRMGroup.Opcode._3, "PREFETCH")
    ),
    FP_D8(
        modRM(ModRMGroup.Opcode._0, "FADD", single_real),
        modRM(ModRMGroup.Opcode._1, "FMUL", single_real),
        modRM(ModRMGroup.Opcode._2, "FCOM", single_real),
        modRM(ModRMGroup.Opcode._3, "FCOMP", single_real),
        modRM(ModRMGroup.Opcode._4, "FSUB", single_real),
        modRM(ModRMGroup.Opcode._5, "FSUBR", single_real),
        modRM(ModRMGroup.Opcode._6, "FDIV", single_real),
        modRM(ModRMGroup.Opcode._7, "FDIVR", single_real)
    ),
    FP_D9(
        modRM(ModRMGroup.Opcode._0, "FLD", single_real),
        modRM(ModRMGroup.Opcode._2, "FST", single_real),
        modRM(ModRMGroup.Opcode._3, "FSTP", single_real),
        modRM(ModRMGroup.Opcode._4, "FLDENV", bytes_14_28),
        modRM(ModRMGroup.Opcode._5, "FLDCW", bytes_2),
        modRM(ModRMGroup.Opcode._6, "FSTENV", bytes_14_28),
        modRM(ModRMGroup.Opcode._7, "FSTCW", bytes_2)
    ),
    FP_DA(
        modRM(ModRMGroup.Opcode._0, "FIADD", short_integer),
        modRM(ModRMGroup.Opcode._1, "FIMUL", short_integer),
        modRM(ModRMGroup.Opcode._2, "FICOM", short_integer),
        modRM(ModRMGroup.Opcode._3, "FICOMP", short_integer),
        modRM(ModRMGroup.Opcode._4, "FISUB", short_integer),
        modRM(ModRMGroup.Opcode._5, "FISUBR", short_integer),
        modRM(ModRMGroup.Opcode._6, "FIDIV", short_integer),
        modRM(ModRMGroup.Opcode._7, "FIDIVR", short_integer)
    ),
    FP_DB(
        modRM(ModRMGroup.Opcode._0, "FILD", short_integer),
        modRM(ModRMGroup.Opcode._2, "FIST", short_integer),
        modRM(ModRMGroup.Opcode._3, "FISTP", short_integer),
        modRM(ModRMGroup.Opcode._5, "FLD", extended_real),
        modRM(ModRMGroup.Opcode._7, "FSTP", extended_real)
    ),
    FP_DC(
        modRM(ModRMGroup.Opcode._0, "FADD", double_real),
        modRM(ModRMGroup.Opcode._1, "FMUL", double_real),
        modRM(ModRMGroup.Opcode._2, "FCOM", double_real),
        modRM(ModRMGroup.Opcode._3, "FCOMP", double_real),
        modRM(ModRMGroup.Opcode._4, "FSUB", double_real),
        modRM(ModRMGroup.Opcode._5, "FSUBR", double_real),
        modRM(ModRMGroup.Opcode._6, "FDIV", double_real),
        modRM(ModRMGroup.Opcode._7, "FDIVR", double_real)
    ),
    FP_DD(
        modRM(ModRMGroup.Opcode._0, "FLD", double_real),
        modRM(ModRMGroup.Opcode._2, "FST", double_real),
        modRM(ModRMGroup.Opcode._3, "FSTP", double_real),
        modRM(ModRMGroup.Opcode._4, "FRSTOR", bytes_98_108),
        modRM(ModRMGroup.Opcode._6, "FSAVE", bytes_98_108),
        modRM(ModRMGroup.Opcode._7, "FSTSW", bytes_2)
    ),
    FP_DE(
        modRM(ModRMGroup.Opcode._0, "FIADD", word_integer),
        modRM(ModRMGroup.Opcode._1, "FIMUL", word_integer),
        modRM(ModRMGroup.Opcode._2, "FICOM", word_integer),
        modRM(ModRMGroup.Opcode._3, "FICOMP", word_integer),
        modRM(ModRMGroup.Opcode._4, "FISUB", word_integer),
        modRM(ModRMGroup.Opcode._5, "FISUBR", word_integer),
        modRM(ModRMGroup.Opcode._6, "FIDIV", word_integer),
        modRM(ModRMGroup.Opcode._7, "FIDIVR", word_integer)
    ),
    FP_DF(
        modRM(ModRMGroup.Opcode._0, "FILD", word_integer),
        modRM(ModRMGroup.Opcode._2, "FIST", word_integer),
        modRM(ModRMGroup.Opcode._3, "FISTP", word_integer),
        modRM(ModRMGroup.Opcode._4, "FBLD", packed_bcd),
        modRM(ModRMGroup.Opcode._5, "FILD", long_integer),
        modRM(ModRMGroup.Opcode._6, "FBSTP", packed_bcd),
        modRM(ModRMGroup.Opcode._7, "FISTP", long_integer)
    );

    private static ModRMDescription modRM(ModRMGroup.Opcode opcode, String name, Object... specifications) {
        return new ModRMDescription(opcode, name, new ArraySequence<Object>(specifications));
    }

    private final ModRMDescription[] _instructionDescriptions;

    private AMD64ModRMGroup(ModRMDescription... instructionDescriptions) {
        _instructionDescriptions = instructionDescriptions;
    }

    public ModRMDescription getInstructionDescription(ModRMGroup.Opcode opcode) {
        for (ModRMDescription instructionDescription : _instructionDescriptions) {
            if (instructionDescription.opcode() == opcode) {
                return instructionDescription;
            }
        }
        return null;
    }
}
