/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.verifier.types.*;

/**
 * A stack map frame that has exactly the same locals as the previous stack map frame and that the number of
 * stack items is 1. The {@code offset_delta} value for the frame is given explicitly. There is a
 * verification_type_info following the frame_type for the one stack item. The form of such a frame is then:
 * <p>
 * <pre>
 *      same_locals_1_stack_item_frame_extended {
 *          u1 frame_type = SAME_LOCALS_1_STACK_ITEM_EXTENDED; // 247
 *          u2 offset_delta;
 *          verification_type_info stack[1];
 *      }
 * </pre>
 *
 * @author David Liu
 * @author Doug Simon
 */
public class SameLocalsOneStackExtended extends StackMapFrame {

    private final VerificationType singleStackItem;

    public SameLocalsOneStackExtended(int positionDelta, VerificationType singleStackItem) {
        super(positionDelta);
        this.singleStackItem = singleStackItem;
    }

    public SameLocalsOneStackExtended(int frameType, ClassfileStream classfileStream, VerificationRegistry registry) {
        this(classfileStream.readUnsigned2(), VerificationType.readVerificationType(classfileStream, registry));
        assert frameType == SAME_LOCALS_1_STACK_ITEM_EXTENDED;
    }

    @Override
    public void applyTo(FrameModel frameModel) {
        frameModel.clearStack();
        frameModel.push(singleStackItem);
    }

    @Override
    public void write(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
        stream.writeByte(frameType());
        stream.writeShort(positionDelta());
        singleStackItem.write(stream, constantPoolEditor);
    }

    @Override
    public int frameType() {
        return SAME_LOCALS_1_STACK_ITEM_EXTENDED;
    }

    @Override
    public String toString() {
        return "frame_type = " + frameType() + " /* same_locals_1_stack_item_frame_extended */\n" +
               "  offset_delta = " + positionDelta() + "\n" +
               "  stack = [ " + singleStackItem + " ]";
    }
}
