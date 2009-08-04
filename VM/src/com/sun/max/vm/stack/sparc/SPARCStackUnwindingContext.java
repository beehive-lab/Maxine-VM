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
package com.sun.max.vm.stack.sparc;

import com.sun.max.unsafe.*;
import com.sun.max.vm.stack.*;

/**
 * Keeps track of the last visited jit->opt adapter frame.
 * This is necessary to restore the correct register window if the exception is handled in a JIT frame.
 *
 * @author Laurent Daynes
 */
public class SPARCStackUnwindingContext extends StackUnwindingContext {
    private Word stackPointer;
    private Word framePointer;
    private boolean isTopFrame;

    public SPARCStackUnwindingContext(Word stackPointer, Word framePointer, Throwable throwable) {
        super(throwable);
        // Initialize with top frame's pointers. This is necessary when unwinding starts from an signal handler stub.
        this.stackPointer = stackPointer;
        this.framePointer = framePointer;
        this.isTopFrame = true;
    }

    public void record(Pointer stackPointer, Pointer framePointer) {
        this.stackPointer = stackPointer;
        this.framePointer = framePointer;
        this.isTopFrame = false;
    }

    public Pointer stackPointer() {
        return stackPointer.asPointer();
    }

    public Pointer framePointer() {
        return framePointer.asPointer();
    }

    public boolean isTopFrame() {
        return isTopFrame;
    }
}
