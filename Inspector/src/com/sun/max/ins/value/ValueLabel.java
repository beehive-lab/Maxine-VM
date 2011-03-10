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
package com.sun.max.ins.value;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 *
 * A textual display label associated with a {@link Value}.
 * When it is to be read from the VM, it is
 * re-read when this label is refreshed.
 */
public abstract class ValueLabel extends InspectorLabel {

    // TODO (mlvdv) the flow of control in this class hierarchy is awkward and should be redesigned.

    private MaxVMState lastRefreshedState = null;

    private Value value;

    protected final Value value() {
        return value;
    }

    /**
     * Creates a new label with no initial value; the value must either be set
     * by {@link #setValue(Value)} or provided by overriding {@link #fetchValue()}.
     */
    protected ValueLabel(Inspection inspection) {
        super(inspection);
        enableDragSource();
    }

    protected ValueLabel(Inspection inspection, Value value) {
        super(inspection);
        this.value = value;
        enableDragSource();
    }

    /**
     * @return the current {@link Value}
     *
     * Subclasses override this to read from the VM for values that can change.
     */
    protected Value fetchValue() {
        return value;
    }

    /**
     * Sets initial value, display properties.
     */
    protected final void initializeValue() {
        try {
            value = fetchValue();
        } catch (DataIOError dataIOError) {
            value = VoidValue.VOID;
        }
        lastRefreshedState = vm().state();
    }

    /**
     * Using the currently cached {@link Value}, sets the text associated with the label.
     */
    protected abstract void updateText();

    /**
     * Explicitly sets the value, as opposed to letting it be read from the VM by an override of {@link #fetchValue()}.
     */
    public void setValue(Value value) {
        this.value = value;
        updateText();
        invalidate();
        repaint();
    }

    public final void refresh(boolean force) {
        if (vm().state().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = vm().state();
            Value newValue;
            try {
                newValue = fetchValue();
            } catch (DataIOError dataIOError) {
                newValue = VoidValue.VOID;
            }
            setValue(newValue);
        }
    }

}
