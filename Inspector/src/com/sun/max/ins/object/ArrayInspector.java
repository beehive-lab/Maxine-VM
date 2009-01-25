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

    private final TeleArrayObject _teleArrayObject;
    private final ArrayClassActor _arrayClassActor;
    private final int _length;

    private ObjectArrayPanel _objectArrayPanel;

    ArrayInspector(Inspection inspection, Residence residence, TeleObject teleObject) {
        super(inspection, residence, teleObject);
        _teleArrayObject = (TeleArrayObject) teleObject;
        _arrayClassActor = (ArrayClassActor) teleObject.classActorForType();
        _length = _teleArrayObject.getLength();
        createFrame(null);
    }

    @Override
    protected synchronized void createView(long epoch) {
        super.createView(epoch);
        final Kind kind = _arrayClassActor.componentClassActor().kind();
        final WordValueLabel.ValueMode valueMode = kind == Kind.REFERENCE ? WordValueLabel.ValueMode.REFERENCE : WordValueLabel.ValueMode.WORD;
        //final JPanel elementsPanel = createArrayPanel(_valueLabels, kind, _arrayClassActor.arrayLayout().getElementOffsetFromOrigin(0).toInt(), 0, _length, "", valueMode);
        _objectArrayPanel = new ObjectArrayPanel(this, _teleArrayObject, kind, _arrayClassActor.arrayLayout().getElementOffsetFromOrigin(0).toInt(), 0, _length, "", valueMode);
        frame().getContentPane().add(new JScrollPane(_objectArrayPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        super.refreshView(epoch, force);
        _objectArrayPanel.refresh(epoch, force);
    }

}
