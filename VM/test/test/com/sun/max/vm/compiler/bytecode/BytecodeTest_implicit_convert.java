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
package test.com.sun.max.vm.compiler.bytecode;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeTest_implicit_convert<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    protected BytecodeTest_implicit_convert(String name) {
        super(name);
    }

    static int perform_byteToInt(byte b) {
        return b;
    }

    public void test_byteToInt() {
        assertTrue(perform_byteToInt((byte) -1) == -1);
        final Method_Type method = compileMethod("perform_byteToInt", SignatureDescriptor.create(int.class, byte.class));
        Value result = execute(method, ByteValue.from((byte) -1));
        assertTrue(result.asInt() == -1);
        result = execute(method, ByteValue.from((byte) -1));
        assertTrue(result.asInt() == -1);
    }

}
