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
package test.com.sun.max.vm.compiler.cps.bytecode;

import java.lang.reflect.*;

import test.com.sun.max.vm.compiler.cps.*;

import com.sun.max.vm.classfile.create.*;
import com.sun.max.vm.compiler.cps.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public abstract class BytecodeTest_cmp<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_cmp(String name) {
        super(name);
    }

    private void lcmp(int greater, int less, int expectedResult) {
        final String className = getClass().getName() + "_test_lcmp" + greater;

        final SignatureDescriptor signature = SignatureDescriptor.create(int.class, long.class, long.class);
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(4, 4);
        code.lload(0);
        code.lload(2);
        code.lcmp();
        code.ireturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_lcmp", signature, code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_lcmp", signature);
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void lcmp() {
                confirmPresence();
            }
        };
        final Value result = execute(method, LongValue.from(greater), LongValue.from(less));
        assertEquals(expectedResult, result.asInt());
    }

    public void test_lcmp() {
        lcmp(1, 1, 0);
        lcmp(2, 1, 1);
        lcmp(3, 5, -1);
    }

    private void fcmpl(int greater, int less, int expectedResult) {
        final String className = getClass().getName() + "_test_fcmpl" + greater;

        final SignatureDescriptor signature = SignatureDescriptor.create(int.class, float.class, float.class);
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(2, 2);
        code.fload(0);
        code.fload(1);
        code.fcmpl();
        code.ireturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_fcmpl", signature, code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_fcmpl", signature);
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void fcmpl() {
                confirmPresence();
            }
        };
        final Value result = execute(method, FloatValue.from(greater), FloatValue.from(less));
        assertEquals(expectedResult, result.asInt());
    }

    public void test_fcmpl() {
        fcmpl(1, 1, 0);
        fcmpl(2, 1, 1);
        fcmpl(3, 5, -1);
    }

    private void fcmpg(int greater, int less, int expectedResult) {
        final String className = getClass().getName() + "_test_fcmpg" + greater;

        final SignatureDescriptor signature = SignatureDescriptor.create(int.class, float.class, float.class);
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(2, 2);
        code.fload(0);
        code.fload(1);
        code.fcmpg();
        code.ireturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_fcmpg", signature, code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_fcmpg", signature);
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void fcmpg() {
                confirmPresence();
            }
        };
        final Value result = execute(method, FloatValue.from(greater), FloatValue.from(less));
        assertEquals(expectedResult, result.asInt());
    }

    public void test_fcmpg() {
        fcmpg(1, 1, 0);
        fcmpg(2, 1, 1);
        fcmpg(3, 5, -1);
    }

    private void dcmpl(int greater, int less, int expectedResult) {
        final String className = getClass().getName() + "_test_dcmpl" + greater;

        final SignatureDescriptor signature = SignatureDescriptor.create(int.class, double.class, double.class);
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(4, 4);
        code.dload(0);
        code.dload(2);
        code.dcmpl();
        code.ireturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_dcmpl", signature, code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_dcmpl", signature);
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void dcmpl() {
                confirmPresence();
            }
        };
        final Value result = execute(method, DoubleValue.from(greater), DoubleValue.from(less));
        assertEquals(expectedResult, result.asInt());
    }

    public void test_dcmpl() {
        dcmpl(1, 1, 0);
        dcmpl(2, 1, 1);
        dcmpl(3, 5, -1);
    }

    private void dcmpg(int greater, int less, int expectedResult) {
        final String className = getClass().getName() + "_test_dcmpg" + greater;

        final SignatureDescriptor signature = SignatureDescriptor.create(int.class, double.class, double.class);
        final MillClass millClass = new MillClass(Modifier.PUBLIC, className, Object.class.getName());
        final MillCode code = new MillCode(4, 4);
        code.dload(0);
        code.dload(2);
        code.dcmpg();
        code.ireturn();
        millClass.addMethod(Modifier.PUBLIC + Modifier.STATIC, "perform_dcmpg", signature, code);
        final byte[] classfileBytes = millClass.generate();

        final Method_Type method = compileMethod(className, classfileBytes, "perform_dcmpg", signature);
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void dcmpg() {
                confirmPresence();
            }
        };
        final Value result = execute(method, DoubleValue.from(greater), DoubleValue.from(less));
        assertEquals(expectedResult, result.asInt());
    }

    public void test_dcmpg() {
        dcmpg(1, 1, 0);
        dcmpg(2, 1, 1);
        dcmpg(3, 5, -1);
    }

}
