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
import com.sun.max.ins.method.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.layout.*;

/**
 * An object inspector specialized for displaying a Maxine low-level heap object in the {@link TeleVM} constructed using {@link TupleLayout}.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public class TupleInspector extends ObjectInspector<TupleInspector> {

    private ObjectFieldsPanel _objectFieldsPanel;

    private final InspectorMenuItems _classMethodInspectorMenuItems;

    private final InspectorMenuItems _targetMethodInspectorMenuItems;

    TupleInspector(Inspection inspection, Residence residence, TeleObject teleObject) {
        super(inspection, residence, teleObject);
        createFrame(null);
        final TeleClassMethodActor teleClassMethodActor = teleObject.getTeleClassMethodActorForObject();
        if (teleClassMethodActor != null) {
            _classMethodInspectorMenuItems = new ClassMethodMenuItems(inspection(), teleClassMethodActor);
        } else {
            _classMethodInspectorMenuItems = null;
        }
        if (TeleTargetMethod.class.isAssignableFrom(teleObject.getClass())) {
            final TeleTargetMethod teleTargetMethod = (TeleTargetMethod) teleObject;
            _targetMethodInspectorMenuItems = new TargetMethodMenuItems(inspection(), teleTargetMethod);
        } else {
            _targetMethodInspectorMenuItems = null;
        }
        if (_classMethodInspectorMenuItems != null) {
            frame().add(_classMethodInspectorMenuItems);
        }
        if (_targetMethodInspectorMenuItems != null) {
            frame().add(_targetMethodInspectorMenuItems);
        }
    }

    @Override
    protected synchronized void createView(long epoch) {
        super.createView(epoch);
        _objectFieldsPanel = new ObjectFieldsPanel(this, getFieldActors(), teleObject());
        frame().getContentPane().add(new JScrollPane(_objectFieldsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        super.refreshView(epoch, force);
        _objectFieldsPanel.refresh(epoch, force);
        if (_classMethodInspectorMenuItems != null) {
            _classMethodInspectorMenuItems.refresh(epoch, force);
        }
        if (_targetMethodInspectorMenuItems != null) {
            _targetMethodInspectorMenuItems.refresh(epoch, force);
        }
    }

}
