package org.objectweb.asm.util;

import org.objectweb.asm.*;

import java.io.FileInputStream;

/***
 * ASM: a very small and fast Java bytecode manipulation framework Copyright (c) 2000-2011 INRIA, France Telecom All
 * rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met: 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or other materials provided with the
 * distribution. 3. Neither the name of the copyright holders nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * Created by andyn on 05/06/14.
 *
 * Based on the Textifier A {@link Printer} that prints a disassembled view of the classes it visits.
 *
 * This class aims to extract the byte code for a method that we then pass to T1X, it will not handle annotations at the
 * moment.
 *
 * @author Eric Bruneton
 * @author Dr. Andy Nisbet
 */

class ClassAdapter extends ClassVisitor {

    private String keepMethod;

    ClassAdapter(ClassVisitor cv, String keepMethod) {

        super(Opcodes.ASM5, cv);
        this.keepMethod = keepMethod;

    }

    @Override
    public void visitAttribute(Attribute attr) {
        return;
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        return;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        return;

    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        return;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {

        return null;
    }

    @Override
    public void visitSource(String source, String debug) {
        return;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return null;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        final MethodVisitor mv;
        if (name.equals(keepMethod)) {
            mv = super.visitMethod(access, name, desc, signature, exceptions);
        } else {
            mv = null;
        }
        return mv;
    }
}

public class MaxineByteCode {

    public MaxineByteCode() {

    }

    public byte[] getByteArray(String keepMethod, String className) throws Exception  {
        ClassReader cr;
        if (className.endsWith(".class") || className.indexOf('\\') > -1 || className.indexOf('/') > -1) {
            cr = new ClassReader(new FileInputStream(className));
        } else {
            cr = new ClassReader(className);
        }
        ClassWriter cw = new ClassWriter(0);
        ClassVisitor cv = new ClassAdapter(cw, keepMethod);
        cr.accept(cv, 0);
        return cw.toByteArrayMethod();
    }

    public static void main(final String[] args) throws Exception {
        boolean ok = true;
        int i = 0;
        if (args.length < 1 || args.length > 3) {
            ok = false;
        }
        if (ok && "-debug".equals(args[0])) {
            i = 2;
            if (args.length != 3) {
                ok = false;
            }
        }
        if (!ok) {
            System.err.println("Prints the bytecode of any method called \"run\" in a class, needs path to class");
            System.err.println("Usage: MaxineByteCode [-debug] run" + "<fully qualified class name or class file name>");
            return;
        }
        ClassReader cr;
        if (args[i].endsWith(".class") || args[i].indexOf('\\') > -1 || args[i].indexOf('/') > -1) {
            cr = new ClassReader(new FileInputStream(args[i]));
        } else {
            cr = new ClassReader(args[i]);
        }

        ClassWriter cw = new ClassWriter(0);
        ClassVisitor cv = new ClassAdapter(cw, args[1]);
        cr.accept(cv, 0);

        byte[] code = cw.toByteArray();
        for (int j = 0; j < code.length; j++) {
            System.out.println("Byte " + j + " " + Integer.toString(0xff & code[j], 16));
        }
    }
}
