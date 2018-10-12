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

import java.io.*;

import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * Representation of a frame in a {@linkplain StackMapTable StackMapTable class file attribute}.
 */
public abstract class StackMapFrame {

    private final int bciDelta;

    /**
     * Creates a stack map frame.
     *
     * @param bciDelta the value that is used to derive the {@linkplain #getBCI(int) BCI} at
     *            which this frame applies based on the BCI at which the previous frame applies
     */
    public StackMapFrame(int bciDelta) {
        this.bciDelta = bciDelta;
    }

    public int bciDelta() {
        return bciDelta;
    }

    /**
     * Gets the BCI at which this frame applies based on the BCI at which the previous frame
     * applies.
     *
     * @param previousFrameBCI the BCI at which the previous frame applies or -1 if this is the first
     *            frame
     */
    public final int getBCI(int previousFrameBCI) {
        return previousFrameBCI == -1 ? bciDelta : previousFrameBCI + 1 + bciDelta;
    }

    /**
     * Transforms a given frame model to match the state described by this stack map frame.
     *
     * @param frameModel a frame model whose current state is the result of calling this method on the previous stack
     *            map frame
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
