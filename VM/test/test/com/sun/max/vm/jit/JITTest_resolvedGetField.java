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
/*VCSID=e0b3827d-e6e6-4264-9637-28acc711e202*/
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;

/**
 * A simple test to print the output produced by the compiler for the
 * sequence aload_0, getfield, ireturn, so as to compare with what
 * a template-based JIT could do.
 *
 * @author Laurent Daynes
 */
public class JITTest_resolvedGetField extends CompilerTestCase<TargetMethod> {

    private int _intField;

    static class ResolvedAtCompileTime {
        int _intField;
    }

    public int perform_not_equal(int a, int b) {
        return (a != b) ? 1 : 0;
    }

    // Used to debug conditional branches
    public void test_not_equal() {
        Trace.on(3);
        final TargetMethod method = compileMethod("perform_not_equal", SignatureDescriptor.create(int.class, int.class, int.class));
        disassemble(method);
    }

    public Object perform_use_null_constant(Object o, int a) {
        return (a != 0) ? o : null;
    }

    public void test_use_null_constant() {
        Trace.on(3);
        final TargetMethod method = compileMethod("perform_use_null_constant", SignatureDescriptor.create(Object.class, Object.class, int.class));
        disassemble(method);
    }

    public int perform_visitor(ResolvedAtCompileTime resolvedAtCompileTime) {
        return resolvedAtCompileTime._intField;
    }

    public void test_visitor() throws ClassNotFoundException {
        Trace.on(3);
        // Make sure the class whose field is being accessed is loaded in the target first (we want a resolved symbol at compiled time).
        PrototypeClassLoader.PROTOTYPE_CLASS_LOADER.loadClass(ResolvedAtCompileTime.class.getName());
        // Now compile the method we're interested in
        final TargetMethod method = compileMethod("perform_visitor", SignatureDescriptor.create(int.class, ResolvedAtCompileTime.class));
        disassemble(method);
    }

    public void perform_increment() {
        int i = _intField;
        i += 5;
        _intField = i;
    }

    public void test_increment() {
        //Trace.on(3);
        final TargetMethod method = compileMethod("perform_increment", SignatureDescriptor.create(void.class));
        Trace.on(1);
        Trace.line(1, method.classMethodActor().name());

        traceBundleAndDisassemble(method);
    }

    public int foo(int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
        return (i1 + i2 * i3 + i4 - i5 * i6) << i7;
    }

    public void perform_invocation(int a) {
        int b = a * 53;
        b = foo(b, a, 67, 86, 77, 88, 99);
    }

    public void test_invocation() {
        //Trace.on(3);
        final TargetMethod method = compileMethod("perform_invocation", SignatureDescriptor.create(void.class, int.class));
        final TargetMethod method2 = compileMethod("foo", SignatureDescriptor.create(int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class));
        Trace.on(1);
        Trace.line(1, method.classMethodActor().name().toString());
        disassemble(method);
        Trace.line(1, "\n\n" + method2.classMethodActor().name());
        disassemble(method2);
    }

}


