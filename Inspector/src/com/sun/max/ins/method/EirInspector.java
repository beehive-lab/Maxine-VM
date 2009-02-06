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

import com.sun.max.ins.*;
import com.sun.max.ins.amd64.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.reference.*;

public final class EirInspector extends IrInspector<EirInspector> {

    private final EirMethod _eirMethod;
    private final EirPanel _eirPanel;

    private InspectorMenuItems _classMethodInspectorMenuItems;

    private EirInspector(Inspection inspection, Residence residence, Reference eirMethodReference) {
        super(inspection, residence, eirMethodReference);
        final TeleObject teleEirMethod = teleVM().makeTeleObject(eirMethodReference);
        _eirMethod = teleEirMethod == null ? null : (EirMethod) teleEirMethod.deepCopy();
        if (!EirMethod.class.isInstance(_eirMethod)) {
            throw new InspectorError("unsupported EIR");
        }
        createFrame(null);
        _eirPanel = new AMD64EirPanel(inspection(), _eirMethod);
        setLocationRelativeToMouse();
    }

    public static EirInspector make(Inspection inspection, Reference eirMethodReference) {
        final UniqueInspector.Key<EirInspector> key = UniqueInspector.Key.create(inspection, EirInspector.class, eirMethodReference);
        EirInspector eirInspector = UniqueInspector.find(inspection, key);
        if (eirInspector == null) {
            eirInspector = new EirInspector(inspection, Residence.INTERNAL, eirMethodReference);
        }
        eirInspector.highlight();
        return eirInspector;
    }

    @Override
    public String getTextForTitle() {
        return _eirPanel.createTitle();
    }

    @Override
    protected void createView(long epoch) {
        _classMethodInspectorMenuItems = new ClassMethodMenuItems(inspection(), teleClassMethodActor());
        frame().add(_classMethodInspectorMenuItems);
        frame().setContentPane(new InspectorScrollPane(inspection(), _eirPanel));
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        super.refreshView(epoch, force);
        _classMethodInspectorMenuItems.refresh(epoch, force);
    }

    public void viewConfigurationChanged(long epoch) {
        _eirPanel.redisplay();
    }

}
