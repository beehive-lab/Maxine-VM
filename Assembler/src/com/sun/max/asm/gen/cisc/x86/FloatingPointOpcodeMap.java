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
package com.sun.max.asm.gen.cisc.x86;

import static com.sun.max.asm.gen.cisc.x86.FloatingPointOperandCode.*;
import static com.sun.max.asm.ia32.IA32GeneralRegister16.*;
import static com.sun.max.asm.x86.FPStackRegister.*;
import static com.sun.max.util.HexByte.*;

import com.sun.max.asm.gen.*;

/**
 * @author Bernd Mathiske
 */
public class FloatingPointOpcodeMap extends X86InstructionDescriptionCreator {

    private void create_D8() {
        define(_D8, _C0, "FADD", ST, ST_i);
        define(_D8, _C8, "FMUL", ST, ST_i);
        define(_D8, _D0, "FCOM", ST_i);
        define(_D8, _D8, "FCOMP", ST_i);
        define(_D8, _E0, "FSUB", ST, ST_i);
        define(_D8, _E8, "FSUBR", ST, ST_i);
        define(_D8, _F0, "FDIV", ST, ST_i);
        define(_D8, _F8, "FDIVR", ST, ST_i);
    }

    private void create_D9() {
        define(_D9, _C0, "FLD", ST_i);
        define(_D9, _C8, "FXCH", ST_i);

        define(_D9, _D0, "FNOP");
        define(_D9, _D8, "FSTP1", ST_i).beNotExternallyTestable(); // not implemented by gas, since this is a redundant instruction

        define(_D9, _E0, "FCHS");
        define(_D9, _E1, "FABS");
        define(_D9, _E4, "FTST");
        define(_D9, _E5, "FXAM");
        define(_D9, _E8, "FLD1");
        define(_D9, _E9, "FLDL2T");
        define(_D9, _EA, "FLDL2E");
        define(_D9, _EB, "FLDPI");
        define(_D9, _EC, "FLDLG2");
        define(_D9, _ED, "FLDLN2");
        define(_D9, _EE, "FLDZ");

        define(_D9, _F0, "F2XM1");
        define(_D9, _F1, "FYL2X");
        define(_D9, _F2, "FPTAN");
        define(_D9, _F3, "FPATAN");
        define(_D9, _F4, "FXTRACT");
        define(_D9, _F5, "FPREM1");
        define(_D9, _F6, "FDECSTP");
        define(_D9, _F7, "FINCSTP");
        define(_D9, _F8, "FPREM");
        define(_D9, _F9, "FYL2XP1");
        define(_D9, _FA, "FSQRT");
        define(_D9, _FB, "FSINCOS");
        define(_D9, _FC, "FRNDINT");
        define(_D9, _FD, "FSCALE");
        define(_D9, _FE, "FSIN");
        define(_D9, _FF, "FCOS");
    }

    private void create_DA() {
        define(_DA, _C0, "FCMOVB", ST, ST_i);
        define(_DA, _C8, "FCMOVE", ST, ST_i);
        define(_DA, _D0, "FCMOVBE", ST, ST_i);
        define(_DA, _D8, "FCMOVU", ST, ST_i);
        define(_DA, _E9, "FUCOMPP");
    }

    private void create_DB() {
        define(_DB, _C0, "FCMOVNB", ST, ST_i);
        define(_DB, _C8, "FCMOVNE", ST, ST_i);
        define(_DB, _D0, "FCMOVNBE", ST, ST_i);
        define(_DB, _D8, "FCMOVNU", ST, ST_i);
        define(_DB, _E2, "FCLEX");
        define(_DB, _E3, "FINIT");
        define(_DB, _E8, "FUCOMI", ST, ST_i);
        define(_DB, _F0, "FCOMI", ST, ST_i);
    }

    private void create_DC() {
        define(_DC, _C0, "FADD", ST_i.excludeExternalTestArguments(ST_0, ST), ST); //gas uses D8 if both operands are ST(0)
        define(_DC, _C8, "FMUL", ST_i.excludeExternalTestArguments(ST_0, ST), ST); //gas uses D8 if both operands are ST(0)
        define(_DC, _D0, "FCOM2", ST_i).beNotExternallyTestable(); // not implemented by gas, since this is a redundant instruction
        define(_DC, _D8, "FCOMP3", ST_i).beNotExternallyTestable(); // not implemented by gas, since this is a redundant instruction
        define(_DC, _E0, "FSUB", ST_i.excludeExternalTestArguments(ST_0, ST), ST); //gas uses D8 if both operands are ST(0)
        define(_DC, _E8, "FSUBR", ST_i.excludeExternalTestArguments(ST_0, ST), ST); //gas uses D8 if both operands are ST(0)
        define(_DC, _F0, "FDIV", ST_i.excludeExternalTestArguments(ST_0, ST), ST); //gas uses D8 if both operands are ST(0)
        define(_DC, _F8, "FDIVR", ST_i.excludeExternalTestArguments(ST_0, ST), ST); //gas uses D8 if both operands are ST(0)
    }

    private void create_DD() {
        define(_DD, _C0, "FFREE", ST_i);
        define(_DD, _C8, "FXCH4", ST_i).beNotExternallyTestable(); // not implemented by gas, since this is a redundant instruction
        define(_DD, _D0, "FST", ST_i);
        define(_DD, _D8, "FSTP", ST_i);
        define(_DD, _E0, "FUCOM", ST_i);
        define(_DD, _E8, "FUCOMP", ST_i);
    }

    private void create_DE() {
        define(_DE, _C0, "FADDP", ST_i, ST);
        define(_DE, _C8, "FMULP", ST_i, ST);
        define(_DE, _D0, "FCOMP5", ST_i, ST).beNotExternallyTestable(); // not implemented by gas, since this is a redundant instruction
        define(_DE, _D9, "FCOMPP");
        define(_DE, _E0, "FSUBRP", ST_i, ST).setExternalName("fsubp"); // gas bug: confounding FSUBRP and FSUBP
        define(_DE, _E8, "FSUBP", ST_i, ST).setExternalName("fsubrp"); // gas bug: confounding FSUBRP and FSUBP
        define(_DE, _F0, "FDIVRP", ST_i, ST).setExternalName("fdivp"); // gas bug: confounding fdivrp and fdivp
        define(_DE, _F8, "FDIVP", ST_i, ST).setExternalName("fdivrp"); // gas bug: confounding fdivrp and fdivp
    }

    private void create_DF() {
        define(_DF, _C0, "FFREEP", ST_i);
        define(_DF, _C8, "FXCH7", ST_i).beNotExternallyTestable(); // not implemented by gas, since this is a redundant instruction
        define(_DF, _D0, "FSTP8", ST_i).beNotExternallyTestable(); // not implemented by gas, since this is a redundant instruction
        define(_DF, _D8, "FSTP9", ST_i).beNotExternallyTestable(); // not implemented by gas, since this is a redundant instruction
        define(_DF, _E0, "FSTSW", AX);
        define(_DF, _E8, "FUCOMIP", ST, ST_i);
        define(_DF, _F0, "FCOMIP", ST, ST_i);
    }

    public FloatingPointOpcodeMap(Assembly<? extends X86Template> assembly) {
        super(assembly);
        create_D8();
        create_D9();
        create_DA();
        create_DB();
        create_DC();
        create_DD();
        create_DE();
        create_DF();
    }
}
