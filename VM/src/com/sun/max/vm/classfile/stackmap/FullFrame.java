/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.classfile.stackmap;

import static com.sun.max.vm.classfile.StackMapTable.*;

import java.io.*;

import com.sun.max.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.verifier.types.*;

/**
 * The frame type represented by the tag value 255. The form of such a frame is then:
 * <p>
 * <pre>
 *      full_frame {
 *          u1 frame_type = FULL_FRAME; // 255
 *          u2 offset_delta;
 *          u2 number_of_locals;
 *          verification_type_info locals[number_of_locals];
 *          u2 number_of_stack_items;
 *          verification_type_info stack[number_of_stack_items];
 *      }
 * </pre>
 * <p>
 * The 0th entry in locals represents the type of local variable 0. If locals[M] represents local variable N, then
 * locals[M+1] represents local variable N+1 if locals[M] is one of Top_variable_info, Integer_variable_info,
 * Float_variable_info, Null_variable_info, UninitializedThis_variable_info, Object_variable_info, or
 * Uninitialized_variable_info, otherwise locals[M+1] represents local variable N+2. It is an error if, for any index i,
 * locals[i] represents a local variable whose index is greater than the maximum number of local variables for the
 * method.
 * <p>
 * The 0th entry in stack represents the type of the bottom of the stack, and subsequent entries represent types of
 * stack elements closer to the top of the operand stack. We shall refer to the bottom element of the stack as stack
 * element 0, and to subsequent elements as stack element 1, 2 etc. If stack[M] represents stack element N, then
 * stack[M+1] represents stack element N+1 if stack[M] is one of Top_variable_info, Integer_variable_info,
 * Float_variable_info, Null_variable_info, UninitializedThis_variable_info, Object_variable_info, or
 * Uninitialized_variable_info, otherwise stack[M+1] represents stack element N+2. It is an error if, for any index i,
 * stack[i] represents a stack entry whose index is greater than the maximum operand stack size for the method.
 *
 * @author David Liu
 * @author Doug Simon
 */
public class FullFrame extends StackMapFrame {

    private final VerificationType[] locals;
    private final VerificationType[] stack;

    public FullFrame(int bciDelta, VerificationType[] locals, VerificationType[] stack) {
        super(bciDelta);
        this.locals = locals;
        this.stack = stack;
    }

    public FullFrame(int frameType, ClassfileStream classfileStream, VerificationRegistry registry) {
        this(
            classfileStream.readUnsigned2(),
            VerificationType.readVerificationTypes(classfileStream, registry, classfileStream.readUnsigned2()),
            VerificationType.readVerificationTypes(classfileStream, registry, classfileStream.readUnsigned2()));
        assert frameType == FULL_FRAME;
    }

    @Override
    public void applyTo(FrameModel frameModel) {
        frameModel.clear();
        for (int i = 0; i < locals.length; i++) {
            frameModel.store(locals[i], frameModel.activeLocals());
        }
        for (int i = 0; i < stack.length; i++) {
            frameModel.push(stack[i]);
        }
    }

    @Override
    public void write(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
        stream.writeByte(frameType());
        stream.writeShort(bciDelta());
        stream.writeShort(locals.length);
        for (int i = 0; i < locals.length; i++) {
            locals[i].write(stream, constantPoolEditor);
        }
        stream.writeShort(stack.length);
        for (int i = 0; i < stack.length; i++) {
            stack[i].write(stream, constantPoolEditor);
        }
    }

    @Override
    public int frameType() {
        return FULL_FRAME;
    }

    @Override
    public String toString() {
        return "frame_type = " + frameType() + " /* full_frame */\n" +
               "  offset_delta = " + bciDelta() + "\n" +
               "  number_of_locals = " + locals.length + "\n" +
               "  locals = [ " + Utils.toString(locals, ", ") + " ]\n" +
               "  number_of_stack_items = " + stack.length + "\n" +
               "  stack = [ " + Utils.toString(stack, ", ") + " ]";
    }
}
