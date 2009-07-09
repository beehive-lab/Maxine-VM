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
package com.sun.c1x.lir;

import com.sun.c1x.util.*;


/**
 * The <code>ArrayStoreExceptionStub</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class ArrayStoreExceptionStub extends CodeStub {

    private CodeEmitInfo info;

    /**
     * @param info
     */
    public ArrayStoreExceptionStub(CodeEmitInfo info) {
        super();
        this.info = info;
    }

    /**
     * Gets the info of this class.
     *
     * @return the info
     */
    @Override
    public CodeEmitInfo info() {
        return info;
    }

    /* (non-Javadoc)
     * @see com.sun.c1x.lir.CodeStub#visit(com.sun.c1x.lir.LIRVisitState)
     */
    @Override
    public void visit(LIRVisitState visitor) {
        visitor.doSlowCase();
    }

    /**
     * Emit the code stub for an array store exception.
     *
     * @param masm the target assembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.sun.c1x.lir.CodeStub#printName(com.sun.c1x.util.LogStream)
     */
    @Override
    public void printName(LogStream out) {
        out.print("ArrayStoreExceptionStub");
    }




//    private:
//        CodeEmitInfo* _info;
//
//       public:
//        ArrayStoreExceptionStub(CodeEmitInfo* info);
//        virtual void emit_code(LIR_Assembler* emit);
//        virtual CodeEmitInfo* info() const             { return _info; }
//        virtual bool is_exception_throw_stub() const   { return true; }
//        virtual void visit(LIR_OpVisitState* visitor) {
//          visitor->do_slow_case(_info);
//        }
//      #ifndef PRODUCT
//        virtual void print_name(outputStream* out) const { out->print("ArrayStoreExceptionStub"); }
//      #endif // PRODUCT
}
