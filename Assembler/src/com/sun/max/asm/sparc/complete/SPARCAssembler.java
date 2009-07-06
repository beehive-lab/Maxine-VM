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
package com.sun.max.asm.sparc.complete;

import static com.sun.max.asm.sparc.GPR.*;

import com.sun.max.asm.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * The base class for the 32-bit and 64-bit SPARC assemblers. This class also defines
 * the more complex synthetic SPARC instructions.
 *
 * @author Bernd Mathiske
 * @author Dave Ungar
 * @author Adam Spitz
 * @author Greg Wright
 */
public abstract class SPARCAssembler extends SPARCLabelAssembler {

    public static SPARCAssembler createAssembler(WordWidth wordWidth) {
        switch (wordWidth) {
            case BITS_32:
                return new SPARC32Assembler();
            case BITS_64:
                return new SPARC64Assembler();
            default:
                throw ProgramError.unexpected("Invalid word width specification");
        }
    }

    @Override
    protected void emitPadding(int numberOfBytes) throws AssemblyException {
        if ((numberOfBytes % 4) != 0) {
            throw new AssemblyException("Cannot pad instruction stream with a number of bytes not divisble by 4");
        }
        for (int i = 0; i < numberOfBytes >> 2; i++) {
            nop();
        }
    }

    // Utilities:

    public int hi(int i) {
        return (i & 0xfffffc00) >> 10;
    }

    public int lo(int i) {
        return i & 0x000003ff;
    }

    public int hi(long i) {
        return hi((int) i);
    }

    public int lo(long i) {
        return lo((int) i);
    }

    public int uhi(long i) {
        return hi((int) (i >> 32));
    }

    public int ulo(long i) {
        return lo((int) (i >> 32));
    }

    public static boolean isSimm11(int value) {
        return Ints.numberOfEffectiveSignedBits(value) <= 11;
    }

    public static boolean isSimm13(int value) {
        return Ints.numberOfEffectiveSignedBits(value) <= 13;
    }

    public static boolean isSimm13(long value) {
        return Longs.numberOfEffectiveSignedBits(value) <= 13;
    }

    public static boolean isSimm19(int value) {
        return Ints.numberOfEffectiveSignedBits(value) <= 19;
    }

    public int setswNumberOfInstructions(int imm) {
        if (0 <= imm && lo(imm) == 0) {
            return 1;
        } else if (-4096 <= imm && imm <= 4095) {
            return 1;
        } else if (imm < 0 && lo(imm) == 0) {
            return 2;
        } else if (imm >= 0) {
            return 2;
        } else {
            return 3;
        }
    }

    public int setuwNumberOfInstructions(int imm) {
        if (lo(imm) == 0) {
            return 1;
        } else if (0 <= imm && imm <= 4095) {
            return 1;
        } else {
            return 2;
        }
    }
    // Complex synthetic instructions according to appendix G3 of the SPARC Architecture Manual V9:

    public void setuw(int imm, GPR rd) throws AssemblyException {
        if (lo(imm) == 0) {
            sethi(hi(imm), rd);
        } else if (0 <= imm && imm <= 4095) {
            or(G0, imm, rd);
        } else {
            sethi(hi(imm), rd);
            or(rd, lo(imm), rd);
        }
    }

    public void set(int imm, GPR rd) throws AssemblyException {
        setuw(imm, rd);
    }

    public void setsw(int imm, GPR rd) throws AssemblyException {
        if (0 <= imm && lo(imm) == 0) {
            sethi(hi(imm), rd);
        } else if (-4096 <= imm && imm <= 4095) {
            or(G0, imm, rd);
        } else if (imm < 0 && lo(imm) == 0) {
            sethi(hi(imm), rd);
            sra(rd, G0, rd);
        } else if (imm >= 0) {
            sethi(hi(imm), rd);
            or(rd, lo(imm), rd);
        } else {
            sethi(hi(imm), rd);
            or(rd, lo(imm), rd);
            sra(rd, G0, rd);
        }
    }

    public void setx(long imm, GPR temp, GPR rd) throws AssemblyException {
        sethi(uhi(imm), temp);
        or(temp, ulo(imm), temp);
        sllx(temp, 32, temp);
        sethi(hi(imm), rd);
        or(rd, temp, rd);
        or(rd, lo(imm), rd);
    }

    public void sethi(Label label, final GPR rd) {
        final int startPosition = currentPosition();
        emitInt(0);
        new InstructionWithAddress(this, startPosition, startPosition + 4, label) {
            @Override
            protected void assemble() throws AssemblyException {
                final int imm22 = hi(addressAsInt());
                sethi(imm22, rd);
            }
        };
    }


    public void add(final GPR rs1, final Label label, final GPR rd) {
        final int startPosition = currentPosition();
        emitInt(0);
        new InstructionWithAddress(this, startPosition, startPosition + 4, label) {
            @Override
            protected void assemble() throws AssemblyException {
                final int simm13 = lo(addressAsInt());
                add(rs1, simm13, rd);
            }
        };
    }

    public void ld(final GPR rs1, final Label base, final Label target, final SFPR rd) {
        final int startPosition = currentPosition();
        emitInt(0);
        new InstructionWithOffset(this, startPosition, startPosition + 4, target) {
            @Override
            protected void assemble() throws AssemblyException {
                ld(rs1, labelOffsetRelative(target, base), rd);
            }
        };
    }

    public void ldd(final GPR rs1, final Label base, final Label target, final DFPR rd) {
        final int startPosition = currentPosition();
        emitInt(0);
        new InstructionWithOffset(this, startPosition, startPosition + 4, target) {
            @Override
            protected void assemble() throws AssemblyException {
                ldd(rs1, labelOffsetRelative(target, base), rd);
            }
        };
    }

    public void ldx(final GPR rs1, final Label base, final Label target, final GPR rd) {
        final int startPosition = currentPosition();
        emitInt(0);
        new InstructionWithOffset(this, startPosition, startPosition + 4, target) {
            @Override
            protected void assemble() throws AssemblyException {
                ldx(rs1, labelOffsetRelative(target, base), rd);
            }
        };
    }

    public void add(final GPR rs1, final Label base, final Label target, final GPR rd) {
        final int startPosition = currentPosition();
        emitInt(0);
        new InstructionWithOffset(this, startPosition, startPosition + 4, target) {
            @Override
            protected void assemble() throws AssemblyException {
                add(rs1, labelOffsetRelative(target, base), rd);
            }
        };
    }
    public void addcc(final GPR rs1, final Label base, final Label target, final GPR rd) {
        final int startPosition = currentPosition();
        emitInt(0);
        new InstructionWithOffset(this, startPosition, startPosition + 4, target) {
            @Override
            protected void assemble() throws AssemblyException {
                addcc(rs1, labelOffsetRelative(target, base), rd);
            }
        };
    }
}
