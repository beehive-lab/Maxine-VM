/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.cps.*;

import com.sun.max.vm.type.*;

/**
* Testing the JIT-compiler with methods operating on atomic type array.
*
* @author Laurent Daynes
*/
public class JITTest_compileMethodUsingArray extends JitCompilerTestCase {

    void compileMethod(Kind kind) {
        final String methodName = methodNameFor(kind);
        compileMethod(methodName, SignatureDescriptor.VOID);
    }

    String methodNameFor(Kind kind) {
        final String kindName = kind.name.toString();
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
}
