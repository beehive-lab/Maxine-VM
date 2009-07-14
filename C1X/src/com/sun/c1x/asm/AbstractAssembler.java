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

import com.sun.c1x.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.target.x86.*;
import com.sun.c1x.util.*;

/**
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public abstract class AbstractAssembler {

    protected CodeSection codeSection; // section within the code buffer
    protected Pointer codeBegin; // first byte of code buffer
    protected Pointer codeLimit; // first byte after code buffer
    protected Pointer codePos; // current code generation position
    protected OopRecorder oopRecorder; // support for relocInfo.oopType
    public final C1XCompilation compilation;

    protected Address addrAt(int pos) {
        return new Address(codeBegin.value + pos);
    }

    protected boolean is8bit(int x) {
        return -0x80 <= x && x < 0x80;
    }

    protected boolean isByte(int x) {
        return 0 <= x && x < 0x100;
    }

    protected boolean isShiftCount(int x) {
        return 0 <= x && x < 32;
    }

    // Accessors
    public CodeSection codeSection() {
        return codeSection;
    }

    public Pointer pc() {
        return codePos;
    }

    public int offset() {
        return Util.safeToInt(codePos.value - codeBegin.value);
    }

    public OopRecorder oopRecorder() {
        return oopRecorder;
    }

    public void setOopRecorder(OopRecorder r) {
        oopRecorder = r;
    }

    public AbstractAssembler(C1XCompilation compilation, CodeBuffer code) {
        this.compilation = compilation;
        if (code == null) {
            return;
        }
        CodeSection cs = code.insts();
        cs.clearMark(); // new assembler kills old mark
        codeSection = cs;
        codeBegin = cs.start();
        codeLimit = cs.limit();
        codePos = cs.end();
        oopRecorder = code.oopRecorder();
        if (codeBegin == null) {
            compilation.runtime.vmExitOutOfMemory1(0, "CodeCache: no room for %s", code.name());
        }
    }

    void setCodeSection(CodeSection cs) {
        assert cs.outer() == codeSection().outer() : "sanity";
        assert cs.isAllocated() : "need to pre-allocate this section";
        cs.clearMark(); // new assembly into this section kills old mark
        codeSection = cs;
        codeBegin = cs.start();
        codeLimit = cs.limit();
        codePos = cs.end();
    }

    // Inform CodeBuffer that incoming code and relocation will be for stubs
    public Pointer startAStub(int requiredSpace) {
        CodeBuffer cb = code();
        CodeSection cs = cb.stubs();
        assert codeSection == cb.insts() : "not in insts?";
        sync();
        if (cs.maybeExpandToEnsureRemaining(requiredSpace) && cb.blob() == null) {
            return null;
        }
        setCodeSection(cs);
        return pc();
    }

    // Inform CodeBuffer that incoming code and relocation will be code
    // Should not be called if startAStub() returned null
    public void endAStub() {
        assert codeSection == code().stubs() : "not in stubs?";
        sync();
        setCodeSection(code().insts());
    }

    // Inform CodeBuffer that incoming code and relocation will be for stubs
    Address startAConst(int requiredSpace, int requiredAlign) {
        // TODO: Figure out how to do this in Java!
// CodeBuffer cb = code();
// CodeSection cs = cb.consts();
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
// setCodeSection(cs);
// return end;
        throw Util.unimplemented();
    }

    // Inform CodeBuffer that incoming code and relocation will be code
    // Should not be called if startAConst() returned null
    void endAConst() {
        assert codeSection == code().consts() : "not in consts?";
        sync();
        setCodeSection(code().insts());
    }

    void flush() {
        sync();
        ICache.invalidateRange(addrAt(0), offset());
    }

    protected void aByte(int x) {
        emitByte(x);
    }

    void aLong(int x) {
        emitLong(x);
    }

    void print(Label l) {
        if (l.isBound()) {
            TTY.println(String.format("bound label to %d|%d", l.locPos(), l.locSect()));
        } else if (l.isUnbound()) {
            l.printInstructions(this);
        } else {
            TTY.println(String.format("label in inconsistent state (loc = %d)", l.loc()));
        }
    }

    public void bind(Label l) {
        if (l.isBound()) {
            // Assembler can bind a label more than once to the same place.
            Util.guarantee(l.loc() == locator(), "attempt to redefine label");
            return;
        }
        l.bindLoc(locator());
        l.patchInstructions(this);
    }

    void generateStackOverflowCheck(int frameSizeInBytes) {
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

    void blockComment(char comment) {
        if (sect() == CodeBuffer.Type.SECT_INSTS.value) {
            codeSection().outer().blockComment(offset(), comment);
        }
    }

    protected abstract int codeFillByte();

    void sync() {
        CodeSection cs = codeSection();
        // TODO: In C1 this is a guarantee!
        assert cs.start() == codeBegin : "must not shift code buffer";
        cs.setEnd(codePos);
    }

    protected void emitByte(long x) {
        assert x == (int) x;
        emitByte((int) x);
    }

    protected void emitByte(int x) {
        assert isByte(x) : "not a byte";

        // TODO: Figure out how to do this in Java!
// *(unsigned char)codePos = (unsigned char)x;
// codePos += sizeof(unsigned char);
        // sync();
        throw Util.unimplemented();
    }

    protected void emitWord(int x) {
        // TODO: Figure out how to do this in Java!
// *(short)codePos = (short)x;
// codePos += sizeof(short);
// sync();
        throw Util.unimplemented();
    }

    protected void emitLong(long x) {
        assert x == (int) x;
        emitLong((int) x);
    }

    protected void emitLong(int x) {
        // TODO: Figure out how to do this in Java!
// *(jint)codePos = x;
// codePos += sizeof(jint);
// sync();
        throw Util.unimplemented();
    }

    protected void emitLong64(long x) {
        // TODO: Figure out how to do this in Java!
//        *(jlong) codePos = x;
//        codePos += sizeof(jlong);
//        codeSection().setEnd(codePos);
        throw Util.unimplemented();
      }

    void emitAddress(Address x) {
        // TODO: Figure out how to do this in Java!
// *(Address)codePos = x;
// codePos += sizeof(Address);
// sync();
        throw Util.unimplemented();
    }

    protected Pointer instMark() {
        return codeSection().mark();
    }

    protected void setInstMark() {
        codeSection().setMark();
    }

    protected void clearInstMark() {
        codeSection().clearMark();
    }

    public void relocate(RelocationHolder rspec) {
        relocate(rspec, 0);
    }

    void relocate(RelocationHolder rspec, int format) {
        assert !pdCheckInstructionMark() || instMark() == null || instMark() == codePos : "call relocate() between instructions";
        codeSection().relocate(codePos, rspec, format);
    }

    protected void relocate(RelocInfo.Type rtype) {
        relocate(rtype, 0);
    }

    void relocate(RelocInfo.Type rtype, int format) {
        if (rtype != RelocInfo.Type.none) {
            // TODO: Implement
            Util.unimplemented();
            // relocate(Relocation::spec_simple(rtype), format);
        }
    }

    protected abstract boolean pdCheckInstructionMark();

    public CodeBuffer code() {
        return codeSection().outer();
    }

    int sect() {
        return codeSection().index();
    }

    protected int locator() {
        return CodeBuffer.locator(offset(), sect());
    }

    protected Pointer target(Label l) {
        return codeSection().target(l, pc());
    }

    // fp constants support
    public Pointer doubleConstant(double c) {

        // TODO: Figure out how to do this in Java!
// Address ptr = startAConst(sizeof(c), sizeof(c));
// if (ptr != null) {
// *(jdouble)ptr = c;
// codePos = ptr + sizeof(c);
// endAConst();
// }
// return ptr;

        throw Util.unimplemented();
    }

    public Pointer floatConstant(float f) {
        // TODO: Figure out how to do this in Java!
// Address ptr = startAConst(sizeof(c), sizeof(c));
// if (ptr != null) {
// *(jfloat)ptr = c;
// codePos = ptr + sizeof(c);
// endAConst();
// }
// return ptr;
        throw Util.unimplemented();
    }

    Address addressConstant(Address c, RelocationHolder rspec) {
        // TODO: Figure out how to do this in Java!
// Address ptr = startAConst(sizeof(c), sizeof(c));
// if (ptr != null) {
// relocate(rspec);
// *(Address)ptr = c;
// codePos = ptr + sizeof(c);
// endAConst();
// }
// return ptr;
        throw Util.unimplemented();
    }

    public abstract void nop();

    public void blockComment(String st) {
        // TODO Auto-generated method stub

    }

    public abstract void nullCheck(Register r);


    public void verifiedEntry() {
        // TODO Auto-generated method stub

    }

    public void buildFrame(int initialFrameSizeInBytes) {
        // TODO Auto-generated method stub

    }

    public abstract void align(int codeEntryAlignment);
}
