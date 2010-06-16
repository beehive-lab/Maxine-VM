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
package com.sun.max.vm.stack;

import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.collect.*;

/**
 * Describes the layout of a frame produced by the optimizing compiler.
 *
 * @author Doug Simon
 */
public class OptoStackFrameLayout extends CompiledStackFrameLayout {

    private final int frameSize;
    private final boolean isReturnAddressPushedByCall;

    public OptoStackFrameLayout(int frameSize) {
        this(frameSize, MaxineVM.target().configuration.platform().processorKind.instructionSet.callsPushReturnAddressOnStack());
    }

    public OptoStackFrameLayout(int frameSize, boolean isReturnAddressPushedByCall) {
        this.frameSize = frameSize;
        this.isReturnAddressPushedByCall = isReturnAddressPushedByCall;
    }

    @Override
    public int frameSize() {
        return frameSize;
    }

    @Override
    public boolean isReturnAddressPushedByCall() {
        return isReturnAddressPushedByCall;
    }

    @Override
    public int frameReferenceMapOffset() {
        return 0;
    }

    @Override
    public int frameReferenceMapSize() {
        return ByteArrayBitMap.computeBitMapSize(Unsigned.idiv(frameSize(), STACK_SLOT_SIZE));
    }
}
