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

import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;

/**
 * A label specialized for displaying information about a {@TypeDescriptor}, even if not yet loaded in the {@link TeleVM}.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public class TypeLabel extends InspectorLabel {

    private TypeDescriptor _typeDescriptor;
    private TeleClassActor _teleClassActor;

    private final class MyMouseClickAdapter extends InspectorMouseClickAdapter {

        public MyMouseClickAdapter(Inspection inspection) {
            super(inspection);
        }

        @Override
        public void procedure(MouseEvent mouseEvent) {
            switch (MaxineInspector.mouseButtonWithModifiers(mouseEvent)) {
                case MouseEvent.BUTTON1: {
                    if (_teleClassActor != null) {
                        if (mouseEvent.isControlDown()) {
                            MemoryInspector.create(inspection(), _teleClassActor).highlight();
                        } else {
                            inspection().focus().setHeapObject(_teleClassActor);
                        }
                        break;
                    }
                }
                case MouseEvent.BUTTON3: {
                    final InspectorMenu menu = new InspectorMenu();
                    final boolean enabled = _teleClassActor != null;

                    final InspectorAction inspectActorAction = inspection().actions().inspectObject(_teleClassActor, "Inspect ClassActor (Left-Button)");
                    inspectActorAction.setEnabled(enabled);
                    menu.add(inspectActorAction);

                    final InspectorAction inspectMemoryAction = inspection().actions().inspectMemory(_teleClassActor, "Inspect ClassActor memory");
                    inspectMemoryAction.setEnabled(enabled);
                    menu.add(inspectMemoryAction);

                    final InspectorAction inspectMemoryWordsAction = inspection().actions().inspectMemoryWords(_teleClassActor, "Inspect ClassActor memory words");
                    inspectMemoryWordsAction.setEnabled(enabled);
                    menu.add(inspectMemoryWordsAction);

                    menu.popupMenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
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
        _typeDescriptor = typeDescriptor;
        updateClassActor();
        addMouseListener(new MyMouseClickAdapter(inspection));
        redisplay();
    }

    /**
     * Changes the value to be displayed by the label.
     */
    public void setValue(TypeDescriptor typeDescriptor) {
        _typeDescriptor = typeDescriptor;
        updateClassActor();
        updateText();
    }

    private void updateClassActor() {
        if (_typeDescriptor == null) {
            _teleClassActor = null;
        } else {
            // Might be null if class not yet known in VM
            _teleClassActor = teleVM().findTeleClassActorByType(_typeDescriptor);
        }
    }

    public void redisplay() {
        setFont(style().javaClassNameFont());
        setBackground(style().javaNameBackgroundColor());
        updateText();
    }

    private void updateText() {
        if (_typeDescriptor == null) {
            setText("");
            setToolTipText("");
            setForeground(style().javaNameColor());
        } else {
            final Class javaType = _typeDescriptor.resolveType(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER);
            setText(javaType.getSimpleName());
            if (_teleClassActor == null) {
                setForeground(style().javaUnresolvedNameColor());
                setToolTipText("<unloaded>" +  javaType.getName());
            } else {
                setForeground(style().javaNameColor());
                setToolTipText(inspection().nameDisplay().referenceToolTipText(_teleClassActor));
            }
        }
    }

    private long _epoch = -1;

    @Override
    public void refresh(long epoch, boolean force) {
        if (epoch > _epoch || force) {
            updateClassActor();
            _epoch = epoch;
        }
    }

}
