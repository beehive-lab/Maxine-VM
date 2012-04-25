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
 * A stack map frame that has exactly the same locals as the previous stack map frame and that the number of
 * stack items is zero. The {@code offset_delta} value for the frame is the value of the tag item, {@code frame_type}.
 * The form of such a frame is then:
 * <p>
 * <pre>
 *      same_frame {
 *          u1 frame_type = SAME; // 0-63
 *      }
 * </pre>
 */
public class SameFrame extends StackMapFrame {

    public SameFrame(int bciDelta) {
        super(bciDelta);
        assert bciDelta >= 0 && bciDelta < SAME_FRAME_BOUND;
    }

    public SameFrame(int frameType, ClassfileStream classfileStream) {
        this(frameType);
    }

    @Override
    public void applyTo(FrameModel frameModel) {
        frameModel.clearStack();
    }

    @Override
    public void write(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
        stream.writeByte(frameType());
    }

    @Override
    public int frameType() {
        return bciDelta();
    }

    @Override
    public String toString() {
        return "frame_type = " + frameType() + " /* same_frame */\n" +
               "  offset_delta = " + bciDelta() + " /* implicit */";
    }
}
