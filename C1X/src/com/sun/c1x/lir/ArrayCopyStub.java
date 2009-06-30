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


/**
 * The <code>ArrayCopyStub</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class ArrayCopyStub {

    private LIRArrayCopy arrayCopy;

    /**
     * Creates a new ArrayCopyStub.
     *
     * @param op
     */
    public ArrayCopyStub(LIRArrayCopy arrayCopy) {
        super();
        this.arrayCopy = arrayCopy;
    }

    public LIROperand src() {
        return arrayCopy.src();
    }

    public LIROperand srcPos() {
        return arrayCopy.srcPos();
    }

    public LIROperand dst() {
        return arrayCopy.dst();
    }

    public LIROperand dstPos() {
        return arrayCopy.dstPos();
    }

    public LIROperand length() {
        return arrayCopy.length();
    }


    public LIROperand tmp() {
        return arrayCopy.tmp();
    }



//    private:
//        LIR_OpArrayCopy* _op;
//
//       public:
//        ArrayCopyStub(LIR_OpArrayCopy* op): _op(op) { }
//
//        LIR_Opr src() const                         { return _op->src(); }
//        LIR_Opr src_pos() const                     { return _op->src_pos(); }
//        LIR_Opr dst() const                         { return _op->dst(); }
//        LIR_Opr dst_pos() const                     { return _op->dst_pos(); }
//        LIR_Opr length() const                      { return _op->length(); }
//        LIR_Opr tmp() const                         { return _op->tmp(); }
//
//        virtual void emit_code(LIR_Assembler* e);
//        virtual CodeEmitInfo* info() const          { return _op->info(); }
//        virtual void visit(LIR_OpVisitState* visitor) {
//          // don't pass in the code emit info since it's processed in the fast path
//          visitor->do_slow_case();
//        }
//      #ifndef PRODUCT
//        virtual void print_name(outputStream* out) const { out->print("ArrayCopyStub"); }
//      #endif // PRODUCT

}
