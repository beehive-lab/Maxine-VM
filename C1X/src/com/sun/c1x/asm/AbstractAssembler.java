/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.asm;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.target.*;
import com.sun.c1x.util.*;

/**
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public abstract class AbstractAssembler {

    protected final Buffer codeBuffer;
    protected final Buffer dataBuffer;
    private int lastInstructionStart;
    private int lastDecodeStart;

    public static final int InvalidInstructionMark = -1;

    protected OopRecorder oopRecorder; // support for relocInfo.oopType
    public final C1XCompilation compilation;

    protected boolean is8bit(int x) {
        return -0x80 <= x && x < 0x80;
    }

    protected boolean isByte(int x) {
        return 0 <= x && x < 0x100;
    }

    protected boolean isShiftCount(int x) {
        return 0 <= x && x < 32;
    }

    public int pc() {
        return codeBuffer.position();
    }

    public int offset() {
        return codeBuffer.position();
    }

    public OopRecorder oopRecorder() {
        return oopRecorder;
    }

    public void setOopRecorder(OopRecorder r) {
        oopRecorder = r;
    }

    public AbstractAssembler(C1XCompilation compilation) {
        this.compilation = compilation;
        this.codeBuffer = new Buffer(compilation.target.arch.bitOrdering);
        this.dataBuffer = new Buffer(compilation.target.arch.bitOrdering);
        oopRecorder = new OopRecorder();
        lastInstructionStart = InvalidInstructionMark;
    }

    // Inform CodeBuffer that incoming code and relocation will be for stubs
    Address startAConst(int requiredSpace, int requiredAlign) {
        // TODO: Figure out how to do this in Java!
// assert codeSection == cb.insts() : "not in insts?";
// sync();
// Address end = cs.end();
// int pad = -end.asInt() & (requiredAlign - 1);
// if (cs.maybeExpandToEnsureRemaining(pad + requiredSpace)) {
// if (cb.blob() == null) {
// return null;
// }
// end = cs.end(); // refresh pointer
// }
// if (pad > 0) {
// while (--pad >= 0) {
// *end++ = 0;
// }
// cs.setEnd(end);
// }
// return end;
        throw Util.unimplemented();
    }

    public void aByte(int x) {
        emitByte(x);
    }

    void aLong(int x) {
        emitInt(x);
    }

    void print(Label l) {
        if (l.isBound()) {
            TTY.println(String.format("bound label to %d", l.loc()));
        } else if (l.isUnbound()) {
            l.printInstructions(this);
        } else {
            TTY.println(String.format("label in inconsistent state (loc = %d)", l.loc()));
        }
    }

    public void bind(Label l) {
        if (l.isBound()) {
            // Assembler can bind a label more than once to the same place.
            Util.guarantee(l.loc() == offset(), "attempt to redefine label");
            return;
        }
        l.bindLoc(offset());
        l.patchInstructions(this);
    }

    protected void generateStackOverflowCheck(int frameSizeInBytes) {
        if (C1XOptions.UseStackBanging) {
            // Each code entry causes one stack bang n pages down the stack where n
            // is configurable by StackBangPages. The setting depends on the maximum
            // depth of VM call stack or native before going back into java code,
            // since only java code can raise a stack overflow exception using the
            // stack banging mechanism. The VM and native code does not detect stack
            // overflow.
            // The code in JavaCalls.call() checks that there is at least n pages
            // available, so all entry code needs to do is bang once for the end of
            // this shadow zone.
            // The entry code may need to bang additional pages if the framesize
            // is greater than a page.

            int pageSize = compilation.runtime.vmPageSize();
            int bangEnd = C1XOptions.StackShadowPages * pageSize;

            // This is how far the previous frame's stack banging extended.
            int bangEndSafe = bangEnd;

            if (frameSizeInBytes > pageSize) {
                bangEnd += frameSizeInBytes;
            }

            int bangOffset = bangEndSafe;
            while (bangOffset <= bangEnd) {
                // Need at least one stack bang at end of shadow zone.
                bangStackWithOffset(bangOffset);
                bangOffset += pageSize;
            }
        } // end (UseStackBanging)
    }

    protected abstract void bangStackWithOffset(int bangOffset);

    protected abstract int codeFillByte();

    protected void emitByte(int x) {
        codeBuffer.emitByte(x);
    }

    protected void emitShort(int x) {
        codeBuffer.emitShort(x);
    }

    protected void emitInt(int x) {
        codeBuffer.emitInt(x);
    }

    protected void emitLong(long x) {
        codeBuffer.emitLong(x);
    }

    protected int instMark() {
        return lastInstructionStart;
    }

    protected void setInstMark() {
        lastInstructionStart = this.codeBuffer.position();
    }

    protected void clearInstMark() {
        lastInstructionStart = InvalidInstructionMark;
    }

    protected void relocate(Relocation rspec) {

        if (rspec == null || rspec == Relocation.none) {
            return;
        }

        assert !pdCheckInstructionMark() || instMark() == InvalidInstructionMark || instMark() == codeBuffer.position() : "call relocate() between instructions";
        relocate(codeBuffer.position(), rspec);
    }

    protected void relocate(int position, Relocation relocation) {

        TTY.println("RELOCATION recorded at position " + position + " " + relocation);
        switch (relocation.type()) {

        }

    }

    protected abstract boolean pdCheckInstructionMark();

    protected int target(Label l) {

        int branchPc = pc();
        if (l.isBound()) {
            int loc = l.loc();
            return loc;
        } else {
            l.addPatchAt(branchPc);

            // Need to return a pc, doesn't matter what it is since it will be
            // replaced during resolution later.
            // Don't return null or badAddress, since branches shouldn't overflow.
            // Don't return base either because that could overflow displacements
            // for shorter branches. It will get checked when bound.
            return branchPc;
        }
    }

    public int doubleConstant(double d) {
        int offset = dataBuffer.emitDouble(d);
        recordDataReferenceInCode(lastInstructionStart, offset);
        return offset;
    }

    private void recordDataReferenceInCode(int codeOffset, int dataOffset) {
        if (compilation.targetMethod == null) {
            TTY.println("Record data reference in code: code-offset=%d, data-offset=%d", codeOffset, dataOffset);
        } else {
            compilation.targetMethod.recordDataReferenceInCode(codeOffset, dataOffset, true);
        }
    }

    public int floatConstant(float f) {
        int offset = dataBuffer.emitFloat(f);
        recordDataReferenceInCode(lastInstructionStart, offset);
        return offset;
    }

    public abstract void nop();

    public void blockComment(String st) {
        // TODO Auto-generated method stub

    }

    public abstract void nullCheck(Register r);

    public void verifiedEntry() {
        // TODO Auto-generated method stub

    }

    public abstract void buildFrame(int initialFrameSizeInBytes);

    public abstract void align(int codeEntryAlignment);

    public abstract void makeOffset(int offset);

    public void pdPatchInstruction(int branch, int target) {
        assert compilation.target.arch.isX86();

        int op = codeBuffer.getByte(branch);
        assert op == 0xE8 // call
                        ||
                        op == 0xE9 // jmp
                        || op == 0xEB // short jmp
                        || (op & 0xF0) == 0x70 // short jcc
                        || op == 0x0F && (codeBuffer.getByte(branch + 1) & 0xF0) == 0x80 // jcc
        : "Invalid opcode at patch point";

        if (op == 0xEB || (op & 0xF0) == 0x70) {

            // short offset operators (jmp and jcc)
            int imm8 = target - (branch + 2);
            assert this.is8bit(imm8) : "Short forward jump exceeds 8-bit offset";
            codeBuffer.emitByte(imm8, branch + 1);

        } else {

            int off = 1;
            if (op == 0x0F) {
                off = 2;
            }

            int imm32 = target - (branch + 4 + off);
            codeBuffer.emitInt(imm32, branch + off);
        }
    }

    public void installTargetMethod() {
        if (compilation.targetMethod == null) {
            byte[] array = codeBuffer.finished();
            int length = codeBuffer.position();
            Util.printBytes(array, length);

            TTY.println("Disassembled code:");
            TTY.println(compilation.runtime.disassemble(Arrays.copyOf(array, length)));

            array = dataBuffer.finished();
            length = dataBuffer.position();
            Util.printBytes(array, length);
            TTY.println("Frame size: %d", compilation.frameMap().framesize());

        } else {
            compilation.targetMethod.setTargetCode(codeBuffer.finished(), codeBuffer.position());
            compilation.targetMethod.setData(dataBuffer.finished(), dataBuffer.position());
            compilation.targetMethod.setFrameSize(compilation.frameMap().framesize());
            compilation.targetMethod.finish();
        }
    }

    public void decode() {
        byte[] currentBytes = codeBuffer.getData(lastDecodeStart, codeBuffer.position());
        Util.printBytes(currentBytes);
        if (currentBytes.length > 0) {
            TTY.println(compilation.runtime.disassemble(currentBytes));
        }
        lastDecodeStart = codeBuffer.position();
    }
}
