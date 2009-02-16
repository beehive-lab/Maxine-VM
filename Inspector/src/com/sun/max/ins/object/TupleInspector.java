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

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.method.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.layout.*;

/**
 * An object inspector specialized for displaying a Maxine low-level heap object in the {@link TeleVM} constructed using {@link TupleLayout}.
 *
 * @author Michael Van De Vanter
 */
public class TupleInspector extends ObjectInspector {

    private ObjectPane _fieldsPane;
    private final InspectorMenuItems _classMethodInspectorMenuItems;
    private final InspectorMenuItems _targetMethodInspectorMenuItems;

    TupleInspector(Inspection inspection, ObjectInspectorFactory factory, Residence residence, TeleObject teleObject) {
        super(inspection, factory, residence, teleObject);
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
        final TeleTupleObject teleTupleObject = (TeleTupleObject) teleObject();
        _fieldsPane = ObjectPane.createFieldsPane(this, teleTupleObject);
        frame().getContentPane().add(_fieldsPane);
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        if (isShowing() || force) {
            _fieldsPane.refresh(epoch, force);
            if (_classMethodInspectorMenuItems != null) {
                _classMethodInspectorMenuItems.refresh(epoch, force);
            }
            if (_targetMethodInspectorMenuItems != null) {
                _targetMethodInspectorMenuItems.refresh(epoch, force);
            }
            super.refreshView(epoch, force);
        }
    }

}
