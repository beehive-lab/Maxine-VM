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

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.gui.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.type.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;


/**
 * A panel that displays fields in a Maxine low level heap object (in tuples or hybrids).
 *
 * @author Michael Van De Vanter
 */
public class ObjectFieldsPanel extends InspectorPanel {

    private final ObjectInspector _objectInspector;
    private final TeleObject _teleObject;
    private final Iterable<FieldActor> _fieldActors;
    private final AppendableSequence<InspectorLabel> _labels = new ArrayListSequence<InspectorLabel>(20);

    ObjectFieldsPanel(ObjectInspector objectInspector, Iterable<FieldActor> fieldActors, TeleObject teleObject) {
        super(objectInspector.inspection(), new SpringLayout());
        _objectInspector = objectInspector;
        _teleObject = teleObject;
        _fieldActors = fieldActors;
        setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, style().defaultBorderColor()));
        setOpaque(true);
        setBackground(style().defaultBackgroundColor());

        final Pointer objectOrigin = _teleObject.getCurrentOrigin();
        final boolean isTeleActor = _teleObject instanceof TeleActor;

        for (final FieldActor fieldActor : _fieldActors) {
            if (_objectInspector.showAddresses()) {
                addLabel(new LocationLabel.AsAddressWithOffset(inspection(), fieldActor.offset(), objectOrigin));  // Field address
            }
            if (_objectInspector.showOffsets()) {
                addLabel(new LocationLabel.AsOffset(inspection(), fieldActor.offset(), objectOrigin));                           // Field position
            }
            if (_objectInspector.showTypes()) {
                addLabel(new ClassActorLabel(inspection(), fieldActor.descriptor()));                                                              // Field type
            }
            addLabel(new FieldActorLabel(inspection(), fieldActor));                                                                                     // Field name

            ValueLabel valueLabel;
            if (fieldActor.kind() == Kind.REFERENCE) {
                valueLabel = new WordValueLabel(inspection(), WordValueLabel.ValueMode.REFERENCE) {
                    @Override
                    public Value fetchValue() {
                        return _teleObject.readFieldValue(fieldActor);
                    }
                };
            } else if (fieldActor.kind() == Kind.WORD) {
                valueLabel = new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD) {
                    @Override
                    public Value fetchValue() {
                        return _teleObject.readFieldValue(fieldActor);
                    }
                };
            } else if (isTeleActor && fieldActor.name().toString().equals("_flags")) {
                final TeleActor teleActor = (TeleActor) _teleObject;
                valueLabel = new ActorFlagsValueLabel(inspection(), teleActor);
            } else {
                valueLabel = new PrimitiveValueLabel(inspection(), fieldActor.kind()) {
                    @Override
                    public Value fetchValue() {
                        return _teleObject.readFieldValue(fieldActor);
                    }
                };
            }
            addLabel(valueLabel);                                                                                                                                     // Field value

            if (_objectInspector.showMemoryRegions()) {
                addLabel(new MemoryRegionValueLabel(inspection()) {
                    @Override
                    public Value fetchValue() {
                        return _teleObject.readFieldValue(fieldActor);
                    }
                });                                                                                                                 // memory region
            }
        }
        final int columnCount = _objectInspector.numberOfTupleColumns();
        SpringUtilities.makeCompactGrid(this, getComponentCount() / columnCount, columnCount, 0, 0, 0, 0);
    }

    private void addLabel(InspectorLabel inspectorLabel) {
        add(inspectorLabel);
        _labels.append(inspectorLabel);
    }

    public void redisplay() {
        setOpaque(true);
        setBackground(style().defaultBackgroundColor());
        for (InspectorLabel inspectorLabel : _labels) {
            inspectorLabel.redisplay();
        }
    }

    public void refresh(long epoch, boolean force) {
        for (InspectorLabel inspectorLabel : _labels) {
            inspectorLabel.refresh(epoch, force);
        }
    }

}
