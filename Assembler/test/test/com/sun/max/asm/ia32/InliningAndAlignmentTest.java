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
package test.com.sun.max.asm.ia32;

import static com.sun.max.asm.ia32.IA32GeneralRegister32.*;
import static com.sun.max.asm.ia32.IA32IndexRegister32.*;
import static com.sun.max.asm.x86.Scale.*;

import java.io.*;

import junit.framework.*;
import test.com.sun.max.asm.*;

import com.sun.max.asm.*;
import com.sun.max.asm.Assembler.*;
import com.sun.max.asm.dis.ia32.*;
import com.sun.max.asm.ia32.complete.*;
import com.sun.max.ide.*;

/**
 * @author David Liu
 */
public class InliningAndAlignmentTest extends MaxTestCase {
    public InliningAndAlignmentTest() {
        super();
    }

    public InliningAndAlignmentTest(String name) {
        super(name);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(InliningAndAlignmentTest.class.getName());
        //$JUnit-BEGIN$
        suite.addTestSuite(InliningAndAlignmentTest.class);
        //$JUnit-END$
        return suite;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(InliningAndAlignmentTest.class);
    }

    private void disassemble(int startAddress, byte[] bytes, InlineDataDecoder inlineDataDecoder) throws IOException, AssemblyException {
        final IA32Disassembler disassembler = new IA32Disassembler(startAddress, inlineDataDecoder);
        final BufferedInputStream stream = new BufferedInputStream(new ByteArrayInputStream(bytes));
        disassembler.scanAndPrint(stream, System.out);
    }

    private byte[] assembleInlinedData(int startAddress, InlineDataRecorder recorder) throws IOException, AssemblyException {
        // tests inlining of various data types
        final IA32Assembler asm = new IA32Assembler(startAddress);
        final Directives dir = asm.directives();
        final Label skip = new Label();

        asm.mov(EAX, 0x12345678);
        asm.jmp(skip);

        final byte byteValue = (byte) 0x77;
        final Label inlinedByte = new Label();
        asm.bindLabel(inlinedByte);
        dir.inlineByte(byteValue);

        final short shortValue = (short) 0xABCD;
        final Label inlinedShort = new Label();
        asm.bindLabel(inlinedShort);
        dir.inlineShort(shortValue);

        final int intValue = 0x12345678;
        final Label inlinedInt = new Label();
        asm.bindLabel(inlinedInt);
        dir.inlineInt(intValue);

        final long longValue = 0x12345678CAFEBABEL;
        final Label inlinedLong = new Label();
        asm.bindLabel(inlinedLong);
        dir.inlineLong(longValue);

        final byte[] byteArrayValue = {1, 2, 3, 4, 5};
        final Label inlinedByteArray = new Label();
        asm.bindLabel(inlinedByteArray);
        dir.inlineByteArray(byteArrayValue);

        final Label labelValue = skip;
        final Label inlinedLabel = new Label();
        asm.bindLabel(inlinedLabel);
        dir.inlineAddress(labelValue);

        final Label inlinedPaddingByte = new Label();
        asm.bindLabel(inlinedPaddingByte);
        dir.inlineByte((byte) 0);

        dir.align(8);
        asm.mov(EBX, 0xFFFFFFFF);
        asm.bindLabel(skip);
        asm.mov(ECX, 0xCAFEBABE);

        // retrieve the byte stream output of the assembler and confirm that the inlined data is in the expected format, and are aligned correctly
        final byte[] asmBytes = asm.toByteArray(recorder);

        assertTrue(ByteUtils.checkBytes(ByteUtils.toByteArray(byteValue), asmBytes, inlinedByte.position()));
        assertEquals(1, inlinedShort.position() - inlinedByte.position());

        assertTrue(ByteUtils.checkBytes(ByteUtils.toLittleEndByteArray(shortValue), asmBytes, inlinedShort.position()));
        assertEquals(2, inlinedInt.position() - inlinedShort.position());

        assertTrue(ByteUtils.checkBytes(ByteUtils.toLittleEndByteArray(intValue), asmBytes, inlinedInt.position()));
        assertEquals(4, inlinedLong.position() - inlinedInt.position());

        assertTrue(ByteUtils.checkBytes(ByteUtils.toLittleEndByteArray(0x12345678CAFEBABEL), asmBytes, inlinedLong.position()));
        assertEquals(8, inlinedByteArray.position() - inlinedLong.position());

        assertTrue(ByteUtils.checkBytes(byteArrayValue, asmBytes, inlinedByteArray.position()));
        assertEquals(5, inlinedLabel.position() - inlinedByteArray.position());

        assertTrue(ByteUtils.checkBytes(ByteUtils.toLittleEndByteArray(asm.startAddress() + labelValue.position()), asmBytes, inlinedLabel.position()));
        assertEquals(asm.wordWidth().numberOfBytes(), inlinedPaddingByte.position() - inlinedLabel.position());

        return asmBytes;
    }

