/***
 * ASM tests
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.objectweb.asm;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * ClassWriter unit tests for the COMPUTE_FRAMES option.
 * 
 * @author Eric Bruneton
 */
public class ClassWriterComputeFramesUnitTest extends TestCase {

    private byte[] getClassBytes() throws IOException {
        String className = ClassWriterComputeFramesUnitTest.class.getName();
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        new ClassReader(className).accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public void visit(final int version, final int access,
                    final String name, final String signature,
                    final String superName, final String[] interfaces) {
                // Set V1_7 version to prevent fallback to old verifier.
                super.visit((version & 0xFFFF) < Opcodes.V1_7 ? Opcodes.V1_7
                        : version, access, name, signature, superName,
                        interfaces);
            }
        }, ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    public void testSameDimension0() throws Exception {
        new TestClassLoader().test(getClassBytes(), "sameDimension0");
    }

    public static Number sameDimension0(boolean b) {
        return b ? new Integer(1) : new Float(1);
    }

    public void testSameDimension1() throws Exception {
        new TestClassLoader().test(getClassBytes(), "sameDimension1");
    }

    public static Number[] sameDimension1(boolean b) {
        return b ? new Integer[1] : new Float[1];
    }

    public void testSameDimension2() throws Exception {
        new TestClassLoader().test(getClassBytes(), "sameDimension2");
    }

    public static Number[][] sameDimension2(boolean b) {
        return b ? new Integer[1][1] : new Float[1][1];
    }

    public void testSameDimension3() throws Exception {
        new TestClassLoader().test(getClassBytes(), "sameDimension3");
    }

    public static Object[] sameDimension3(boolean b) {
        return b ? (Object[]) new byte[1][1] : (Object[]) new short[1][1];
    }

    public void testSameDimension4() throws Exception {
        new TestClassLoader().test(getClassBytes(), "sameDimension4");
    }

    public static Object sameDimension4(boolean b) {
        return b ? new byte[1] : new Float[1];
    }

    public void testSameDimension5() throws Exception {
        new TestClassLoader().test(getClassBytes(), "sameDimension5");
    }

    public static Object[] sameDimension5(boolean b) {
        return b ? (Object[]) new byte[1][1] : (Object[]) new Float[1][1];
    }

    public void testDifferentDimension1() throws Exception {
        new TestClassLoader().test(getClassBytes(), "differentDimension1");
    }

    public static Object differentDimension1(boolean b) {
        return b ? new byte[1] : new byte[1][1];
    }

    public void testDifferentDimension2() throws Exception {
        new TestClassLoader().test(getClassBytes(), "differentDimension2");
    }

    public static Object[] differentDimension2(boolean b) {
        return b ? new Object[1] : new byte[1][1];
    }

    public void testDifferentDimension3() throws Exception {
        new TestClassLoader().test(getClassBytes(), "differentDimension3");
    }

    public static Object[] differentDimension3(boolean b) {
        return b ? (Object[]) new byte[1][1] : (Object[]) new byte[1][1][1];
    }

    public void testDifferentDimension4() throws Exception {
        new TestClassLoader().test(getClassBytes(), "differentDimension4");
    }

    public static Object[][] differentDimension4(boolean b) {
        return b ? (Object[][]) new byte[1][1][1]
                : (Object[][]) new byte[1][1][1][1];
    }

    public void testDifferentDimension5() throws Exception {
        new TestClassLoader().test(getClassBytes(), "differentDimension5");
    }

    public static Object differentDimension5(boolean b) {
        return b ? new Integer(1) : new byte[1];
    }

    public void testDifferentDimension6() throws Exception {
        new TestClassLoader().test(getClassBytes(), "differentDimension6");
    }

    public static Object differentDimension6(boolean b) {
        return b ? new Integer(1) : new Float[1];
    }

    // ------------------------------------------------------------------------

    static class TestClassLoader extends ClassLoader {

        public void test(final byte[] b, String methodName) throws Exception {
            String className = ClassWriterComputeFramesUnitTest.class.getName();
            defineClass(className, b, 0, b.length).getDeclaredMethod(
                    methodName, boolean.class);
        }
    }
}
