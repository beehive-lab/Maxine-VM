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
package org.objectweb.asm.signature;

import junit.framework.TestSuite;

import org.objectweb.asm.AbstractTest;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Signature tests.
 * 
 * @author Eric Bruneton
 */
public class SignatureWriterTest extends AbstractTest {

    public static TestSuite suite() throws Exception {
        return new SignatureWriterTest().getSuite();
    }

    @Override
    public void test() throws Exception {
        ClassReader cr = new ClassReader(is);
        cr.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public void visit(int version, int access, String name,
                    String signature, String superName, String[] interfaces) {
                if (signature != null) {
                    SignatureReader sr = new SignatureReader(signature);
                    SignatureWriter sw = new SignatureWriter();
                    sr.accept(sw);
                    assertEquals(signature, sw.toString());
                }
            }

            @Override
            public FieldVisitor visitField(int access, String name,
                    String desc, String signature, Object value) {
                if (signature != null) {
                    SignatureReader sr = new SignatureReader(signature);
                    SignatureWriter sw = new SignatureWriter();
                    sr.acceptType(sw);
                    assertEquals(signature, sw.toString());
                }
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name,
                    String desc, String signature, String[] exceptions) {
                if (signature != null) {
                    SignatureReader sr = new SignatureReader(signature);
                    SignatureWriter sw = new SignatureWriter();
                    sr.accept(sw);
                    assertEquals(signature, sw.toString());
                }
                return null;
            }

        }, 0);
    }
}
