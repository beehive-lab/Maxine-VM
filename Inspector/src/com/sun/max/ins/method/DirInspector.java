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
package com.sun.max.ins.method;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.lang.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.reference.*;

public final class DirInspector extends IrInspector<DirInspector> {

    private final DirMethod _dirMethod;

    private InspectorMenuItems _classMethodInspectorMenuItems;

    private DirPanel _dirPanel;

    @Override
    protected void createView(long epoch) {
        _classMethodInspectorMenuItems = new ClassMethodMenuItems(inspection(), teleClassMethodActor());
        frame().add(_classMethodInspectorMenuItems);

        _dirPanel = new DirPanel(inspection(), _dirMethod);

        frame().setContentPane(new JScrollPane(_dirPanel));
    }

    private DirInspector(Inspection inspection, Residence residence, Reference dirMethodReference) {
        super(inspection, residence, dirMethodReference);
        final TeleObject teleDirMethod = teleVM().makeTeleObject(dirMethodReference);
        _dirMethod = teleDirMethod == null ? null : (DirMethod) teleDirMethod.deepCopy();
        createFrame(null);
        setLocationRelativeToMouse();
    }

    public static DirInspector make(Inspection inspection, Reference dirMethodReference) {
        final UniqueInspector.Key<DirInspector> key = UniqueInspector.Key.create(inspection, DirInspector.class, dirMethodReference);
        DirInspector dirInspector = UniqueInspector.find(inspection, key);
        if (dirInspector == null) {
            dirInspector = new DirInspector(inspection, Residence.INTERNAL, dirMethodReference);
        }
        dirInspector.highlight();
        return dirInspector;
    }

    @Override
    public String getTextForTitle() {
        return _dirMethod.name() + "(" + Arrays.toString(_dirMethod.parameters(), ", ") + ")";
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        super.refreshView(epoch, force);
        _classMethodInspectorMenuItems.refresh(epoch, force);
    }

    public void viewConfigurationChanged(long epoch) {
        _dirPanel.redisplay();
    }
}
