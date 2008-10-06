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
/*VCSID=1ed5b88c-1211-4ac2-9d2b-28c2d18e6f7a*/

package com.sun.max.vm.classfile.stackmap;

import static com.sun.max.vm.classfile.StackMapTable.*;

import java.io.*;

import com.sun.max.lang.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.verifier.types.*;

/**
 * A frame where the operand stack is empty and the current locals are the same as the locals in the previous frame,
 * except that {@code k} additional locals are defined. The value of {@code k} is given by the formula
 * {@code frame_type - 251}. The form of such a frame is then:
 * <p>
 * <pre>
 *      append_frame {
 *          u1 frame_type = APPEND; // 252 - 254
 *          u2 offset_delta;
 *          verification_type_info locals[frame_type - 251];
 *      }
 *  </pre>
 *  <p>
 *  The 0th entry in locals represents the type of the first additional local variable. If
 *  locals[M] represents local variable N, then locals[M+1] represents local
 *  variable N+1 if locals[M] is one of Top_variable_info,
 *  Integer_variable_info, Float_variable_info, Null_variable_info,
 *  UninitializedThis_variable_info, Object_variable_info, or
 *  Uninitialized_variable_info, otherwise locals[M+1] represents local
 *  variable N+2. It is an error if, for any index i, locals[i] represents a local
 *  variable whose index is greater than the maximum number of local variables for
 *  the method.
 * 
 *  @author David Liu
 *  @author Doug Simon
 */
public class AppendFrame extends StackMapFrame {

    private final VerificationType[] _locals;

    public AppendFrame(int positionDelta, VerificationType[] locals) {
        super(positionDelta);
        assert locals.length > 0 && locals.length <= 3;
        _locals = locals;
    }

    public AppendFrame(int frameType, ClassfileStream classfileStream, VerificationRegistry registry) {
        this(classfileStream.readUnsigned2(), VerificationType.readVerificationTypes(classfileStream, registry, frameType - SAME_FRAME_EXTENDED));
        assert frameType > SAME_FRAME_EXTENDED && frameType < FULL_FRAME;
    }

    @Override
    public void applyTo(FrameModel frameModel) {
        for (int i = 0; i < _locals.length; i++) {
            frameModel.store(_locals[i], frameModel.activeLocals());
        }
        frameModel.clearStack();
    }

    @Override
    public void write(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
        stream.writeByte(frameType());
        stream.writeShort(positionDelta());
        for (int i = 0; i < _locals.length; i++) {
            _locals[i].write(stream, constantPoolEditor);
        }
    }

    @Override
    public int frameType() {
        return SAME_FRAME_EXTENDED + _locals.length;
    }

    @Override
    public String toString() {
        return "frame_type = " + frameType() + " /* append_frame */\n" +
               "  offset_delta = " + positionDelta() + "\n" +
               "  locals = [ " + Arrays.toString(_locals, ", ") + " ]";
    }
}
