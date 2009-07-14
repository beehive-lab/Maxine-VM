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
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

public abstract class AbstractAssembler {

    protected CodeSection codeSection; // section within the code buffer
    protected Address codeBegin; // first byte of code buffer
    protected Address codeLimit; // first byte after code buffer
    protected Address codePos; // current code generation position
    protected OopRecorder oopRecorder; // support for relocInfo.oopType
    public final C1XCompilation compilation;

    protected Address addrAt(int pos) {
        return new Address(codeBegin.address() + pos);
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

    public Address pc() {
        return codePos;
    }

    public int offset() {
        return codePos.sub(codeBegin);
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

    // fp constants support
    Address doubleConstant(double c) {

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

    Address floatConstant(float f) {
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

    public CodeBuffer code() {
        // TODO Auto-generated method stub
        return null;
    }

    public void bind(Label label) {
        // TODO Auto-generated method stub

    }

    public abstract void nullCheck(Register r);

    public void align(int codeEntryAlignment) {
        // TODO Auto-generated method stub

    }

    public void verifiedEntry() {
        // TODO Auto-generated method stub

    }

    public void buildFrame(int initialFrameSizeInBytes) {
        // TODO Auto-generated method stub

    }
}
