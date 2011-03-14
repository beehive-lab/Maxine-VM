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

    private final VerificationType[] locals;

    public AppendFrame(int bciDelta, VerificationType[] locals) {
        super(bciDelta);
        assert locals.length > 0 && locals.length <= 3;
        this.locals = locals;
    }

    public AppendFrame(int frameType, ClassfileStream classfileStream, VerificationRegistry registry) {
        this(classfileStream.readUnsigned2(), VerificationType.readVerificationTypes(classfileStream, registry, frameType - SAME_FRAME_EXTENDED));
        assert frameType > SAME_FRAME_EXTENDED && frameType < FULL_FRAME;
    }

    @Override
    public void applyTo(FrameModel frameModel) {
        for (int i = 0; i < locals.length; i++) {
            frameModel.store(locals[i], frameModel.activeLocals());
        }
        frameModel.clearStack();
    }

    @Override
    public void write(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
        stream.writeByte(frameType());
        stream.writeShort(bciDelta());
        for (int i = 0; i < locals.length; i++) {
            locals[i].write(stream, constantPoolEditor);
        }
    }

    @Override
    public int frameType() {
        return SAME_FRAME_EXTENDED + locals.length;
    }

    @Override
    public String toString() {
        return "frame_type = " + frameType() + " /* append_frame */\n" +
               "  offset_delta = " + bciDelta() + "\n" +
               "  locals = [ " + Utils.toString(locals, ", ") + " ]";
    }
}
