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
/*VCSID=eca5ea26-b864-4b37-a404-96ed8318d9b5*/
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
 * 
 * @author David Liu
 * @author Doug Simon
 */
public class SameFrame extends StackMapFrame {

    public SameFrame(int positionDelta) {
        super(positionDelta);
        assert positionDelta >= 0 && positionDelta < SAME_FRAME_BOUND;
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
        return positionDelta();
    }

    @Override
    public String toString() {
        return "frame_type = " + frameType() + " /* same_frame */\n" +
               "  offset_delta = " + positionDelta() + " /* implicit */";
    }
}
