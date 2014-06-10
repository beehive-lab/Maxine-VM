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

package org.objectweb.asm;

import junit.framework.TestCase;

/**
 * Backward compatibility unit tests.
 * 
 * @author Eric Bruneton
 */
public class BackwardCompatibilityUnitTest extends TestCase {

    public void testBackwardCompatibilityOk() {
        CheckMethodVisitor cmv = new CheckMethodVisitor();
        MethodVisitor mv = new MethodVisitor4(cmv);
        mv.visitMethodInsn(0, "C", "m", "()V", false);
        assertEquals("m", cmv.name);
    }

    public void testBackwardCompatibilityFail() {
        CheckMethodVisitor cmv = new CheckMethodVisitor();
        MethodVisitor mv = new MethodVisitor4(cmv);
        try {
            mv.visitMethodInsn(0, "C", "m", "()V", true);
            fail();
        } catch (RuntimeException e) {
        }
    }

    public void testBackwardCompatibilityOverride() {
        CheckMethodVisitor cmv = new CheckMethodVisitor();
        MethodVisitor mv = new MethodVisitor4Override(cmv);
        mv.visitMethodInsn(0, "C", "m", "()V", false);
        assertEquals("mv4", cmv.name);
    }

    public void testBackwardCompatibilityOverrideFail() {
        CheckMethodVisitor cmv = new CheckMethodVisitor();
        MethodVisitor mv = new MethodVisitor4Override(cmv);
        try {
            mv.visitMethodInsn(0, "C", "m", "()V", true);
            fail();
        } catch (RuntimeException e) {
        }
    }

    public void testNewMethod() {
        CheckMethodVisitor cmv = new CheckMethodVisitor();
        MethodVisitor mv = new MethodVisitor5(cmv);
        mv.visitMethodInsn(0, "C", "m", "()V", false);
        assertEquals("m", cmv.name);
    }

    public void testNewMethodOverride() {
        CheckMethodVisitor cmv = new CheckMethodVisitor();
        MethodVisitor mv = new MethodVisitor5Override(cmv);
        mv.visitMethodInsn(0, "C", "m", "()V", false);
        assertEquals("mv5", cmv.name);
    }

    public void testBackwardCompatibilityMixedChain() {
        CheckMethodVisitor cmv = new CheckMethodVisitor();
        MethodVisitor mv = new MethodVisitor4(cmv);
        mv = new MethodVisitor4Override(mv);
        mv = new MethodVisitor5(mv);
        mv = new MethodVisitor5Override(mv);
        mv.visitMethodInsn(0, "C", "m", "()V", false);
        assertEquals("mv5v4", cmv.name);
    }

    public void testBackwardCompatibilityMixedChain2() {
        CheckMethodVisitor cmv = new CheckMethodVisitor();
        MethodVisitor mv = new MethodVisitor5(cmv);
        mv = new MethodVisitor5Override(mv);
        mv = new MethodVisitor4(mv);
        mv = new MethodVisitor4Override(mv);
        mv.visitMethodInsn(0, "C", "m", "()V", false);
        assertEquals("mv4v5", cmv.name);
    }

    class MethodVisitor4 extends MethodVisitor {
        MethodVisitor4(MethodVisitor mv) {
            super(Opcodes.ASM4, mv);
        }
    }

    class MethodVisitor4Override extends MethodVisitor {
        MethodVisitor4Override(MethodVisitor mv) {
            super(Opcodes.ASM4, mv);
        }

        @Deprecated
        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                String desc) {
            super.visitMethodInsn(opcode, owner, name + "v4", desc);
        }
    }

    class MethodVisitor5 extends MethodVisitor {
        MethodVisitor5(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }
    }

    class MethodVisitor5Override extends MethodVisitor {
        MethodVisitor5Override(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                String desc, boolean itf) {
            super.visitMethodInsn(opcode, owner, name + "v5", desc, itf);
        }
    }

    class CheckMethodVisitor extends MethodVisitor {
        public String name;

        CheckMethodVisitor() {
            super(Opcodes.ASM5);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                String desc, boolean itf) {
            this.name = name;
        }
    }
}
