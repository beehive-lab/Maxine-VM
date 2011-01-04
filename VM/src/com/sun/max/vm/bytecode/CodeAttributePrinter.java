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
package com.sun.max.vm.bytecode;

import java.io.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.classfile.stackmap.*;
import com.sun.max.vm.verifier.*;

/**
 * Facilities for printing the details of a {@link CodeAttribute} in a textual format. This
 * includes support for disassembling the {@linkplain CodeAttribute#code()}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class CodeAttributePrinter {

    private CodeAttributePrinter() {
    }

    /**
     * Formats the contents of a given CodeAttribute to a string.
     */
    public static String toString(CodeAttribute codeAttribute) {
        CharArrayWriter charArrayWriter = new CharArrayWriter();
        print(charArrayWriter, codeAttribute);
        return charArrayWriter.toString();
    }

    /**
     * Prints the contents of a given CodeAttribute in a textual format to a given print stream.
     *
     * @param stream where to print the contents
     * @param codeAttribute the object to print
     */
    public static void print(OutputStream stream, CodeAttribute codeAttribute) {
        final PrintWriter writer = new PrintWriter(stream);
        print(writer, codeAttribute);
    }

    /**
     * Prints the contents of a given CodeAttribute in a textual format to a given print writer.
     *
     * @param writer where to print the contents
     * @param codeAttribute the object to print
     */
    public static void print(Writer writer, CodeAttribute codeAttribute) {
        final PrintWriter printWriter = (writer instanceof PrintWriter) ? (PrintWriter) writer : new PrintWriter(writer);
        printWriter.println("Stack=" + (int) codeAttribute.maxStack + ", Locals=" + (int) codeAttribute.maxLocals);
        final BytecodePrinter bytecodePrinter = new BytecodePrinter(printWriter, codeAttribute.constantPool);
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(bytecodePrinter);
        try {
            bytecodeScanner.scan(new BytecodeBlock(codeAttribute.code()));

            // Print the attributes within the CodeAttribute
            printExceptionHandlerTable(codeAttribute, printWriter);
            printStackMapTable(codeAttribute, printWriter);
            printLineNumberTable(codeAttribute, printWriter);
            printLocalVariableTable(codeAttribute, printWriter);

        } catch (Throwable throwable) {
            printWriter.flush();
            ProgramWarning.message("could not print bytecodes: " + throwable);
        }
        printWriter.flush();
    }

    /**
     * Prints the contents of the {@linkplain CodeAttribute#exceptionHandlerTable() exception table} in a given
     * code attribute object to a given print writer. This method outputs nothing to {@code printWriter} if the exception
     * table is empty.
     *
     * @param codeAttribute
     *                a code attribute that contains a (possibly empty) exception table
     * @param printWriter
     *                where to print the contents of the exception table
     */
    public static void printExceptionHandlerTable(CodeAttribute codeAttribute, final PrintWriter printWriter) {
        final ExceptionHandlerEntry[] exceptionHandlerTable = codeAttribute.exceptionHandlerTable();
        if (exceptionHandlerTable.length != 0) {
            printWriter.println("Exception Handlers:");
            printWriter.println("  from  to  target  type");
            for (ExceptionHandlerEntry entry : exceptionHandlerTable) {
                String catchType;
                final int catchTypeIndex = entry.catchTypeIndex();
                if (catchTypeIndex == 0) {
                    catchType = "*any*";
                } else {
                    try {
                        catchType = codeAttribute.constantPool.classAt(catchTypeIndex).typeDescriptor().toJavaString();
                    } catch (ClassFormatError classFormatError) {
                        catchType = "*ERROR[cpi=" + catchTypeIndex + "]*";
                    }
                }
                printWriter.println(String.format("  %-6d%-4d%-8d%s", entry.startPosition(), entry.endPosition(), entry.handlerPosition(), catchType));
            }
        }
        printWriter.flush();
    }

    /**
     * Prints the contents of the {@linkplain CodeAttribute#stackMapTable() stack map table} in a given code attribute
     * object to a given print writer. This method outputs nothing to {@code printWriter} if the stack map table is
     * empty or does not exist.
     *
     * @param codeAttribute a code attribute that contains a (possibly null) stack map table
     * @param printWriter where to print the contents of the stack map table
     */
    public static void printStackMapTable(CodeAttribute codeAttribute, final PrintWriter printWriter) {
        final StackMapTable stackMapTable = codeAttribute.stackMapTable();
        if (stackMapTable != null) {
            final Verifier verifier = new Verifier(codeAttribute.constantPool);
            final StackMapFrame[] frames = stackMapTable.getFrames(verifier);
            printWriter.println("StackMapTable: number of entries = " + frames.length);
            int previousFrameOffset = -1;
            for (int i = 0; i != frames.length; ++i) {
                final StackMapFrame stackMapFrame = frames[i];
                final int offset = stackMapFrame.getPosition(previousFrameOffset);
                printWriter.println(Strings.indent(offset + ": " + stackMapFrame.toString(), "  "));
                previousFrameOffset = offset;
            }
        }
        printWriter.flush();
    }

    /**
     * Prints the contents of the {@linkplain CodeAttribute#lineNumberTable() line number table} in a given code
     * attribute object to a given print writer. This method outputs nothing to {@code printWriter} if the line number
     * table is empty.
     *
     * @param codeAttribute a code attribute that contains a (possibly empty) line number table
     * @param printWriter where to print the contents of the line number table
     */
    public static void printLineNumberTable(CodeAttribute codeAttribute, final PrintWriter printWriter) {
        final LineNumberTable lineNumberTable = codeAttribute.lineNumberTable();
        if (!lineNumberTable.isEmpty()) {
            printWriter.println("LineNumberTable:");
            for (LineNumberTable.Entry entry : lineNumberTable.entries()) {
                printWriter.println("  line " + entry.lineNumber() + ": " + entry.position());
            }
        }
        printWriter.flush();
    }

    /**
     * Prints the contents of the {@linkplain CodeAttribute#localVariableTable() local variable table} in a given code
     * attribute object to a given print writer. This method outputs nothing to {@code printWriter} if the local
     * variable table is empty.
     *
     * @param codeAttribute a code attribute that contains a (possibly empty) local variable table
     * @param printWriter where to print the contents of the local variable table
     */
    public static void printLocalVariableTable(CodeAttribute codeAttribute, final PrintWriter printWriter) {
        final LocalVariableTable localVariableTable = codeAttribute.localVariableTable();
        if (!localVariableTable.isEmpty()) {
            printWriter.println("LocalVariableTable:");
            printWriter.println("  Start Length Slot Name               Descriptor            Generic-signature");
            final ConstantPool cp = codeAttribute.constantPool;
            for (LocalVariableTable.Entry entry : localVariableTable.entries()) {
                final int signatureIndex = entry.signatureIndex();
                final String name = utf8At(cp, entry.nameIndex());
                final String descriptor = utf8At(cp, entry.descriptorIndex());
                final String genericSignature = signatureIndex == 0 ? "" : utf8At(cp, signatureIndex);
                printWriter.println(String.format("  %-6d%-7d%-5d%-19s%-22s%s", entry.startPosition(), entry.length(), entry.slot(), name, descriptor, genericSignature));
            }
        }
        printWriter.flush();
    }

    /**
     * Gets a UTF8 string from a given constant pool. If a {@code ClassFormatError} occurs while retrieving the UTF8
     * value, then the result of calling {@link ClassFormatError#toString()} on the error object is return instead.
     *
     * @param constantPool
     * @param index
     * @return the string version of the UTF8 at {@code index} in {@code constantPool}
     */
    public static String utf8At(ConstantPool constantPool, int index) {
        try {
            return constantPool.utf8At(index, null).toString();
        } catch (ClassFormatError classFormatError) {
            return classFormatError.toString();
        }
    }
}
