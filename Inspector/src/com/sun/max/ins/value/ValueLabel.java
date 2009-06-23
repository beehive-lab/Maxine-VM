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
package com.sun.max.ins.value;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
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
    }

    protected ValueLabel(Inspection inspection, Value value) {
        super(inspection);
        this.value = value;
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
        value = fetchValue();
        lastRefreshedState = maxVMState();
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
        if (maxVMState().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = maxVMState();
            setValue(fetchValue());
        }
    }

}
