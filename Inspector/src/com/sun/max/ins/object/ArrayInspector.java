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
package com.sun.max.ins.object;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.type.*;

/**
 * An object inspector specialized for displaying a Maxine low-level object in the {@link TeleVM}, constructed using {@link ArrayLayout}.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class ArrayInspector extends ObjectInspector<ArrayInspector> {

    private InspectorPanel _elementsPanel;

    ArrayInspector(Inspection inspection, Residence residence, TeleObject teleObject) {
        super(inspection, residence, teleObject);
        createFrame(null);
    }

    @Override
    protected synchronized void createView(long epoch) {
        super.createView(epoch);
        final TeleArrayObject teleArrayObject = (TeleArrayObject) teleObject();
        final int length = teleArrayObject.getLength();
        final ArrayClassActor arrayClassActor = (ArrayClassActor) teleObject().classActorForType();
        final Kind kind = arrayClassActor.componentClassActor().kind();
        final WordValueLabel.ValueMode valueMode = kind == Kind.REFERENCE ? WordValueLabel.ValueMode.REFERENCE : WordValueLabel.ValueMode.WORD;
        final int arrayOffsetFromOrigin = arrayClassActor.arrayLayout().getElementOffsetFromOrigin(0).toInt();
        _elementsPanel = new JTableObjectArrayPanel(this, kind, arrayOffsetFromOrigin, 0, length, "", valueMode);
        final JScrollPane scrollPane = new JScrollPane(_elementsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBackground(style().defaultBackgroundColor());
        scrollPane.setOpaque(true);
        frame().getContentPane().add(scrollPane);
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        super.refreshView(epoch, force);
        _elementsPanel.refresh(epoch, force);
    }

}
