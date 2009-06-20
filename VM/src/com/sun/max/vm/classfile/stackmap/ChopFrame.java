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
 *
 * @author David Liu
 * @author Doug Simon
 */
public class ChopFrame extends StackMapFrame {

    private final int chop;

    public ChopFrame(int positionDelta, int chop) {
        super(positionDelta);
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
        stream.writeShort(positionDelta());
    }

    @Override
    public int frameType() {
        return SAME_FRAME_EXTENDED - chop;
    }

    @Override
    public String toString() {
        return "frame_type = " + frameType() + " /* chop_frame */\n" +
               "  chop = " + chop + " /* implicit */\n" +
               "  offset_delta = " + positionDelta();
    }
}
