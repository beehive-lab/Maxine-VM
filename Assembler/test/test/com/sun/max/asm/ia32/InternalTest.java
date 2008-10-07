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
/*VCSID=c5b6d192-c284-4b63-900e-2415684e1530*/
package test.com.sun.max.asm.ia32;

import static com.sun.max.asm.ia32.IA32GeneralRegister16.*;
import static com.sun.max.asm.ia32.IA32GeneralRegister32.*;
import static com.sun.max.asm.ia32.IA32GeneralRegister8.*;
import static com.sun.max.asm.ia32.IA32IndexRegister32.*;
import static com.sun.max.asm.x86.Scale.*;

import java.io.*;

import junit.framework.*;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.ia32.*;
import com.sun.max.asm.ia32.*;
import com.sun.max.asm.ia32.complete.*;
import com.sun.max.asm.x86.*;
import com.sun.max.ide.*;

/**
 * @author Bernd Mathiske
 */
public class InternalTest extends MaxTestCase {

    public InternalTest() {
        super();
    }

    public InternalTest(String name) {
        super(name);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(InternalTest.class.getName());
        //$JUnit-BEGIN$
        suite.addTestSuite(InternalTest.class);
        //$JUnit-END$
        return suite;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(InternalTest.class);
    }

    private byte[] assemble(int startAddress) throws IOException, AssemblyException {
        final IA32GeneralRegister32 myGPR = EAX;
        final IA32Assembler asm = new IA32Assembler(startAddress);
        final Label startLabel = new Label();
        final Label endLabel = new Label();
        final Label label1 = new Label();
        final Label label2 = new Label();
        final Label label3 = new Label();
        final Label fixLabel = new Label();

        asm.bindLabel(startLabel);
        asm.jmp(label1);
        asm.jmp(startLabel);
        asm.call(fixLabel);
        /* 1*/ asm.add(EDX.indirect(), BL);
        /* 2*/ asm.add(EAX.base(), EBX.index(), SCALE_2, CL);
        /* 3*/ asm.m_add(0x1234AB78, EBP.index(), SCALE_1, AH);
        /* 4*/ asm.m_add(0x12345678, BH);
        /* 5*/ asm.add((byte) 0x23, EAX.indirect(), AL);
        /* 6*/ asm.add((byte) 0x23, EAX.base(), EBX.index(), SCALE_4, CL);
        /* 7*/ asm.add(0x87654321, EDI.indirect(), CH);
        /* 8*/ asm.add(0x87654321, EAX.base(), EBX.index(), SCALE_1, DL);
        /* 9*/ asm.add(CL, DL);
        asm.fixLabel(fixLabel, startAddress + 52); // choose the right addend according to output to hit an instruction start address
        /*10*/ asm.add(ESI.indirect(), EDX);
        /*11*/ asm.add(EAX.base(), EBX.index(), SCALE_8, ECX);
        /*12*/ asm.m_add(0x12345678, EDX.index(), SCALE_1, EAX);
        /*13*/ asm.m_add(0x12345678, EBP);
        /*14*/ asm.add((byte) 0x16, EAX.indirect(), EBX);
        /*15*/ asm.add((byte) 0x20, EAX.base(), EBX.index(), SCALE_4, ECX);
        /*16*/ asm.add(0x87654321, EDX.indirect(), EAX);
        /*17*/ asm.add(0x87654321, EAX.base(), EBX.index(), SCALE_4, ECX);
        /*18*/ asm.add(EAX, EAX);
        asm.jmp(fixLabel);
        /*19*/ asm.add(ECX.indirect(), DX);
        /*20*/ asm.add(EAX.base(), EBX.index(), SCALE_2, DX);
        /*21*/ asm.m_add(0x12345678, EDX.index(), SCALE_1, AX);
        /*22*/ asm.m_add(0x12345678, BP);
        asm.jmp(label3);
        /*23*/ asm.add(0x87654321, EDX.indirect(), AX);
        /*24*/ asm.add((byte) 0x20, EAX.base(), EBX.index(), SCALE_4, CX);
        /*25*/ asm.add(0x87654321, EDX.indirect(), AX);
        /*26*/ asm.add(0x87654321, EAX.base(), EBX.index(), SCALE_4, CX);
        /*27*/ asm.add(BX, CX);
        /*28*/ asm.add(AL, EAX.indirect());
        /*29*/ asm.add(BX, EAX.base(), EBX.index(), SCALE_4);
        /*30*/ asm.m_add(AH, 0x12345678, EBP.index(), SCALE_1);
        /*31*/ asm.m_add(DH, 0x12345678);
        asm.bindLabel(label3);
        /*32*/ asm.add(BL, (byte) 0x16, EAX.indirect());
        /*33*/ asm.add(CL, (byte) 0x20, EAX.base(), EBX.index(), SCALE_4);
        /*34*/ asm.add(AL, 0x87654321, EDX.indirect());
        /*35*/ asm.add(AL, 0x87654321, EAX.base(), EBX.index(), SCALE_4);
        /*36*/ asm.add(EAX, ECX.indirect());
        asm.jmp(label3);
        asm.call(startLabel);
        /*37*/ asm.add(EBX, EAX.base(), ECX.index(), SCALE_2);
        /*38*/ asm.m_add(ESI, 0x12345678, EAX.index(), SCALE_1);
        /*39*/ asm.m_add(ESP, 0x12345678);
        /*40*/ asm.add(EAX, (byte) 0x16, EAX.indirect());
        /*41*/ asm.add(ECX, (byte) 0x20, EAX.base(), EBX.index(), SCALE_4);
        /*42*/ asm.add(EDX, 0x87AB4321, EDX.indirect());
        /*43*/ asm.add(EBX, 0x87654321, EAX.base(), EBX.index(), SCALE_4);
        /*44*/ asm.add(CX, EBX.indirect());
        asm.jmp(endLabel);
        /*45*/ asm.add(BX, EAX.base(), ECX.index(), SCALE_2);
        /*46*/ asm.m_add(SI, 0x12345678, EAX.index(), SCALE_1);
        /*47*/ asm.m_add(SP, 0x1234AB78);
        /*48*/ asm.add(AX, (byte) 0x16, EAX.indirect());
        /*49*/ asm.add(CX, (byte) 0x20, EAX.base(), EBX.index(), SCALE_4);
        /*50*/ asm.add(DX, 0x87654321, EDX.indirect());
        /*51*/ asm.add(BX, 0x87AB4321, EAX.base(), EBX.index(), SCALE_1);
        /*52*/ asm.add_AL((byte) 0x08);
        /*53*/ asm.add_EAX(0x12344321);
        /*54*/ asm.add_AX((short) 0x1234);
        /*55*/ asm.push_ES();
        asm.addl(ECX, 7);
        asm.bindLabel(label1);
        asm.cmpl(myGPR.indirect(), (byte) 7);
        asm.subl((byte) 4, EBX.base(), ECX.index(), Scale.SCALE_2, 5);
        asm.jmp(label1);
        asm.fixLabel(label2, 0x12345678);
        asm.call(label2);
        asm.jmp(startLabel);
        asm.bindLabel(endLabel);
        return asm.toByteArray();
    }

