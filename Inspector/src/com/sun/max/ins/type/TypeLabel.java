/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.datatransfer.*;
import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

/**
 * A label specialized for displaying information about a {@TypeDescriptor}, even if not yet loaded in the VM.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public class TypeLabel extends InspectorLabel {

    private TypeDescriptor typeDescriptor;
    private TeleClassActor teleClassActor;

    private final class MyMouseClickAdapter extends InspectorMouseClickAdapter {

        public MyMouseClickAdapter(Inspection inspection) {
            super(inspection);
        }

        @Override
        public void procedure(MouseEvent mouseEvent) {
            switch (Inspection.mouseButtonWithModifiers(mouseEvent)) {
                case MouseEvent.BUTTON1: {
                    if (teleClassActor != null) {
                        if (mouseEvent.isControlDown()) {
                            actions().inspectObjectMemoryWords(teleClassActor).perform();
                        } else {
                            focus().setHeapObject(teleClassActor);
                        }
                        break;
                    }
                }
                case MouseEvent.BUTTON3: {
                    final InspectorPopupMenu menu = new InspectorPopupMenu();
                    final boolean enabled = teleClassActor != null;

                    final InspectorAction inspectActorAction = actions().inspectObject(teleClassActor, "Inspect ClassActor (Left-Button)");
                    inspectActorAction.setEnabled(enabled);
                    menu.add(inspectActorAction);

                    final InspectorAction inspectMemoryWordsAction = actions().inspectObjectMemoryWords(teleClassActor, "Inspect ClassActor memory words");
                    inspectMemoryWordsAction.setEnabled(enabled);
                    menu.add(inspectMemoryWordsAction);

                    menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    break;
                }
                default: {
                    break;
                }
            }
        }

    }

    public TypeLabel(final Inspection inspection) {
        this(inspection, null);
    }

    public TypeLabel(final Inspection inspection, TypeDescriptor typeDescriptor) {
        super(inspection);
        this.typeDescriptor = typeDescriptor;
        updateClassActor();
        addMouseListener(new MyMouseClickAdapter(inspection));
        redisplay();
    }

    /**
     * Changes the value to be displayed by the label.
     */
    public void setValue(TypeDescriptor typeDescriptor) {
        this.typeDescriptor = typeDescriptor;
        updateClassActor();
        updateText();
    }

    private void updateClassActor() {
        if (typeDescriptor == null) {
            teleClassActor = null;
        } else {
            // Might be null if class not yet known in VM
            teleClassActor = vm().classRegistry().findTeleClassActor(typeDescriptor);
        }
    }

    public void redisplay() {
        updateText();
        setFont(style().defaultFont());
    }

    private void updateText() {
        if (typeDescriptor == null) {
            setText("");
            setToolTipText("");
        } else {
            final Class javaType = typeDescriptor.resolveType(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER);
            setText(javaType.getSimpleName());
            if (teleClassActor == null) {
                setForeground(style().javaUnresolvedNameColor());
                setToolTipText("<unloaded>" +  javaType.getName());
            } else {
                setToolTipText(inspection().nameDisplay().referenceToolTipText(teleClassActor));
            }
        }
    }

    private MaxVMState lastRefreshedState = null;

    public void refresh(boolean force) {
        if (vm().state().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = vm().state();
            updateClassActor();
        }
    }

    @Override
    public Transferable getTransferable() {
        if (teleClassActor != null) {
            return new InspectorTransferable.TeleObjectTransferable(inspection(), teleClassActor);
        }
        return null;
    }
}
