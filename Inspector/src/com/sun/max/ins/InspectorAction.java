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
package com.sun.max.ins;

import java.awt.event.*;

import javax.swing.*;

import com.sun.max.gui.*;
import com.sun.max.program.*;

/**
 * Base class for all "actions" in the Inspector that can be bound to menus, buttons, and keystrokes.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public abstract class InspectorAction extends AbstractAction implements Prober {

    private static final int TRACE_VALUE = 1;

    /**
     * Creates a disabled, impotent action.
     */
    public static InspectorAction dummyAction(Inspection inspection, String title) {
        final InspectorAction action = new InspectorAction(inspection, title) {

            @Override
            protected void procedure() {
            }
        };
        action.setEnabled(false);
        return action;
    }

    private String tracePrefix() {
        return "[InspectorAction] ";
    }

    private final Inspection inspection;

    /**
     * Creates an action than involves Inspector machinery in addition to GUI machinery.
     * @param title  name of the action for human consumption
     */
    public InspectorAction(Inspection inspection, String title) {
        super(title);
        this.inspection = inspection;
        inspection.registerAction(this);
    }

    /**
     * @return name of the action suitable for displaying to a user.
     */
    public String name() {
        return (String) getValue(Action.NAME);
    }

    /**
     * Sets the name of the action, as it will appear in menus.
     */
    protected void setName(String name) {
        putValue(Action.NAME, name);
    }

    protected abstract void procedure();

    public void refresh(boolean force) {
    }

    public void redisplay() {
    }

    private final Object actionTracer  = new Object() {
        @Override
        public String toString() {
            return tracePrefix() + name();
        }
    };

    public void perform() {
        Trace.begin(TRACE_VALUE, actionTracer);
        final long startTimeMillis = System.currentTimeMillis();
        inspection.gui().showInspectorBusy(true);
        try {
            procedure();
        } catch (InspectorError inspectorError) {
            inspectorError.display(inspection);
        } catch (Throwable throwable) {
            ThrowableDialog.showLater(throwable, inspection.gui().frame(), "Error while performing \"" + name() + "\"");
        } finally {
            inspection.setCurrentAction(null);
            Trace.end(TRACE_VALUE, actionTracer, startTimeMillis);
            inspection.gui().showInspectorBusy(false);
        }
    }

    public void actionPerformed(ActionEvent event) {
        perform();
    }

}
