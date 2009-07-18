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
package com.sun.c1x.stub;

import com.sun.c1x.asm.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.target.*;
import com.sun.c1x.util.*;


/**
 * The <code>PatchingStub</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class PatchingStub extends CodeStub {

    public static final int PATCHINFOSIZE = 3;

    public enum PatchID {
        AccessFieldId, LoadKlassId
    }

    private PatchID id;
    private Pointer pcStart;
    private int bytesToCopy;
    private Label patchedCodeEntry;
    private Label patchSiteEntry;
    private Label patchSiteContinuation;
    private Register obj;
    private CodeEmitInfo info;
    private int oopIndex; // index of the patchable oop in nmethod oop table if needed
    private static int patchInfoOffset;

    public static int patchInfoOffset() {
        return patchInfoOffset;
    }

    /**
     * Constructs a new <code>PatchingStub</code>.
     *
     * @param masm
     * @param id
     */
    public PatchingStub(AbstractAssembler masm, PatchID id) {
        this(masm, id, -1);
    }

    /**
     * Constructs a new <code>PatchingStub</code>.
     *
     * @param masm
     * @param id
     * @param oopIndex
     */
    public PatchingStub(AbstractAssembler masm, PatchID id, int oopIndex) {
        this.id = id;
        this.oopIndex = oopIndex;
        info = null;
        if (masm.compilation.runtime.isMP()) {
            // force alignment of patch sites on MP hardware so we
            // can guarantee atomic writes to the patch site.
            alignPatchSite(masm);
        }
        pcStart = masm.pc();
        masm.bind(patchSiteEntry);
    }

    public void install(AbstractAssembler masm, LIRPatchCode patchCode, Register obj, CodeEmitInfo info) {
        this.info = info;
        this.obj = obj;
        masm.bind(patchSiteContinuation);
        bytesToCopy = (int) (masm.pc().address() - pcStart.address());
        if (id == PatchID.AccessFieldId) {
            // embed a fixed offset to handle long patches which need to be offset by a word.
            // the patching code will just add the field offset field to this offset so
            // that we can reference either the high or low word of a double word field.
            int fieldOffset = 0;
            switch (patchCode) {
                case PatchLow:
                    fieldOffset = masm.compilation.target.arch.loWordOffsetInBytes;
                    break;
                case PatchHigh:
                    fieldOffset = masm.compilation.target.arch.hiWordOffsetInBytes;
                    break;
                case PatchNormal:
                    fieldOffset = 0;
                    break;
                default:
                    Util.shouldNotReachHere();
            }
            NativeMovRegMem nMove = NativeMovRegMem.nativeMoveRegMemAt(pcStart());
            nMove.setOffset(fieldOffset);
        } else if (id == PatchID.LoadKlassId) {
            assert !obj.isNoReg() : "must have register object for loadKlass";
            // verify that we're pointing at a NativeMovConstReg
            assert NativeMovConstReg.isNativeMovConstRegAt(pcStart());
        } else {
            Util.shouldNotReachHere();
        }
        assert bytesToCopy <= (masm.pc().address() - pcStart().address()) : "not enough bytes";
    }

    /**
     * Gets the start pc of this code stub.
     *
     * @return the pcStart
     */
    public Pointer pcStart() {
        return pcStart;
    }

    /**
     * Gets the id of this code stub.
     *
     * @return the id
     */
    public PatchID id() {
        return id;
    }

    /**
     * Gets the patchedCodeEntry of this class.
     *
     * @return the patchedCodeEntry
     */
    public Label patchedCodeEntry() {
        return patchedCodeEntry;
    }

    /**
     * Gets the obj of this class.
     *
     * @return the obj
     */
    public Register obj() {
        return obj;
    }

    /**
     * Gets the oopIndex of this class.
     *
     * @return the oopIndex
     */
    public int oopIndex() {
        return oopIndex;
    }

    @Override
    public void visit(LIRVisitState visitor) {
        visitor.doSlowCase(info);
    }

    private void alignPatchSite(AbstractAssembler masm) {
        // TODO: Platform dependent. Only needed for x86
    }

    @Override
    public void emitCode(LIRAssembler e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void printName(LogStream out) {
        out.print("PatchingStub");
    }

}
