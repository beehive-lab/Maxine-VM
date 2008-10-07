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
/*VCSID=47891d5f-0000-4d0d-a4c3-ef0070176e2d*/
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.program.*;
import com.sun.max.vm.template.source.*;
import com.sun.max.vm.type.*;

/**
* Testing the JIT-compiler with methods operating on atomic type array.
*
* @author Laurent Daynes
*/
public class JITTest_compileMethodUsingArray extends JitCompilerTestCase {

    void compileMethod(Kind kind) {
        final String methodName = methodNameFor(kind);
        Trace.on(1);
        Trace.line(1, "\nCompile " + methodName);
        compileMethod(methodName, SignatureDescriptor.create("()V"));
    }

    String methodNameFor(Kind kind) {
        final String kindName = kind.name().toString();
        final String typeName = kindName.substring(0, 1).toUpperCase() +  kindName.substring(1).toLowerCase();
        return "use" + typeName + "Array";
    }

    void useByteArray() {
        final byte[] a = new byte[2];
        a[0] = 10;
        a[1] = (byte) (10 * a[1]);
    }

    void useBooleanArray() {
        final boolean[] a = new boolean[2];
        a[0] = true;
        a[1] = a[0] ^ true;
    }

    void useCharArray() {
        final char[] a = new char[2];
        a[0] = '0';
        a[1] = (char) (a[1] + 1);
    }

    void useShortArray() {
        final short[] a = new short[2];
        a[0] = 10;
        a[1] = (short) (10 * a[1]);
    }

    void useIntArray() {
        final int[] a = new int[2];
        a[0] = 10;
        a[1] = 10 * a[1];
    }

    void useFloatArray() {
        final float[] a = new float[2];
        a[0] = 10F;
        a[1] = 10 * a[1];
    }

    void useLongArray() {
        final long[] a = new long[2];
        a[0] = 10L;
        a[1] = 10 * a[1];
    }

    void useDoubleArray() {
        final double[] a = new double[2];
        a[0] = 10D;
        a[1] = 10 * a[1];
    }

    public void test_useByteArray() {
        compileMethod(Kind.BYTE);
    }

    public void test_useBooleanArray() {
        compileMethod(Kind.BOOLEAN);
    }

    public void test_useCharArray() {
        compileMethod(Kind.CHAR);
    }

    public void test_useShortArray() {
        compileMethod(Kind.SHORT);
    }

    public void test_useIntArray() {
        compileMethod(Kind.INT);
    }

    public void test_useFloatArray() {
        compileMethod(Kind.FLOAT);
    }

    public void test_useLongArray() {
        compileMethod(Kind.LONG);
    }

    public void test_useDoubleArray() {
        compileMethod(Kind.DOUBLE);
    }

    /**
     * Testing with unresolved, resolved, and initialized class constant.
     */
    @Override
    protected Class[] templateSources() {
        return new Class[]{UnoptimizedBytecodeTemplateSource.class, ResolvedFieldAccessTemplateSource.class, InitializedStaticFieldAccessTemplateSource.class, InstrumentedBytecodeSource.class};
    }

}
