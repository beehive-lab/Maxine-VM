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
package org.objectweb.asm.tree;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestSuite;

import org.objectweb.asm.AbstractTest;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ClassNode tests.
 * 
 * @author Eric Bruneton
 */
public class ClassNodeTest extends AbstractTest {

    public static TestSuite suite() throws Exception {
        return new ClassNodeTest().getSuite();
    }

    @Override
    public void test() throws Exception {
        ClassReader cr = new ClassReader(is);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        // clone instructions for testing clone methods
        for (int i = 0; i < cn.methods.size(); ++i) {
            MethodNode mn = cn.methods.get(i);
            Iterator<AbstractInsnNode> it = mn.instructions.iterator();
            Map<LabelNode, LabelNode> m = new HashMap<LabelNode, LabelNode>() {
                @Override
                public LabelNode get(final Object o) {
                    return (LabelNode) o;
                }
            };
            while (it.hasNext()) {
                AbstractInsnNode insn = it.next();
                mn.instructions.set(insn, insn.clone(m));
            }
        }
        // test accept with visitors that remove class members
        cn.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public FieldVisitor visitField(int access, String name,
                    String desc, String signature, Object value) {
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name,
                    String desc, String signature, String[] exceptions) {
                return null;
            }
        });
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        assertEquals(cr, new ClassReader(cw.toByteArray()));
    }
}
