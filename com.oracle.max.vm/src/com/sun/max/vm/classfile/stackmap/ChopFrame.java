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

import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * A frame where the operand stack is empty and the current locals are the same as the locals in the previous frame,
 * except that the {@code k} last locals are absent. The value of {@code k} is given by the formula
 * {@code 251 - frame_type}. The form of such a frame is then:
 * <p>
 * <pre>
 *      chop_frame {
 *          u1 frame_type = CHOP; // 248-250
 *          u2 offset_delta;
 *      }
 * </pre>
 */
public class ChopFrame extends StackMapFrame {

    private final int chop;

    public ChopFrame(int bciDelta, int chop) {
        super(bciDelta);
        assert chop >= 1 && chop <= 3;
        this.chop = chop;
    }

    public ChopFrame(int frameType, ClassfileStream classfileStream) {
        this(classfileStream.readUnsigned2(), SAME_FRAME_EXTENDED - frameType);
    }

    @Override
    public void applyTo(FrameModel frameModel) {
        frameModel.chopLocals(chop);
        frameModel.clearStack();
    }

    @Override
    public void write(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
        stream.writeByte(frameType());
        stream.writeShort(bciDelta());
    }

    @Override
    public int frameType() {
        return SAME_FRAME_EXTENDED - chop;
    }

    @Override
    public String toString() {
        return "frame_type = " + frameType() + " /* chop_frame */\n" +
               "  chop = " + chop + " /* implicit */\n" +
               "  offset_delta = " + bciDelta();
    }
}
