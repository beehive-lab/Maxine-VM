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

import java.awt.*;
import java.util.*;

import javax.swing.*;

import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.value.*;

/**
 * An inspector that lists (a subset of) the objects in a {@link ObjectAggregator}.
 *
 * @author Doug Simon
 */
public final class ObjectAggregatorInspector extends UniqueInspector<ObjectAggregatorInspector> {

    private final ObjectAggregator _objectAggregator;
    private int _start;
    private int _end;
    private final  JPanel _contentPane;

    private JComponent createController() {
        final JPanel controller = new InspectorPanel(inspection(), new SpringLayout());

        controller.add(new TextLabel(inspection(), "start:"));
        final AddressInputField.Hex startField = new AddressInputField.Hex(inspection(), Address.fromInt(_start)) {
            @Override
            public void update(Address start) {
                if (!start.equals(_start)) {
                    _start = start.toInt();
                    if (_end < _start) {
                        _start = _end;
                    }
                    updateView();
                }
            }
        };
        startField.setRange(0, _objectAggregator.count() - 1);
        controller.add(startField);

        controller.add(new TextLabel(inspection(), "end:"));
        final AddressInputField.Decimal endField = new AddressInputField.Decimal(inspection(), Address.fromInt(_end)) {
            @Override
            public void update(Address end) {
                if (!end.equals(_end)) {
                    _end = end.toInt();
                    if (_end < _start) {
                        _end = _start;
                    }
                    updateView();
                }
            }
        };
        endField.setRange(0, _objectAggregator.count() - 1);
        controller.add(endField);

        SpringUtilities.makeCompactGrid(controller, 4);
        return controller;
    }

    private WordValueLabel[] _referenceLabels;

    @Override
    protected synchronized void refreshView(long epoch, boolean force) {
        for (WordValueLabel wordValueLabel : _referenceLabels) {
            wordValueLabel.refresh(epoch, force);
        }
        super.refreshView(epoch, force);
    }

    public void viewConfigurationChanged(long epoch) {
        for (WordValueLabel wordValueLabel : _referenceLabels) {
            wordValueLabel.redisplay();
        }
    }

    @Override
    public String getTextForTitle() {
        return "Aggregator for " + _objectAggregator.type();
    }

    @Override
    protected synchronized void createView(long epoch) {
        frame().setContentPane(_contentPane);
        _contentPane.removeAll();
        _contentPane.setLayout(new BorderLayout());

        _contentPane.add(new TextLabel(inspection(), _objectAggregator.count() + " instances occupying " + _objectAggregator.size().toLong() + " bytes"), BorderLayout.NORTH);

        final JPanel view = new InspectorPanel(inspection(), new SpringLayout());

        final Iterator<Reference> iterator = _objectAggregator.instances(vm());

        int index = 0;
        while (index != _start) {
            iterator.next();
            ++index;
        }

        _referenceLabels = new WordValueLabel[(_end + 1) - _start];
        int i = 0;
        while (index <= _end) {
            final WordValueLabel referenceLabel = new WordValueLabel(inspection(), WordValueLabel.ValueMode.REFERENCE, iterator.next().toOrigin());
            _referenceLabels[i++] = referenceLabel;
            view.add(referenceLabel);
            ++index;
        }

        SpringUtilities.makeCompactGrid(view, 1);

        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), view);
        final Dimension dim = scrollPane.getPreferredSize();
        scrollPane.setPreferredSize(new Dimension(Math.min(200, dim.width), Math.min(400, dim.height)));
        _contentPane.add(scrollPane, BorderLayout.CENTER);

        _contentPane.add(createController(), BorderLayout.SOUTH);
    }

    private ObjectAggregatorInspector(Inspection inspection, ObjectAggregator objectAggregator) {
        super(inspection, ReferenceValue.from(objectAggregator.type()));
        _objectAggregator = objectAggregator;
        _end = Math.min(100, _objectAggregator.count() - 1);
        _contentPane = new InspectorPanel(inspection);
        createFrame(null);
        frame().moveToMiddle();
    }

    /**
     * Displays an inspector for an ObjectAggregator.
     * @return the possibly new inspector
     */
    public static ObjectAggregatorInspector make(Inspection inspection, ObjectAggregator objectAggregator) {
        final UniqueInspector.Key<? extends ObjectAggregatorInspector> key = UniqueInspector.Key.create(ObjectAggregatorInspector.class, ReferenceValue.from(objectAggregator.type()));
        ObjectAggregatorInspector objectAggregatorInspector = UniqueInspector.find(inspection, key);
        if (objectAggregatorInspector == null) {
            objectAggregatorInspector = new ObjectAggregatorInspector(inspection, objectAggregator);
        }
        return objectAggregatorInspector;
    }

}
