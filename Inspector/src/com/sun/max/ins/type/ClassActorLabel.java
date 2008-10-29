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
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;

/**
 * A label specialized for displaying information about a {@ClassActor} in the {@link TeleVM}.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class ClassActorLabel extends InspectorLabel {

    private TypeDescriptor _typeDescriptor;

    // VM epoch as of the last time data was read.
    private long _epoch = -1;

    private final InspectorMouseClickAdapter _inspectorMouseClickAdapter;

    private final class MyMouseClickAdapter extends InspectorMouseClickAdapter {

        public MyMouseClickAdapter(Inspection inspection) {
            super(inspection);
        }

        @Override
        public void procedure(MouseEvent mouseEvent) {
            switch (MaxineInspector.mouseButtonWithModifiers(mouseEvent)) {
                case MouseEvent.BUTTON1: {
                    if (mouseEvent.isControlDown()) {
                        MemoryInspector.create(inspection(), getTeleClassActor());
                    } else {
                        inspection().focus().setHeapObject(getTeleClassActor());
                    }
                    break;
                }
                case MouseEvent.BUTTON3: {
                    final InspectorMenu menu = new InspectorMenu();
                    menu.add(inspection().inspectionMenus().getInspectObjectAction(getTeleClassActor(), "Inspect ClassActor (Left-Button)"));
                    menu.add(inspection().inspectionMenus().getInspectMemoryAction(getTeleClassActor(), "Inspect ClassActor memory"));
                    menu.add(inspection().inspectionMenus().getInspectMemoryWordsAction(getTeleClassActor(), "Inspect ClassActor memory words"));
                    menu.popupMenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    break;
                }
                default: {
                    break;
                }
            }
        }

    }

    public ClassActorLabel(final Inspection inspection, TypeDescriptor typeDescriptor) {
        super(inspection);
        _typeDescriptor = typeDescriptor;
        _inspectorMouseClickAdapter = new MyMouseClickAdapter(inspection);
        redisplay();
    }

    public void refresh(long epoch, boolean force) {
        if (epoch > _epoch || force) {
            updateText();
            _epoch = epoch;
        }
    }

    public void redisplay() {
        setFont(style().javaClassNameFont());
        setForeground(style().javaNameColor());
        setBackground(style().javaNameBackgroundColor());
        refresh(teleVM().teleProcess().epoch(), true);
    }

    private TeleClassActor _teleClassActor;

    private TeleClassActor getTeleClassActor() {
        getTeleClassActorOrNull();
        InspectorError.check(_teleClassActor != null, "failed to find ClassActorReference for \"" + _typeDescriptor.toString() + "\"");
        return _teleClassActor;
    }

    /**
     * Fetches local surrogate for the {@link ClassActor} in the tele VM associated with this label,
     * or set it to null if the class is not loaded in the inspected virtual machine.
     * @return a tele class actor, or null if the class is not loaded.
     */
    private TeleClassActor getTeleClassActorOrNull() {
        if (_teleClassActor == null) {
            _teleClassActor = teleVM().teleClassRegistry().findTeleClassActorByType(_typeDescriptor);
        }
        return _teleClassActor;
    }

    private void updateText() {
        if (_typeDescriptor == null) {
            //_typeDescriptor = teleVM().referenceToClassActor(getClassActorReference()).typeDescriptor();
            _typeDescriptor = getTeleClassActor().classActor().typeDescriptor();
        }
        final Class javaType = _typeDescriptor.toJava(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER);
        final TeleClassActor teleClassActor = getTeleClassActorOrNull();
        String prefix = null;
        if (teleClassActor == null) {
            prefix = "<unloaded>";
            setForeground(style().javaUnresolvedNameColor());
        }
        setText(javaType.getSimpleName());
        setToolTipText(inspection().nameDisplay().longName(prefix, teleClassActor, "ClassActor", javaType.getName()));
        if (getTeleClassActorOrNull() != null) {
            addMouseListener(_inspectorMouseClickAdapter);
        }
    }


}
