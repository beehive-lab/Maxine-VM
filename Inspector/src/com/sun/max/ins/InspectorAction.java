/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins;

import java.awt.event.*;

import javax.swing.*;

import com.sun.max.gui.*;
import com.sun.max.ins.util.*;
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
