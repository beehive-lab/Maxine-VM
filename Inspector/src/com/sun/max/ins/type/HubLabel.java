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
package com.sun.max.ins.type;

import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;

/**
 * A label specialized for displaying a reference in the header of a low-level
 * Maxine object in the VM to the object's {@link Hub}.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class HubLabel extends InspectorLabel {

    private final TeleHub teleHub;

    private MaxVMState lastRefreshedState = null;

    public HubLabel(Inspection inspection, TeleHub hub) {
        super(inspection);
        this.teleHub = hub;
        addMouseListener(new InspectorMouseClickAdapter(inspection()) {
            @Override
            public void procedure(MouseEvent mouseEvent) {
                if (Inspection.mouseButtonWithModifiers(mouseEvent) == MouseEvent.BUTTON1) {
                    if (mouseEvent.isControlDown()) {
                        actions().inspectObjectMemoryWords(teleHub).perform();
                    } else {
                        focus().setHeapObject(teleHub);
                    }
                }
            }
        });
        redisplay();
    }

    public void refresh(boolean force) {
        if (vmState().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = vmState();
            updateText();
        }
    }

    public void redisplay() {
        setFont(style().javaNameFont());
        updateText();
    }

    private void updateText() {
        final Class javaType = teleHub.hub().classActor.toJava();
        setText(inspection().nameDisplay().referenceLabelText(teleHub));
        if (!(javaType.isPrimitive() || Word.class.isAssignableFrom(javaType))) {
            setToolTipText(inspection().nameDisplay().referenceToolTipText(teleHub));
        }
    }

}
