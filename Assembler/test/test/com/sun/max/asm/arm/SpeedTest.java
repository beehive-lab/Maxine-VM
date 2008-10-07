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
/*VCSID=46257266-01be-428c-b0c9-e81424029a2a*/
package test.com.sun.max.asm.arm;

import java.io.*;

import junit.framework.*;

import com.sun.max.asm.*;
import com.sun.max.asm.arm.complete.*;
import com.sun.max.ide.*;

/**
 * @author Sumeet Panchal
 */
public class SpeedTest extends MaxTestCase {

    public SpeedTest() {
        super();

    }

    public SpeedTest(String name) {
        super(name);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(SpeedTest.class.getName());
        //$JUnit-BEGIN$
        suite.addTestSuite(SpeedTest.class);
        //$JUnit-END$
        return suite;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SpeedTest.class);
    }

    public byte[] produce()  throws IOException, AssemblyException {
        final int startAddress = 0x0000ecf0;
        final ARMAssembler asm = new ARMAssembler(startAddress);
        //final Label label1 = new Label();
        /*
        asm.mflr(R0);
        asm.stwu(SP, -96, SP);
        asm.stmw(R23, 60, SP);
        asm.stw(R0, 100, SP);
        asm.mr(R23, R3);
        asm.mr(R31, R23);
        asm.cmplwi(R31, 2);
        asm.blt(CR0, label1, NONE);
        asm.addic(R30, R31, -1);
        asm.addic(R29, R31, -2);
        asm.mr(R3, R30);
        asm.mr(R3, R30);
        asm.lis(R24, 0);
        asm.ori(R24, R24, 60656);
        asm.mtctr(R24);
        asm.bctrl();
        asm.mr(R30, R3);
        asm.mr(R3, R29);
        asm.lis(R24, 0);
        asm.ori(R24, R24, 60656);
        asm.mtctr(R24);
        asm.bctrl();
        asm.mr(R29, R3);
        asm.addic(R30, R30, 1);
        asm.add(R3, R30, R29);
        asm.lwz(R0, 100, SP);
        asm.mtlr(R0);
        asm.lmw(R23, 60, SP);
        asm.addi(SP, SP, 96);
        asm.blr();
        asm.bindLabel(label1);
        asm.li(R3, 1);
        asm.lwz(R0, 100, SP);
        asm.mtlr(R0);
        asm.lmw(R23, 60, SP);
        asm.addi(SP, SP, 96);
        asm.blr();
        */
        //asm.adc(GPR.R0, GPR.R0, 1);
        return asm.toByteArray();
    }

    public void test_speed() throws IOException, AssemblyException {
        System.out.println("start");
        for (int i = 0; i < 10000000; i++) {
            produce();
        }
        System.out.println("done.");
        //final PPC32Disassembler disassembler = new PPC32Disassembler(startAddress);
        //disassemble(disassembler, bytes);
    }
}