    private final int _startAddress = 0x12345678;

    public void testInlinedData() throws IOException, AssemblyException {
        System.out.println("--- testInlinedData: ---");
        final InlineDataRecorder recorder = new InlineDataRecorder();
        final byte[] bytes = assembleInlinedData(_startAddress, recorder);
        disassemble(_startAddress, bytes, InlineDataDecoder.createFrom(recorder));
    }

    private byte[] assembleAlignmentPadding(int startAddress, InlineDataRecorder recorder) throws IOException, AssemblyException {
        // test memory alignment directives from 1 byte to 16 bytes
        final IA32Assembler asm = new IA32Assembler(startAddress);
        final Directives dir = asm.directives();

        final Label unalignedLabel1 = new Label();
        final Label alignedLabel1 = new Label();

        final Label unalignedLabel2 = new Label();
        final Label alignedLabel2 = new Label();

        final Label unalignedLabel4By1 = new Label();
        final Label alignedLabel4By1 = new Label();

        final Label unalignedLabel4By2 = new Label();
        final Label alignedLabel4By2 = new Label();

        final Label unalignedLabel4By3 = new Label();
        final Label alignedLabel4By3 = new Label();

        final Label unalignedLabel8 = new Label();
        final Label alignedLabel8 = new Label();

        final Label unalignedLabel16 = new Label();
        final Label alignedLabel16 = new Label();

        asm.jmp(alignedLabel1);
        asm.bindLabel(unalignedLabel1);
        dir.align(1);
        asm.bindLabel(alignedLabel1);
        asm.nop();

        asm.jmp(alignedLabel2);
        asm.bindLabel(unalignedLabel2);
        dir.align(2);
        asm.bindLabel(alignedLabel2);
        asm.nop();

        asm.jmp(alignedLabel4By1);
        dir.inlineByteArray(new byte[]{}); // padding to make the following unaligned by 1 byte
        asm.bindLabel(unalignedLabel4By1);
        dir.align(4);
        asm.bindLabel(alignedLabel4By1);
        asm.nop();

        asm.jmp(alignedLabel4By2);
        dir.inlineByteArray(new byte[]{1, 2, 3}); // padding to make the following unaligned by 2 bytes
        asm.bindLabel(unalignedLabel4By2);
        dir.align(4);
        asm.bindLabel(alignedLabel4By2);
        asm.nop();

        asm.jmp(alignedLabel4By3);
        dir.inlineByteArray(new byte[]{}); // padding to make the following unaligned by 3 bytes
        asm.bindLabel(unalignedLabel4By3);
        dir.align(4);
        asm.bindLabel(alignedLabel4By3);
        asm.nop();

        asm.jmp(alignedLabel8);
        dir.inlineByteArray(new byte[]{1, 2, 3, 4, 5, 6}); // padding to make the following unaligned by 1 byte
        asm.bindLabel(unalignedLabel8);
        dir.align(8);
        asm.bindLabel(alignedLabel8);
        asm.nop();

        asm.jmp(alignedLabel16);
        dir.inlineByteArray(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14});  // padding to make the following unaligned by 1 byte
        asm.bindLabel(unalignedLabel16);
        dir.align(16);
        asm.bindLabel(alignedLabel16);
        asm.nop();

