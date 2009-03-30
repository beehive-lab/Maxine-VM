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
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.layout.*;

/**
 * An object inspector specialized for displaying a Maxine low-level object in the {@link TeleVM}, constructed using {@link ArrayLayout}.
 *
 * @author Michael Van De Vanter
 */
public final class ArrayInspector extends ObjectInspector {

    private ObjectPane _elementsPane;

    ArrayInspector(Inspection inspection, ObjectInspectorFactory factory, TeleObject teleObject) {
        super(inspection, factory, teleObject);
        createFrame(null);
    }

    @Override
    protected synchronized void createView(long epoch) {
        super.createView(epoch);
        final TeleArrayObject teleArrayObject = (TeleArrayObject) teleObject();
        _elementsPane = ObjectPane.createArrayElementsPane(this, teleArrayObject);
        frame().getContentPane().add(_elementsPane);
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        _elementsPane.refresh(epoch, force);
        super.refreshView(epoch, force);
    }

}
