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
import com.sun.max.tele.object.*;
import com.sun.max.vm.layout.*;

/**
 * An object inspector specialized for displaying a Maxine low-level heap object in the VM constructed using {@link TupleLayout}.
 *
 * @author Michael Van De Vanter
 */
public class TupleInspector extends ObjectInspector {

    private ObjectScrollPane fieldsPane;
    private final InspectorMenuItems classMethodInspectorMenuItems;
    private final InspectorMenuItems targetMethodInspectorMenuItems;

    TupleInspector(Inspection inspection, ObjectInspectorFactory factory, TeleObject teleObject) {
        super(inspection, factory, teleObject);
        createFrame(null);
        final TeleClassMethodActor teleClassMethodActor = teleObject.getTeleClassMethodActorForObject();
        if (teleClassMethodActor != null) {
            classMethodInspectorMenuItems = new ClassMethodMenuItems(inspection(), teleClassMethodActor);
        } else {
            classMethodInspectorMenuItems = null;
        }
        if (TeleTargetMethod.class.isAssignableFrom(teleObject.getClass())) {
            final TeleTargetMethod teleTargetMethod = (TeleTargetMethod) teleObject;
            targetMethodInspectorMenuItems = new TargetMethodMenuItems(inspection(), teleTargetMethod);
        } else {
            targetMethodInspectorMenuItems = null;
        }
        if (classMethodInspectorMenuItems != null) {
            getMenu(DEFAULT_INSPECTOR_MENU).add(classMethodInspectorMenuItems);
        }
        if (targetMethodInspectorMenuItems != null) {
            getMenu(DEFAULT_INSPECTOR_MENU).add(targetMethodInspectorMenuItems);
        }
    }

    @Override
    protected void createView() {
        super.createView();
        final TeleTupleObject teleTupleObject = (TeleTupleObject) teleObject();
        fieldsPane = ObjectScrollPane.createFieldsPane(this, teleTupleObject);
        frame().getContentPane().add(fieldsPane);
    }

    @Override
    protected boolean refreshView(boolean force) {
        if (isShowing() || force) {
            fieldsPane.refresh(force);
            if (classMethodInspectorMenuItems != null) {
                classMethodInspectorMenuItems.refresh(force);
            }
            if (targetMethodInspectorMenuItems != null) {
                targetMethodInspectorMenuItems.refresh(force);
            }
            super.refreshView(force);
        }
        return true;
    }

}
