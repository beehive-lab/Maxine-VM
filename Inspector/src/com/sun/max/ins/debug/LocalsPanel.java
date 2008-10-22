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

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.stack.*;

/**
 * A panel for displaying locals and operand stack of a stack frame associated with a method
 * compiled with the template-based compiler.
 * @author Laurent Daynes
 *
 */
public final class LocalsPanel extends InspectorPanel {

    public final int _maxLocals;
    public final int _maxStacks;
    public final int _numArguments;

    public LocalsPanel(Inspection inspection, JavaStackFrame javaStackFrame) {
        super(inspection, new SpringLayout());
        final ClassMethodActor classMethodActor = javaStackFrame.targetMethod().classMethodActor();
        _maxLocals = classMethodActor.codeAttribute().maxLocals();
        _maxStacks = classMethodActor.codeAttribute().maxStack();
        _numArguments = classMethodActor.numberOfParameterLocals();
    }

    public void refresh(long epoch) {
        // No data that can change
    }

    public void redisplay() {
        // No apparent view configuration information
    }
}
