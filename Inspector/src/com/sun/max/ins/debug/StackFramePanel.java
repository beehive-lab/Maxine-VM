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
package com.sun.max.ins.debug;

import java.awt.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.stack.*;


abstract class StackFramePanel<StackFrame_Type extends StackFrame> extends InspectorPanel {

    protected StackFrame_Type stackFrame;

    public StackFramePanel(Inspection inspection, StackFrame_Type stackFrame) {
        super(inspection, new BorderLayout());
        this.stackFrame = stackFrame;
    }

    public final StackFrame_Type stackFrame() {
        return stackFrame;
    }

    public final void setStackFrame(StackFrame stackFrame) {
        final Class<StackFrame_Type> type = null;
        this.stackFrame = StaticLoophole.cast(type, stackFrame);
        refresh(true);
    }

    public void instructionPointerFocusChanged(Pointer instructionPointer) {
    }
}
