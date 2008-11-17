/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.classfile.stackmap;

import static com.sun.max.vm.classfile.StackMapTable.*;

import java.io.*;

import com.sun.max.lang.*;
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

    private final VerificationType[] _locals;
    private final VerificationType[] _stack;

    public FullFrame(int positionDelta, VerificationType[] locals, VerificationType[] stack) {
        super(positionDelta);
        _locals = locals;
        _stack = stack;
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
        for (int i = 0; i < _locals.length; i++) {
            frameModel.store(_locals[i], frameModel.activeLocals());
        }
        for (int i = 0; i < _stack.length; i++) {
            frameModel.push(_stack[i]);
        }
    }

    @Override
    public void write(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
        stream.writeByte(frameType());
        stream.writeShort(positionDelta());
        stream.writeShort(_locals.length);
        for (int i = 0; i < _locals.length; i++) {
            _locals[i].write(stream, constantPoolEditor);
        }
        stream.writeShort(_stack.length);
        for (int i = 0; i < _stack.length; i++) {
            _stack[i].write(stream, constantPoolEditor);
        }
    }

    @Override
    public int frameType() {
        return FULL_FRAME;
    }

    @Override
    public String toString() {
        return "frame_type = " + frameType() + " /* full_frame */\n" +
               "  offset_delta = " + positionDelta() + "\n" +
               "  number_of_locals = " + _locals.length + "\n" +
               "  locals = [ " + Arrays.toString(_locals, ", ") + " ]\n" +
               "  number_of_stack_items = " + _stack.length + "\n" +
               "  stack = [ " + Arrays.toString(_stack, ", ") + " ]";
    }
}
