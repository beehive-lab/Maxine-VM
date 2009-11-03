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
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.vm.template.source.*;
import com.sun.max.vm.type.*;

/**
 * Testing the JIT-compiler with methods doing arithmetic operation only (int, float, double, etc.).
 * This exercises the customization of template with immediate and literal pool constant to numeric value,
 *  immediate operand, and typed load/store from/to the evaluation stack and local variables.
 *
 * @author Laurent Daynes
 */
public class JITTest_compileArithmeticOp extends JitCompilerTestCase {

    public JITTest_compileArithmeticOp() {
    }

    /**
     * Makes uses of arithmetic bytecode instructions and instructions with constant operands
     * (cause the jit to modify code with immediate int constant dependency).
     */
    public void perform_int_op() {
        int i = 10;             // will exercise a bipush
        i = 2 * i + 1;          // will exercise iconst2 & iconst1
    }

    public void perform_idiv() {
        int i = 100;
        i = i / 9;
    }

    public void perform_iinc() {
        int i = 23;
        i--;
        i++;
        i -= 3;
        i += 5;
    }

    /**
     * Makes uses of arithmetic bytecode instructions and instructions with constant pool literal
     * (cause the jit to modify code with literal float constant dependency).
     */
    public void perform_floating_point_op() {
        float f = 3335.20F;   // will exercise ldc and fstore1
        f = 0.275F * f + 1f;    // will exercise ldc and fconst1, fload1
    }


    /**
     * Makes uses of arithmetic bytecode instructions and instructions with constant pool literal
     * (cause the jit to modify code with literal constant dependency).
     */
    public void perform_double_op() {
        double d = 33300003435.20D;  // will exercise ldc2_w and dstore1
        d = 0.275F * d + 1D;                   // will exercise ldc2_w, dconst1, dload1, etc.
    }

    public void test_int_op() {
        compileMethod("perform_int_op", SignatureDescriptor.VOID);
    }

    public void test_idiv() {
        compileMethod("perform_idiv", SignatureDescriptor.VOID);
    }

    public void test_iinc() {
        compileMethod("perform_iinc", SignatureDescriptor.VOID);
    }

    public void test_floating_point_op() {
        compileMethod("perform_floating_point_op", SignatureDescriptor.VOID);
    }

    public void test_double_op() {
        compileMethod("perform_double_op", SignatureDescriptor.VOID);
    }


    @Override
    protected Class[] templateSources() {
        return TemplateTableConfiguration.OPTIMIZED_TEMPLATE_SOURCES;
    }

    public JITTest_compileArithmeticOp(String name) {
        super(name);
    }
}