    private void disassemble(int startAddress, byte[] bytes) throws IOException, AssemblyException {
        final IA32Disassembler disassembler = new IA32Disassembler(startAddress);
        final BufferedInputStream stream = new BufferedInputStream(new ByteArrayInputStream(bytes));
        disassembler.scanAndPrint(stream, System.out);
    }

    public void test() throws IOException, AssemblyException {
        final int startAddress = 0x12358;
        final byte[] bytes = assemble(startAddress);
        disassemble(startAddress, bytes);
    }

    private byte[] assembleSwitchTable(int startAddress) throws IOException, AssemblyException {
        final IA32Assembler asm = new IA32Assembler(startAddress);
        final Label skip = new Label();
        final Label table = new Label();
        final Label case1 = new Label();
        final Label case2 = new Label();
        final Label case3 = new Label();

        /*

         switch (esi) {
            case 1: ecx = 0xDEADBEEF; break;
            case 2: ecx = 0xCAFEBABE; break;
            case 3: ecx = 0xFFFFFFFF; break;
         }

         * 
         */
        asm.mov(ESI, 1);
        asm.m_jmp(table, ESI_INDEX, SCALE_4);
        asm.nop();

        asm.directives().align(4);
        asm.bindLabel(table);
        asm.directives().inlineAddress(case1);
        asm.directives().inlineAddress(case2);
        asm.directives().inlineAddress(case3);

        asm.directives().align(4);
        asm.bindLabel(case1);
        asm.mov(ECX, 0xDEADBEEF);
        asm.jmp(skip);

        asm.bindLabel(case2);
        asm.mov(ECX, 0xCAFEBABE);
        asm.jmp(skip);

        asm.bindLabel(case3);
        asm.mov(ECX, 0xFFFFFFFF);
        asm.jmp(skip);

        asm.bindLabel(skip);
        asm.nop();

        return asm.toByteArray();
    }

    public void testSwitchTable() throws IOException, AssemblyException {
        System.out.println("--- testSwitchTable: ---");
        final int startAddress = 0x12345678;
        final byte[] bytes = assembleSwitchTable(startAddress);
        disassemble(startAddress, bytes);
    }
}
