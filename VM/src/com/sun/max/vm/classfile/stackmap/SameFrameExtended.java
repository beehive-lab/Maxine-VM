/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A frame that has exactly the same locals as the previous stack map frame and that the number of stack items is zero.
 * The form of such a frame is then:
 * <p>
 * <pre>
 *      same_frame_extended {
 *          u1 frame_type = SAME_FRAME_EXTENDED; // 251
 *          u2 offset_delta;
 *      }
 * </pre>
 * 
 * @author David Liu
 * @author Doug Simon
 */
public class SameFrameExtended extends StackMapFrame {

    public SameFrameExtended(int positionDelta) {
        super(positionDelta);
    }

    public SameFrameExtended(int frameType, ClassfileStream classfileStream) {
        this(classfileStream.readUnsigned2());
        assert frameType == SAME_FRAME_EXTENDED;
    }

    @Override
    public void applyTo(FrameModel frameModel) {
        frameModel.clearStack();
    }

    @Override
    public void write(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
        stream.writeByte(frameType());
        stream.writeShort(positionDelta());
    }

    @Override
    public int frameType() {
        return SAME_FRAME_EXTENDED;
    }

    @Override
    public String toString() {
        return "frame_type = " + frameType() + " /* same_frame_extended */\n" +
               "  offset_delta = " + positionDelta();
    }
}
