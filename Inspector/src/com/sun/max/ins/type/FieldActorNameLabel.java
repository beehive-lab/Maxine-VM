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

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.vm.actor.member.*;

/**
 * A label specialized for displaying information about a {@FieldActor}.
 *
 * @author Michael Van De Vanter
 */

public class FieldActorNameLabel extends InspectorLabel {

    private FieldActor fieldActor;

    public FieldActorNameLabel(Inspection inspection, FieldActor fieldActor) {
        super(inspection);
        this.fieldActor = fieldActor;
        redisplay();
    }

    public FieldActorNameLabel(Inspection inspection) {
        this(inspection, null);
    }

    public void setValue(FieldActor fieldActor) {
        this.fieldActor = fieldActor;
        updateText();
    }

    public void refresh(boolean force) {
        // local fieldActor state is assumed not to change.
    }

    public void redisplay() {
        setFont(style().javaNameFont());
        setForeground(style().javaNameColor());
        setBackground(style().javaNameBackgroundColor());
        updateText();

    }

    private void updateText() {
        if (fieldActor == null) {
            setText("");
            setToolTipText("");
        } else {
            setText(fieldActor.name.toString());
            setToolTipText("Type: " + fieldActor.descriptor().toJavaString(true) + " in " + fieldActor.holder().qualifiedName());
        }
    }

}
