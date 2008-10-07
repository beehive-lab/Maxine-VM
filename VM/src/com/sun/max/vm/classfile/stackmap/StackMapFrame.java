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

import java.io.*;

import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * Representation of a frame in a {@linkplain StackMapTable StackMapTable class file attribute}.
 * 
 * @author David Liu
 * @author Doug Simon
 */
public abstract class StackMapFrame {

    private final int _positionDelta;

    /**
     * Creates a stack map frame.
     * 
     * @param positionDelta
     *            the value that is used to derive the {@linkplain #getPosition(int) bytecode position} at which
     *            this frame applies based on the bytecode position at which the previous frame applies
     */
    public StackMapFrame(int positionDelta) {
        _positionDelta = positionDelta;
    }

    public int positionDelta() {
        return _positionDelta;
    }

    /**
     * Gets the bytecode position at which this frame applies based on the bytecode position at which the previous frame
     * applies.
     * 
     * @param previousFramePosition
     *            the bytecode position at which the previous frame applies or -1 if this is the first frame
     */
    public final int getPosition(int previousFramePosition) {
        return previousFramePosition == -1 ? _positionDelta : previousFramePosition + 1 + _positionDelta;
    }

    /**
     * Transforms a given frame model to match the state described by this stack map frame.
     * 
     * @param frameModel
     *            a frame model whose current state is the result of calling this method on the previous stack map frame
     */
    public abstract void applyTo(FrameModel frameModel);

    /**
     * Writes this stack map frame to a stream in class file format.
     * 
     * @param stream
     * @param constantPoolEditor
     */
    public abstract void write(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException;

    public abstract int frameType();

    /**
     * Gets a string representation of this frame. The format of the returned string closely
     * resembles the output of javap.
     */
    @Override
    public abstract String toString();
}
