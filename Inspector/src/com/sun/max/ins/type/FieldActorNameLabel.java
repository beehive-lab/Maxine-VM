/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
