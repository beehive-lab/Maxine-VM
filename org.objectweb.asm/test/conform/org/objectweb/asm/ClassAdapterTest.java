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

import junit.framework.TestSuite;

/**
 * ClassVisitor tests.
 * 
 * @author Eric Bruneton
 */
public class ClassAdapterTest extends AbstractTest {

    private final static TestClassLoader LOADER = new TestClassLoader();

    public static TestSuite suite() throws Exception {
        return new ClassAdapterTest().getSuite();
    }

    @Override
    public void test() throws Exception {
        ClassReader cr = new ClassReader(is);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {

            @Override
            public AnnotationVisitor visitAnnotation(String desc,
                    boolean visible) {
                return new AnnotationAdapter(super.visitAnnotation(desc,
                        visible));
            }

            @Override
            public AnnotationVisitor visitTypeAnnotation(int typeRef,
                    TypePath typePath, String desc, boolean visible) {
                return new AnnotationAdapter(super.visitTypeAnnotation(typeRef,
                        typePath, desc, visible));
            }

            @Override
            public FieldVisitor visitField(int access, String name,
                    String desc, String signature, Object value) {
                return new FieldVisitor(Opcodes.ASM5, super.visitField(access,
                        name, desc, signature, value)) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc,
                            boolean visible) {
                        return new AnnotationAdapter(super.visitAnnotation(
                                desc, visible));
                    }

                    @Override
                    public AnnotationVisitor visitTypeAnnotation(int typeRef,
                            TypePath typePath, String desc, boolean visible) {
                        return new AnnotationAdapter(super.visitTypeAnnotation(
                                typeRef, typePath, desc, visible));
                    }
                };
            }

            @Override
            public MethodVisitor visitMethod(int access, String name,
                    String desc, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM5, super.visitMethod(
                        access, name, desc, signature, exceptions)) {
                    @Override
                    public AnnotationVisitor visitAnnotationDefault() {
                        return new AnnotationAdapter(super
                                .visitAnnotationDefault());
                    }

                    @Override
                    public AnnotationVisitor visitAnnotation(String desc,
                            boolean visible) {
                        return new AnnotationAdapter(super.visitAnnotation(
                                desc, visible));
                    }

                    @Override
                    public AnnotationVisitor visitTypeAnnotation(int typeRef,
                            TypePath typePath, String desc, boolean visible) {
                        return new AnnotationAdapter(super.visitTypeAnnotation(
                                typeRef, typePath, desc, visible));
                    }

                    @Override
                    public AnnotationVisitor visitParameterAnnotation(
                            int parameter, String desc, boolean visible) {
                        return new AnnotationAdapter(super
                                .visitParameterAnnotation(parameter, desc,
                                        visible));
                    }

                    @Override
                    public AnnotationVisitor visitInsnAnnotation(int typeRef,
                            TypePath typePath, String desc, boolean visible) {
                        return new AnnotationAdapter(super.visitInsnAnnotation(
                                typeRef, typePath, desc, visible));
                    }

                    @Override
                    public AnnotationVisitor visitTryCatchAnnotation(
                            int typeRef, TypePath typePath, String desc,
                            boolean visible) {
                        return new AnnotationAdapter(super
                                .visitTryCatchAnnotation(typeRef, typePath,
                                        desc, visible));
                    }

                    @Override
                    public AnnotationVisitor visitLocalVariableAnnotation(
                            int typeRef, TypePath typePath, Label[] start,
                            Label[] end, int[] index, String desc,
                            boolean visible) {
                        return new AnnotationAdapter(super
                                .visitLocalVariableAnnotation(typeRef,
                                        typePath, start, end, index, desc,
                                        visible));
                    }
                };
            }
        }, 0);
        byte[] b = cw.toByteArray();
        try {
            LOADER.defineClass(n, b);
        } catch (ClassFormatError cfe) {
            fail(cfe.getMessage());
        } catch (Throwable ignored) {
        }
    }

    static class AnnotationAdapter extends AnnotationVisitor {

        public AnnotationAdapter(final AnnotationVisitor av) {
            super(Opcodes.ASM5, av);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            return new AnnotationAdapter(super.visitAnnotation(name, desc));
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return new AnnotationAdapter(super.visitArray(name));
        }
    }

    // ------------------------------------------------------------------------

    static class TestClassLoader extends ClassLoader {

        public Class<?> defineClass(final String name, final byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }
}
