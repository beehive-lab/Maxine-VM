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
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;


/**
 * The <code>PatchingStub</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class PatchingStub extends CodeStub {
    public enum PatchID {
          AccessFieldId,
          LoadKlassId
        }

    @Override
    public void emitCode(LIRAssembler e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void printName(LogStream out) {
        // TODO Auto-generated method stub

    }


    public PatchID id() {
        // TODO Auto-generated method stub
        return null;
    }

    public void install(MacroAssembler masm, LIRPatchCode patchCode, Register obj, CodeEmitInfo info) {
        // TODO Auto-generated method stub

    }

    public Address pcStart() {
        // TODO Auto-generated method stub
        return null;
    }

//     public static final int PATCHINFOSIZE = 3;
//
//
//      private PatchID       id;
//      private address       pcStart;
//      private int           bytesToCopy;
//      private Label         patchedCodeEntry;
//      private Label         patchSiteEntry;
//      private Label         patchSiteContinuation;
//      private Register      obj;
//      private CodeEmitInfo info;
//      private int           oopIndex;  // index of the patchable oop in nmethod oop table if needed
//      private static int    patchInfoOffset;
//
//      private void alignPatchSite(MacroAssembler masm);
//
//       public static int patchInfoOffset() { return patchInfoOffset; }
//
//       public PatchingStub(MacroAssembler masm, PatchID id, int oopIndex = -1):
//            id(id)
//          , info(null)
//          , oopIndex(oopIndex) {
//          if (os.isMP()) {
//            // force alignment of patch sites on MP hardware so we
//            // can guarantee atomic writes to the patch site.
//            alignPatchSite(masm);
//          }
//          pcStart = masm.pc();
//          masm.bind(patchSiteEntry);
//        }
//
//        void install(MacroAssembler masm, LIRPatchCode patchCode, Register obj, CodeEmitInfo info) {
//          info = info;
//          obj = obj;
//          masm.bind(patchSiteContinuation);
//          bytesToCopy = masm.pc() - pcStart();
//          if (id == PatchingStub.accessFieldId) {
//            // embed a fixed offset to handle long patches which need to be offset by a word.
//            // the patching code will just add the field offset field to this offset so
//            // that we can refernce either the high or low word of a double word field.
//            int fieldOffset = 0;
//            switch (patchCode) {
//            case lirPatchLow:         fieldOffset = loWordOffsetInBytes; break;
//            case lirPatchHigh:        fieldOffset = hiWordOffsetInBytes; break;
//            case lirPatchNormal:      fieldOffset = 0;                       break;
//            default: Util.shouldNotReachHere();
//            }
//            NativeMovRegMem nMove = nativeMovRegMemAt(pcStart());
//            nMove.setOffset(fieldOffset);
//          } else if (id == loadKlassId) {
//            assert obj != noreg :  "must have register object for loadKlass";
//      #ifdef ASSERT
//            // verify that we're pointing at a NativeMovConstReg
//            nativeMovConstRegAt(pcStart());
//      #endif
//          } else {
//            Util.shouldNotReachHere();
//          }
//          assert bytesToCopy <= (masm.pc() - pcStart()) :  "not enough bytes";
//        }
//
//        address pcStart()                        { return pcStart; }
//        PatchID id()                              { return id; }
//
//        virtual void emitCode(LIRAssembler e);
//        virtual CodeEmitInfo info()              { return info; }
//        virtual void visit(LIROpVisitState visitor) {
//          visitor.doSlowCase(info);
//        }
//      #ifndef PRODUCT
//        virtual void printName(outputStream out)  { out.print("PatchingStub"); }
//      #endif // PRODUCT


}
