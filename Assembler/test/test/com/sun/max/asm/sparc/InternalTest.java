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
package test.com.sun.max.asm.sparc;

import static com.sun.max.asm.sparc.AnnulBit.*;
import static com.sun.max.asm.sparc.BranchPredictionBit.*;
import static com.sun.max.asm.sparc.GPR.*;
import static com.sun.max.asm.sparc.ICCOperand.*;
import static com.sun.max.asm.sparc.MembarOperand.*;

import java.io.*;

import junit.framework.*;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.sparc.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
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

    private byte[] assemble(SPARCAssembler asm) throws IOException, AssemblyException {
        asm.rd(StateRegister.PC, G1);

        asm.add(G0, I1, O2);
        asm.sub(G5, 12, G7);
        asm.movvc(ICC, -12, G7);
        asm.sethi(0x1234, G7);

        // Example code from J.6:
        final Label retry = new Label();
        final Label loop = new Label();
        final Label out = new Label();

        asm.bindLabel(retry);
        asm.ldstub(G1, G4, I0);
        asm.tst(I0);
        asm.be(out);
        asm.nop();
        asm.bindLabel(loop);
        asm.ldub(G1, G4, I0);
        asm.tst(I0);
        asm.bne(loop);
        asm.nop();
        asm.ba(retry);
        asm.ba(A, PT, ICC, retry);
        asm.bindLabel(out);
        asm.rd(StateRegister.PC, L0);

        try {
            asm.sethi(0x0fffffff, G1);
            fail();
        } catch (IllegalArgumentException illegalArgumentException) {
        }

        asm.membar(LOAD_LOAD.or(LOAD_STORE));
        asm.membar(NO_MEMBAR.or(LOAD_STORE));

        return asm.toByteArray();
    }

    private void disassemble(SPARCDisassembler disassembler, byte[] bytes) throws IOException, AssemblyException {
        final BufferedInputStream stream = new BufferedInputStream(new ByteArrayInputStream(bytes));
        disassembler.scanAndPrint(stream, System.out);
    }

    public void test32() throws IOException, AssemblyException {
        final int startAddress = 0x12340000;
        final SPARC32Assembler assembler = new SPARC32Assembler(startAddress);
        final SPARC32Disassembler disassembler = new SPARC32Disassembler(startAddress, null);
        final byte[] bytes = assemble(assembler);
        disassemble(disassembler, bytes);
        System.out.println();
    }

    public void test64() throws IOException, AssemblyException {
        final long startAddress = 0x1234567812340000L;
        final SPARC64Assembler assembler = new SPARC64Assembler(startAddress);
        final SPARC64Disassembler disassembler = new SPARC64Disassembler(startAddress, null);
        final byte[] bytes = assemble(assembler);
        disassemble(disassembler, bytes);
        System.out.println();
    }
}
