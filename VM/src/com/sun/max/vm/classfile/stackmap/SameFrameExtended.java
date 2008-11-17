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
