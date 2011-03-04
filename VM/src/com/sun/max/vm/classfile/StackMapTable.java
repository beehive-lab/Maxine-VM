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

package com.sun.max.vm.classfile;

import static com.sun.max.vm.classfile.ErrorContext.*;

import java.io.*;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.classfile.stackmap.*;
import com.sun.max.vm.verifier.*;
import com.sun.max.vm.verifier.types.*;

/**
 * From <a href="http://jcp.org/en/jsr/detail?id=202">JSR 202</a>:
 * <p>
 * The stack map attribute is a variable-length attribute in the attributes table of a Code attribute. The name of the
 * attribute is StackMapTable. This attribute is used during the process of
 * {@linkplain TypeCheckingVerifier verification by typechecking}.
 * <p>
 * A stack map attribute consists of zero or more stack map frames. Each stack map frame specifies (either explicitly or
 * implicitly) a bytecode position, the {@linkplain VerificationType verification types} for the local variables, and
 * the verification types for the operand stack.
 * <p>
 * The type checker deals with and manipulates the expected types of a method's local variables and operand stack.
 * Throughout this section, a location refers to either a single local variable or to a single operand stack entry. We
 * will use the terms stack map frame and type state interchangeably to describe a mapping from locations in the operand
 * stack and local variables of a method to verification types. We will usually use the term stack map frame when such a
 * mapping is provided in the class file, and the term type state when the mapping is inferred by the type checker.
 * <p>
 * If a method's Code attribute does not have a StackMapTable attribute, it has an implicit stack map attribute. This
 * implicit stack map attribute is equivalent to a StackMapTable attribute with number_of_entries equal to zero. A
 * method's Code attribute may have at most one StackMapTable attribute, otherwise a ClassFormatError is thrown.
 * <p>
 * The format of the stack map in the class file is given below.
 * <p>
 *
 * <pre>
 *       stack_map {
 *           u2 attribute_name_index;
 *           u4 attribute_length
 *           u2 number_of_entries;
 *           stack_map_frame entries[number_of_entries];
 *       }
 * </pre>
 *
 * <p>
 * Each stack_map_frame structure specifies the type state at a particular bytecode position. Each frame type specifies
 * (explicitly or implicitly) a {@link StackMapFrame#positionDelta() delta} that is used to calculate the actual
 * bytecode position at which it applies. The bytecode position at which the frame applies is given by adding
 * {@code 1 + delta} to the position of the previous frame, unless the previous frame is the initial frame of the
 * method, in which case the bytecode position is {@code delta}.
 *
 * @author Doug Simon
 */
public class StackMapTable {

    public static final int SAME_FRAME_BOUND                  = 64;
    public static final int SAME_LOCALS_1_STACK_ITEM_BOUND    = 128;
    public static final int SAME_LOCALS_1_STACK_ITEM_EXTENDED = 247;
    public static final int SAME_FRAME_EXTENDED               = 251;
    public static final int FULL_FRAME                        = 255;

    public static final StackMapFrame[] NO_STACK_MAP_FRAMES = {};

    private final byte[] attributeData;

    /**
     * Gets the stack map frames defined by this attribute.
     * <p>
     * This implementation parses the bytes of the attribute each time this method is called and so the result should be
     * cached if it is to be read more than once.
     *
     * @param registry
     *                used to create specific {@linkplain ReferenceType reference types}. Can be null in which case
     *                {@link VerificationType#REFERENCE} is used in the returned frames
     */
    public final StackMapFrame[] getFrames(VerificationRegistry registry) {
        return readFrames(registry, attributeData);
    }

    private static StackMapFrame[] readFrames(VerificationRegistry registry, byte[] attributeData) {
        final ClassfileStream classfileStream = new ClassfileStream(attributeData);
        final int numberOfEntries = classfileStream.readUnsigned2();
        if (numberOfEntries == 0) {
            return NO_STACK_MAP_FRAMES;
        }
        final StackMapFrame[] entries = new StackMapFrame[numberOfEntries];
        for (int i = 0; i < numberOfEntries; i++) {
            entries[i] = readStackMapFrame(classfileStream, registry);
        }
        return entries;
    }

    /**
     *
     * @param classfileStream
     * @param registry
     *                used to create specific {@linkplain ReferenceType reference types}. If {@code null}, then
     *                {@link VerificationType#OBJECT} is return for any object type and
     *                {@link VerificationType#UNINITIALIZED} is returned for any uninitialized type
     * @return
     */
    public static StackMapFrame readStackMapFrame(ClassfileStream classfileStream, VerificationRegistry registry) {
        final int type = classfileStream.readUnsigned1();
        StackMapFrame returnFrame = null;
        if (type < SAME_FRAME_BOUND) {
            returnFrame = new SameFrame(type, classfileStream);
        } else if (type < SAME_LOCALS_1_STACK_ITEM_BOUND) {
            returnFrame =  new SameLocalsOneStack(type, classfileStream, registry);
        } else if (type < SAME_LOCALS_1_STACK_ITEM_EXTENDED) {
            /* throw an error: these are reserved for future use */
            throw classFormatError("Invalid StackFrame type id " + type);
        } else if (type == SAME_LOCALS_1_STACK_ITEM_EXTENDED) {
            returnFrame =  new SameLocalsOneStackExtended(type, classfileStream, registry);
        } else if (type < SAME_FRAME_EXTENDED) {
            returnFrame =  new ChopFrame(type, classfileStream);
        } else if (type == SAME_FRAME_EXTENDED) {
            returnFrame =  new SameFrameExtended(type, classfileStream);
        } else if (type < FULL_FRAME) {
            returnFrame =  new AppendFrame(type, classfileStream, registry);
        } else if (type == FULL_FRAME) {
            returnFrame =  new FullFrame(type, classfileStream, registry);
        } else {
            /* throw error, out of bound _type */
            throw classFormatError("Out of range StackFrame type id " + type);
        }
        return returnFrame;
    }

    public StackMapTable(StackMapFrame[] frames, ConstantPoolEditor constantPoolEditor) {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeShort(frames.length);
            for (StackMapFrame frame : frames) {
                frame.write(dataOutputStream, constantPoolEditor);
            }
        } catch (IOException e) {
            // This will never occur
            ProgramError.unexpected(e);
        }
        attributeData = byteArrayOutputStream.toByteArray();
    }

    public StackMapTable(ClassfileStream classfileStream, final ConstantPool constantPool, Size attributeSize) {
        attributeData = classfileStream.readByteArray(attributeSize);
    }

    /**
     * Writes this stack map table to a given stream as a StackMapTable class file attribute.
     *
     * @param stream
     *                a data output stream that has just written the 'attribute_name_index' and 'attribute_length'
     *                fields of a class file attribute
     * @param constantPoolEditor
     */
    public void writeAttributeInfo(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
        stream.write(attributeData);
    }
}
