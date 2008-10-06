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
/*VCSID=7e7ee94d-56d6-40f9-9380-9cdefae4130f*/
package com.sun.max.vm.classfile.stackmap;

import static com.sun.max.vm.classfile.StackMapTable.*;

import java.io.*;

import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.verifier.types.*;

/**
 * A stack map frame that has exactly the same locals as the previous stack map frame and that the number of
 * stack items is 1. The {@code offset_delta} value for the frame is the value {@code (frame_type - 64)}. There is a
 * verification_type_info following the frame_type for the one stack item. The form of such a frame is then:
 * <p>
 * <pre>
 *      same_locals_1_stack_item_frame {
 *          u1 frame_type = SAME_LOCALS_1_STACK_ITEM; // 64-127
 *          verification_type_info stack[1];
 *      }
 * </pre>
 * 
 * @author David Liu
 * @author Doug Simon
 */
public class SameLocalsOneStack extends StackMapFrame {

    private final VerificationType _singleStackItem;

    public SameLocalsOneStack(int positionDelta, VerificationType singleStackItem) {
        super(positionDelta);
        assert positionDelta >= 0 && positionDelta <= (SAME_LOCALS_1_STACK_ITEM_BOUND - SAME_FRAME_BOUND);
        _singleStackItem = singleStackItem;
    }

    public SameLocalsOneStack(int frameType, ClassfileStream classfileStream, VerificationRegistry registry) {
        this(frameType - SAME_FRAME_BOUND, VerificationType.readVerificationType(classfileStream, registry));
    }

    @Override
    public void applyTo(FrameModel frameModel) {
        frameModel.clearStack();
        frameModel.push(_singleStackItem);
    }

    @Override
    public void write(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
        final int frameType = frameType();
        stream.writeByte(frameType);
        _singleStackItem.write(stream, constantPoolEditor);
    }

    @Override
    public int frameType() {
        return SAME_FRAME_BOUND + positionDelta();
    }

    @Override
    public String toString() {
        return "frame_type = " + frameType() + " /* same_locals_1_stack_item_frame */\n" +
               "  offset_delta = " + positionDelta() + " /* implicit */\n" +
               "  stack = [ " + _singleStackItem + " ]";
    }
}