        // check the memory alignment (and that the memory locations were unaligned before the alignment directives)
        final byte[] asmCode = asm.toByteArray(recorder);

        assertEquals(1, (asm.startAddress() + unalignedLabel2.position()) % 2);
        assertEquals(0, (asm.startAddress() + alignedLabel2.position()) % 2);

        assertEquals(1, (asm.startAddress() + unalignedLabel4By1.position()) % 4);
        assertEquals(0, (asm.startAddress() + alignedLabel4By1.position()) % 4);

        assertEquals(2, (asm.startAddress() + unalignedLabel4By2.position()) % 4);
        assertEquals(0, (asm.startAddress() + alignedLabel4By2.position()) % 4);

        assertEquals(3, (asm.startAddress() + unalignedLabel4By3.position()) % 4);
        assertEquals(0, (asm.startAddress() + alignedLabel4By3.position()) % 4);

        assertEquals(1, (asm.startAddress() + unalignedLabel8.position()) % 8);
        assertEquals(0, (asm.startAddress() + alignedLabel8.position()) % 8);

        assertEquals(1, (asm.startAddress() + unalignedLabel16.position()) % 16);
        assertEquals(0, (asm.startAddress() + alignedLabel16.position()) % 16);

        return asmCode;
    }

    public void testAlignmentPadding() throws IOException, AssemblyException {
        System.out.println("--- testAlignmentPadding: ---");
        final InlineDataRecorder recorder = new InlineDataRecorder();
        final byte[] bytes = assembleAlignmentPadding(_startAddress, recorder);
        disassemble(_startAddress, bytes, InlineDataDecoder.createFrom(recorder));
    }

    private byte[] assembleJumpAndAlignmentPadding(int startAddress, InlineDataRecorder recorder) throws IOException, AssemblyException {
        // tests span dependent instruction processing for label and padding instructions
        final IA32Assembler asm = new IA32Assembler(startAddress);
        final Directives dir = asm.directives();
        final Label longJump = new Label();
        final Label shortJump = new Label();

        asm.jmp(longJump); // this instruction is initially 2 bytes long, but will expand to 5 bytes
        final byte[] nopArray = new byte [297];
        java.util.Arrays.fill(nopArray, (byte) 0x90);
        dir.inlineByteArray(nopArray);

        final Label unalignedLocation1 = new Label();
        asm.bindLabel(unalignedLocation1);
        dir.align(8); // initially creates 5 bytes padding, but will reduce to 2 bytes
        final Label alignedLocation1 = new Label();
        asm.bindLabel(alignedLocation1);

        assertEquals(3, (asm.startAddress() + unalignedLocation1.position()) % 8);
        assertEquals(0, (asm.startAddress() + alignedLocation1.position()) % 8);

        asm.bindLabel(longJump);
        asm.jmp(shortJump);

        final Label unalignedLocation2 = new Label();
        asm.bindLabel(unalignedLocation2);
        dir.align(8);
        final Label alignedLocation2 = new Label();
        asm.bindLabel(alignedLocation2);
        asm.nop();

        assertEquals(2, (asm.startAddress() + unalignedLocation2.position()) % 8);
        assertEquals(0, (asm.startAddress() + alignedLocation2.position()) % 8);

        asm.bindLabel(shortJump);
        asm.nop();
        asm.nop();

        final Label unalignedLocation3 = new Label();
        asm.bindLabel(unalignedLocation3);
        dir.align(8);
        final Label alignedLocation3 = new Label();
        asm.bindLabel(alignedLocation3);

        assertEquals(3, (asm.startAddress() + unalignedLocation3.position()) % 8);
        assertEquals(0, (asm.startAddress() + alignedLocation3.position()) % 8);

        final byte[] asmCode = asm.toByteArray(recorder);

        assertEquals(6, (asm.startAddress() + unalignedLocation1.position()) % 8);
        assertEquals(0, (asm.startAddress() + alignedLocation1.position()) % 8);

        assertEquals(2, (asm.startAddress() + unalignedLocation2.position()) % 8);
        assertEquals(0, (asm.startAddress() + alignedLocation2.position()) % 8);

        assertEquals(3, (asm.startAddress() + unalignedLocation3.position()) % 8);
        assertEquals(0, (asm.startAddress() + alignedLocation3.position()) % 8);

        return asmCode;
    }

    public void testJumpAndAlignmentPadding() throws IOException, AssemblyException {
        System.out.println("--- testJumpAndAlignmentPadding: ---");
        final InlineDataRecorder recorder = new InlineDataRecorder();
        final byte[] bytes = assembleJumpAndAlignmentPadding(_startAddress, recorder);
        disassemble(_startAddress, bytes, InlineDataDecoder.createFrom(recorder));
    }

    private byte[] assembleInvalidInstructionDisassembly(int startAddress, InlineDataRecorder recorder) throws IOException, AssemblyException {
        // tests span dependent instruction processing for label and padding instructions
        final IA32Assembler asm = new IA32Assembler(startAddress);
        final Directives dir = asm.directives();
        final Label jumpTarget1 = new Label();
        final Label jumpTarget2 = new Label();
        final Label jumpTarget3 = new Label();

        asm.jmp(jumpTarget3);

        dir.inlineAddress(jumpTarget1);
        dir.inlineAddress(jumpTarget2);
        dir.inlineAddress(jumpTarget3);
        for (int i = 0; i <= 0xFF; i++) {
            dir.inlineByte((byte) i);
        }

        asm.bindLabel(jumpTarget1);
        asm.mov(EAX, 0x12345678);

        asm.bindLabel(jumpTarget2);
        asm.nop();

        dir.inlineByte((byte) 0xEB); // this is the first byte of a two-byte instruction
        asm.bindLabel(jumpTarget3);
        asm.nop();

        return asm.toByteArray(recorder);
    }

    public void testInvalidInstructionDisassembly() throws IOException, AssemblyException {
        System.out.println("--- testInvalidInstructionDisassembly: ---");
        final InlineDataRecorder recorder = new InlineDataRecorder();
        final byte[] bytes = assembleInvalidInstructionDisassembly(_startAddress, recorder);
        disassemble(_startAddress, bytes, InlineDataDecoder.createFrom(recorder));
    }

    private byte[] assembleSwitchTable(int startAddress, InlineDataRecorder recorder) throws IOException, AssemblyException {
        final IA32Assembler asm = new IA32Assembler(startAddress);
        final Label skip = new Label();
        final Label table = new Label();
        final Label case1 = new Label();
        final Label case2 = new Label();
        final Label case3 = new Label();

        /*
             switch (rsi) {
                 case 0: rcx = 0xDEADBEEFDEADBEEFL; break;
                 case 1: rcx = 0xCAFEBABECAFEBABEL; break;
                 case 2: rcx = 0xFFFFFFFFFFFFFFFFL; break;
             }
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

        return asm.toByteArray(recorder);
    }

    public void testSwitchTable() throws IOException, AssemblyException {
        System.out.println("--- testSwitchTable: ---");
        final InlineDataRecorder recorder = new InlineDataRecorder();
        final byte[] bytes = assembleSwitchTable(_startAddress, recorder);
        disassemble(_startAddress, bytes, InlineDataDecoder.createFrom(recorder));
    }
}
