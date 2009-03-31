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
package test.com.sun.max.vm.compiler;

import com.sun.max.lang.*;
import com.sun.max.util.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;

/**
 * Tests for individual methods that exposed limits in the compiler's use of resources, mostly causing OutOfMemoryErrors.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class CompilerTest_large<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    public CompilerTest_large(String name) {
        super(name);
        // Set the system property that overrides the default behaviour of ClassfileReader when it encounters
        // a <clinit> while MaxineVM.isPrototying() returns true. The default behaviour is to discard such methods.
        System.setProperty("max.loader.preserveClinitMethods", "");
    }

    public void test_ReducedHexByte_clinit() {
        Classes.load(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER, ReducedHexByte.class.getName());
        compileMethod(ReducedHexByte.class, SymbolTable.CLINIT.toString(), SignatureDescriptor.create(void.class));
    }

    public void test_HexByte_clinit() {
        compileMethod(HexByte.class, SymbolTable.CLINIT.toString(), SignatureDescriptor.create(void.class));
    }

    public void test_Bytecode_clinit() {
        compileMethod(Bytecode.class, SymbolTable.CLINIT.toString(), SignatureDescriptor.create(void.class));
    }

}
