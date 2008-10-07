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
package com.sun.max.vm.jit.amd64;

import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 */
final class JitStackFrameInfo {
    private boolean _isAdapterFrame;

    public boolean isAdapterFrame() {
        return _isAdapterFrame;
    }

    private final int _callSaveAreaPosition;

    public int callSaveAreaPosition() {
        return _callSaveAreaPosition;
    }

    private final int _framePointerPosition;

    public int framePointerPosition() {
        return _framePointerPosition;
    }

    private final Pointer _instructionPointer;

    public Pointer instructionPointer() {
        return _instructionPointer;
    }

    public JitStackFrameInfo(int saveAreaPosition, int framePointerPosition, Pointer instructionPointer) {
        _isAdapterFrame = false;
        _callSaveAreaPosition = saveAreaPosition;
        _framePointerPosition = framePointerPosition;
        _instructionPointer = instructionPointer;
    }

    public JitStackFrameInfo(int saveAreaPosition, Pointer instructionPointer) {
        _isAdapterFrame = true;
        _callSaveAreaPosition = saveAreaPosition;
        _framePointerPosition = -1;
        _instructionPointer = instructionPointer;
    }
}
