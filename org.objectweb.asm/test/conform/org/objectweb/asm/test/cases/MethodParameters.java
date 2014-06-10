/***
 * ASM: a very small and fast Java bytecode manipulation framework
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

package org.objectweb.asm.test.cases;

import java.io.IOException;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

/**
 * Generates a class with 2 methods with method parameters.
 * 
 * @author Remi Forax
 */
public class MethodParameters extends Generator {
    @Override
    public void generate(final String dir) throws IOException {
        generate(dir, "pkg/MethodParameters.class", dumpCode());
    }

    public byte[] dumpCode() {
        ClassWriter cw = new ClassWriter(0);
        ClassVisitor cv = cw;
        cv.visit(V1_8, ACC_PUBLIC + ACC_ABSTRACT, "pkg/MethodParameters", null,
                "java/lang/Object", null);

        // static method
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC + ACC_STATIC, "m",
                "(ILjava/lang/Object;Ljava/lang/String;Ljava/lang/Object;I)V",
                null, null);

        // parameter 0 type int
        mv.visitParameter("i", 0);
        // parameter 1 type Object
        mv.visitParameter("o", ACC_FINAL);
        // parameter 2 type String
        mv.visitParameter("s", ACC_MANDATED);
        // parameter 3 type Object
        mv.visitParameter("o2", ACC_SYNTHETIC);
        // parameter 4 type Object
        mv.visitParameter("i2", ACC_FINAL + ACC_SYNTHETIC);

        mv.visitCode();
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 5);
        mv.visitEnd();

        // abstract method
        MethodVisitor mv2 = cv.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "m",
                "(J)V", null, null);

        // parameter 0 type long
        mv2.visitParameter("l", 0);
        mv2.visitEnd();

        cv.visitEnd();
        return cw.toByteArray();
    }
}
