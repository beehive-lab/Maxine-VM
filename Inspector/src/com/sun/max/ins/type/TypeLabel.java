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

import java.awt.datatransfer.*;
import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.prototype.*;
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
            teleClassActor = maxVM().findTeleClassActor(typeDescriptor);
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
        if (vmState().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = vmState();
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
