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

import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Random;

import org.objectweb.asm.util.TraceClassVisitor;

import junit.framework.TestSuite;

/**
 * ClassWriter tests.
 * 
 * @author Eric Bruneton
 */
public class ClassWriterComputeFramesDeadCodeTest extends AbstractTest {

    public static void premain(final String agentArgs,
            final Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            public byte[] transform(final ClassLoader loader,
                    final String className, final Class<?> classBeingRedefined,
                    final ProtectionDomain domain, final byte[] classFileBuffer)
                    throws IllegalClassFormatException {
                String n = className.replace('/', '.');
                if (n.indexOf("javax") == -1 || n.startsWith("invalid.")) {
                    return null;
                }
                if (agentArgs.length() == 0 || n.indexOf(agentArgs) != -1) {
                    return transformClass(n, classFileBuffer);
                } else {
                    return null;
                }
            }
        });
    }

    static byte[] transformClass(final String n, final byte[] clazz) {
        ClassReader cr = new ClassReader(clazz);
        ClassWriter cw = new ComputeClassWriter(ClassWriter.COMPUTE_FRAMES);
        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {

            private String className;

            @Override
            public void visit(final int version, final int access,
                    final String name, final String signature,
                    final String superName, final String[] interfaces) {
                className = name;
                // Set V1_7 version to prevent fallback to old verifier.
                super.visit((version & 0xFFFF) < Opcodes.V1_7 ? Opcodes.V1_7
                        : version, access, name, signature, superName,
                        interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name,
                    String desc, String signature, String[] exceptions) {
                int seed = (className + "." + name + desc).hashCode();
                return new MethodDeadCodeInserter(seed, super.visitMethod(
                        access, name, desc, signature, exceptions));
            }

        }, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        byte[] b = cw.toByteArray();
        if (n.equals("javax.imageio.ImageIO"))
            new ClassReader(b).accept(new TraceClassVisitor(new PrintWriter(
                    System.err)), 0);

        return b;
    }

    public static TestSuite suite() throws Exception {
        TestSuite suite = new ClassWriterComputeFramesDeadCodeTest().getSuite();
        suite.addTest(new VerifierTest());
        return suite;
    }

    @Override
    public void test() throws Exception {
        try {
            Class.forName(n, true, getClass().getClassLoader());
        } catch (NoClassDefFoundError ncdfe) {
            // ignored
        } catch (UnsatisfiedLinkError ule) {
            // ignored
        } catch (ClassFormatError cfe) {
            fail(cfe.getMessage());
        } catch (VerifyError ve) {
            // String s = n.replace('.', '/') + ".class";
            // InputStream is =
            // getClass().getClassLoader().getResourceAsStream(s);
            // ClassReader cr = new ClassReader(is);
            // byte[] b = transformClass("", cr.b);
            // StringWriter sw1 = new StringWriter();
            // StringWriter sw2 = new StringWriter();
            // sw2.write(ve.toString() + "\n");
            // ClassVisitor cv1 = new TraceClassVisitor(new PrintWriter(sw1));
            // ClassVisitor cv2 = new TraceClassVisitor(new PrintWriter(sw2));
            // cr.accept(cv1, 0);
            // new ClassReader(b).accept(cv2, 0);
            // String s1 = sw1.toString();
            // String s2 = sw2.toString();
            // assertEquals("different data", s1, s2);
            fail(ve.getMessage());
        }
    }
}

class MethodDeadCodeInserter extends MethodVisitor implements Opcodes {

    private Random r;

    public MethodDeadCodeInserter(int seed, final MethodVisitor mv) {
        super(ASM5, mv);
        r = new Random(seed);
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        insertDeadcode();
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        insertDeadcode();
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        insertDeadcode();
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
        insertDeadcode();
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name,
            String desc) {
        super.visitFieldInsn(opcode, owner, name, desc);
        insertDeadcode();
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
            String desc, boolean itf) {
        super.visitMethodInsn(opcode, owner, name, desc, itf);
        insertDeadcode();
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
            Object... bsmArgs) {
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        insertDeadcode();
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        insertDeadcode();
    }

    @Override
    public void visitLdcInsn(Object cst) {
        super.visitLdcInsn(cst);
        insertDeadcode();
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        insertDeadcode();
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt,
            Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        insertDeadcode();
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        insertDeadcode();
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        super.visitMultiANewArrayInsn(desc, dims);
        insertDeadcode();
    }

    private void insertDeadcode() {
        // inserts dead code once every 50 instructions in average
        if (r.nextFloat() < 1.0 / 50.0) {
            Label end = new Label();
            mv.visitJumpInsn(Opcodes.GOTO, end);
            mv.visitLdcInsn("DEAD CODE");
            mv.visitLabel(end);
        }
    }
}
