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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;

/**
 * Generates a class with type annotations of all kinds.
 * 
 * @author Eric Bruneton
 */
public class TypeAnnotation extends Generator {

    @Override
    public void generate(final String dir) throws IOException {
        generate(dir, "pkg/TypeAnnotations.class", dumpTypeAnnotations());
    }

    public byte[] dumpTypeAnnotations() {
        ClassWriter cw = new ClassWriter(0);
        ClassVisitor cv = cw;
        cv.visit(
                V1_8,
                ACC_PUBLIC,
                "pkg/TypeAnnotations",
                "<E:Lpkg/C;F:Lpkg/D;>Lpkg/S<TE;TF;>;Lpkg/I1<TE;>;Lpkg/I2<TF;>;",
                "pkg/S", new String[] { "pkg/I1", "pkg/I2" });
        AnnotationVisitor av = cv.visitAnnotation("LA1;", true);
        av.visit("a", "0");
        av.visit("b", new Integer(1));
        cv.visitAnnotation("LA2;", false).visitEnd();
        // targets param 0
        av = cv.visitTypeAnnotation(
                TypeReference.newTypeParameterReference(
                        TypeReference.CLASS_TYPE_PARAMETER, 0).getValue(),
                null, "LA3;", true);
        av.visitAnnotation("c", "LA4;").visitEnd();
        // targets param 1
        cv.visitTypeAnnotation(
                TypeReference.newTypeParameterReference(
                        TypeReference.CLASS_TYPE_PARAMETER, 1).getValue(),
                null, "LA5;", true).visitEnd();
        // targets param 0, bound 0
        cv.visitTypeAnnotation(
                TypeReference.newTypeParameterBoundReference(
                        TypeReference.CLASS_TYPE_PARAMETER_BOUND, 0, 0)
                        .getValue(), null, "LA6;", true).visitEnd();
        // targets param 1, bound 0
        cv.visitTypeAnnotation(
                TypeReference.newTypeParameterBoundReference(
                        TypeReference.CLASS_TYPE_PARAMETER_BOUND, 0, 0)
                        .getValue(), null, "LA7;", true).visitEnd();
        // targets super class
        cv.visitTypeAnnotation(
                TypeReference.newSuperTypeReference(-1).getValue(), null,
                "LA8;", true).visitEnd();
        // targets super class, type argument 0
        cv.visitTypeAnnotation(
                TypeReference.newSuperTypeReference(-1).getValue(),
                TypePath.fromString("0"), "LA9;", true).visitEnd();
        // targets super class, type argument 1
        cv.visitTypeAnnotation(
                TypeReference.newSuperTypeReference(-1).getValue(),
                TypePath.fromString("1"), "LA10;", true).visitEnd();
        // targets interface 0
        cv.visitTypeAnnotation(
                TypeReference.newSuperTypeReference(0).getValue(), null,
                "LA11;", true).visitEnd();
        // targets interface 0, type argument 0
        cv.visitTypeAnnotation(
                TypeReference.newSuperTypeReference(0).getValue(),
                TypePath.fromString("0"), "LA12;", true).visitEnd();
        // targets interface 1
        cv.visitTypeAnnotation(
                TypeReference.newSuperTypeReference(1).getValue(), null,
                "LA13;", true).visitEnd();
        // targets interface 1, type argument 0
        cv.visitTypeAnnotation(
                TypeReference.newSuperTypeReference(1).getValue(),
                TypePath.fromString("0"), "LA14;", false).visitEnd();

        FieldVisitor fv = cv.visitField(ACC_PUBLIC, "f", "Lpkg/S;",
                "Lpkg/S<TE;TF;>;", null);
        av = fv.visitAnnotation("LB1;", true);
        av.visit("c", "0");
        av.visit("d", new Integer(1));
        fv.visitAnnotation("LB2;", false).visitEnd();
        // targets type argument 0
        fv.visitTypeAnnotation(
                TypeReference.newTypeReference(TypeReference.FIELD).getValue(),
                TypePath.fromString("0"), "LB3;", true).visitEnd();
        // targets type argument 1
        fv.visitTypeAnnotation(
                TypeReference.newTypeReference(TypeReference.FIELD).getValue(),
                TypePath.fromString("1"), "LB4;", false).visitEnd();
        fv.visitEnd();

        String signature = "<E:Lpkg/X;F:Lpkg/Y;>(TE;TF;Lpkg/Z<+TE;+TF;>;)Lpkg/Z<+TE;+TF;>;^Lpkg/E1<TX;>;^Lpkg/E2<TY;>;";
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC + ACC_STATIC, "m",
                "(Lpkg/X;Lpkg/Y;Lpkg/Z;)Lpkg/Z;", signature, new String[] {
                        "pkg/E1", "pkg/E2" });
        av = mv.visitAnnotation("LC1;", true);
        av.visit("e", "0");
        av.visit("f", new Integer(1));
        mv.visitAnnotation("LC2;", false).visitEnd();
        // targets param 0
        av = mv.visitTypeAnnotation(
                TypeReference.newTypeParameterReference(
                        TypeReference.METHOD_TYPE_PARAMETER, 0).getValue(),
                null, "LC3;", true);
        av.visitAnnotation("c", "LC4;").visitEnd();
        // targets param 1
        mv.visitTypeAnnotation(
                TypeReference.newTypeParameterReference(
                        TypeReference.METHOD_TYPE_PARAMETER, 1).getValue(),
                null, "LC5;", true).visitEnd();
        // targets param 0, bound 0
        mv.visitTypeAnnotation(
                TypeReference.newTypeParameterBoundReference(
                        TypeReference.METHOD_TYPE_PARAMETER_BOUND, 0, 0)
                        .getValue(), null, "LC6;", true).visitEnd();
        // targets param 1, bound 0
        mv.visitTypeAnnotation(
                TypeReference.newTypeParameterBoundReference(
                        TypeReference.METHOD_TYPE_PARAMETER_BOUND, 1, 0)
                        .getValue(), null, "LC7;", true).visitEnd();
        // targets return type
        mv.visitTypeAnnotation(
                TypeReference.newTypeReference(TypeReference.METHOD_RETURN)
                        .getValue(), null, "LC8;", true).visitEnd();
        // targets return type, type argument 0
        mv.visitTypeAnnotation(
                TypeReference.newTypeReference(TypeReference.METHOD_RETURN)
                        .getValue(), TypePath.fromString("0"), "LC9;", true)
                .visitEnd();
        // targets return type, type argument 1
        mv.visitTypeAnnotation(
                TypeReference.newTypeReference(TypeReference.METHOD_RETURN)
                        .getValue(), TypePath.fromString("1"), "LC10;", true)
                .visitEnd();
        // no receiver type (static method)
        //
        // targets parameter 0
        mv.visitTypeAnnotation(
                TypeReference.newFormalParameterReference(0).getValue(), null,
                "LC11;", true).visitEnd();
        // targets parameter 1
        mv.visitTypeAnnotation(
                TypeReference.newFormalParameterReference(1).getValue(), null,
                "LC12;", true).visitEnd();
        // targets parameter 2
        mv.visitTypeAnnotation(
                TypeReference.newFormalParameterReference(2).getValue(), null,
                "LC13;", true).visitEnd();
        // targets parameter 2, type argument 0
        mv.visitTypeAnnotation(
                TypeReference.newFormalParameterReference(2).getValue(),
                TypePath.fromString("0"), "LC14;", true).visitEnd();
        // targets parameter 2, type argument 1
        mv.visitTypeAnnotation(
                TypeReference.newFormalParameterReference(2).getValue(),
                TypePath.fromString("1"), "LC15;", true).visitEnd();
        // targets exception 0
        mv.visitTypeAnnotation(
                TypeReference.newExceptionReference(0).getValue(), null,
                "LC16;", true).visitEnd();
        // targets exception 1
        mv.visitTypeAnnotation(
                TypeReference.newExceptionReference(1).getValue(), null,
                "LC17;", true).visitEnd();
        // targets exception 0, type argument 0
        mv.visitTypeAnnotation(
                TypeReference.newExceptionReference(0).getValue(),
                TypePath.fromString("0"), "LC18;", true).visitEnd();
        // targets exception 1, type argument 1
        mv.visitTypeAnnotation(
                TypeReference.newExceptionReference(1).getValue(),
                TypePath.fromString("1"), "LC19;", false).visitEnd();
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        Label l3 = new Label();
        Label l4 = new Label();
        Label l5 = new Label();
        Label l6 = new Label();
        Label l7 = new Label();
        mv.visitTryCatchBlock(l2, l3, l7, "pkg/E1");
        mv.visitTryCatchBlock(l5, l6, l7, "pkg/E2");
        // targets try catch block 0
        mv.visitTryCatchAnnotation(
                TypeReference.newTryCatchReference(0).getValue(), null,
                "LC20;", true).visitEnd();
        // targets try catch block 1
        mv.visitTryCatchAnnotation(
                TypeReference.newTryCatchReference(1).getValue(), null,
                "LC21;", false).visitEnd();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLabel(l0);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitInsn(NOP);
        mv.visitInsn(NOP);
        mv.visitInsn(NOP);
        mv.visitLabel(l1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitLabel(l2);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitInsn(NOP);
        mv.visitInsn(NOP);
        mv.visitInsn(NOP);
        mv.visitLabel(l3);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLabel(l4);
        mv.visitVarInsn(ASTORE, 3);
        // targets the astore instruction
        av = mv.visitInsnAnnotation(
                TypeReference.newTypeArgumentReference(TypeReference.CAST, 0)
                        .getValue(), null, "LC22;", true);
        av.visit("g", "0");
        av.visit("h", new Integer(1));
        av.visitEnd();
        // targets the astore instruction
        mv.visitInsnAnnotation(
                TypeReference.newTypeArgumentReference(TypeReference.CAST, 0)
                        .getValue(), null, "LC23;", false).visitEnd();
        mv.visitInsn(NOP);
        mv.visitInsn(NOP);
        mv.visitInsn(NOP);
        mv.visitLabel(l5);
        mv.visitInsn(NOP);
        mv.visitLabel(l6);
        mv.visitInsn(ACONST_NULL);
        mv.visitLabel(l7);
        mv.visitVarInsn(ASTORE, 4);
        // targets the astore instruction
        mv.visitInsnAnnotation(
                TypeReference.newTypeArgumentReference(TypeReference.CAST, 0)
                        .getValue(), null, "LC24;", true).visitEnd();
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitLocalVariable("a", "Lpkg/X;", "TE;", l0, l1, 3);
        mv.visitLocalVariable("b", "Lpkg/Z;", "Lpkg/Z<+TE;+TF;>;", l2, l3, 3);
        mv.visitLocalVariable("a", "Lpkg/X;", "TE;", l4, l5, 3);
        mv.visitLocalVariable("a", "Lpkg/X;", "TE;", l0, l1, 3);
        // targets local variable a
        av = mv.visitLocalVariableAnnotation(
                TypeReference.newTypeReference(TypeReference.LOCAL_VARIABLE)
                        .getValue(), null, new Label[] { l0, l4 }, new Label[] {
                        l1, l5 }, new int[] { 3, 3 }, "LD0;", true);
        av.visit("i", "0");
        av.visit("j", new Integer(1));
        av.visitEnd();
        // targets local variable b
        mv.visitLocalVariableAnnotation(
                TypeReference.newTypeReference(TypeReference.LOCAL_VARIABLE)
                        .getValue(), null, new Label[] { l2 },
                new Label[] { l3 }, new int[] { 3 }, "LC25;", true).visitEnd();
        // targets local variable b, type argument 0
        mv.visitLocalVariableAnnotation(
                TypeReference.newTypeReference(TypeReference.LOCAL_VARIABLE)
                        .getValue(), TypePath.fromString("0"),
                new Label[] { l2 }, new Label[] { l3 }, new int[] { 3 },
                "LC26;", true).visitEnd();
        // targets local variable b, type argument 1
        mv.visitLocalVariableAnnotation(
                TypeReference.newTypeReference(TypeReference.LOCAL_VARIABLE)
                        .getValue(), TypePath.fromString("1"),
                new Label[] { l2 }, new Label[] { l3 }, new int[] { 3 },
                "LC27;", false).visitEnd();
        mv.visitMaxs(1, 5);
        mv.visitEnd();
        cv.visitEnd();
        return cw.toByteArray();
    }
}
