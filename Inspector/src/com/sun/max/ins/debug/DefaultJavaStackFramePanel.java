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

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.vm.stack.*;

/**
 * Display panel specialized for displaying VM stack frames for Java methods.
 *
 * @author Michael Van De Vanter
 */
public final class DefaultJavaStackFramePanel extends CompiledStackFramePanel<CompiledStackFrame> {

    private final CompiledStackFrameTable javaStackFrameTable;

    public DefaultJavaStackFramePanel(Inspection inspection, CompiledStackFrame javaStackFrame, MaxThread thread, CompiledStackFrameViewPreferences preferences) {
        super(inspection, javaStackFrame);
        javaStackFrameTable = new CompiledStackFrameTable(inspection, javaStackFrame, thread, preferences);
        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), javaStackFrameTable);
        add(scrollPane, BorderLayout.CENTER);
        refresh(true);
    }

    @Override
    public void refresh(boolean force) {
        javaStackFrameTable.refresh(force);
        super.refresh(force);
    }

    @Override
    public void redisplay() {
        javaStackFrameTable.redisplay();
        super.redisplay();
    }

}
