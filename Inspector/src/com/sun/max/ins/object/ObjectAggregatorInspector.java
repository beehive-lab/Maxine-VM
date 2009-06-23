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

    private final ObjectAggregator objectAggregator;
    private int start;
    private int end;
    private final  JPanel contentPane;

    private JComponent createController() {
        final JPanel controller = new InspectorPanel(inspection(), new SpringLayout());

        controller.add(new TextLabel(inspection(), "start:"));
        final AddressInputField.Hex startField = new AddressInputField.Hex(inspection(), Address.fromInt(start)) {
            @Override
            public void update(Address newStart) {
                if (!newStart.equals(start)) {
                    start = newStart.toInt();
                    if (end < start) {
                        start = end;
                    }
                    updateView();
                }
            }
        };
        startField.setRange(0, objectAggregator.count() - 1);
        controller.add(startField);

        controller.add(new TextLabel(inspection(), "end:"));
        final AddressInputField.Decimal endField = new AddressInputField.Decimal(inspection(), Address.fromInt(end)) {
            @Override
            public void update(Address newEnd) {
                if (!newEnd.equals(end)) {
                    end = newEnd.toInt();
                    if (end < start) {
                        end = start;
                    }
                    updateView();
                }
            }
        };
        endField.setRange(0, objectAggregator.count() - 1);
        controller.add(endField);

        SpringUtilities.makeCompactGrid(controller, 4);
        return controller;
    }

    private WordValueLabel[] referenceLabels;

    @Override
    protected void refreshView(boolean force) {
        for (WordValueLabel wordValueLabel : referenceLabels) {
            wordValueLabel.refresh(force);
        }
        super.refreshView(force);
    }

    public void viewConfigurationChanged() {
        for (WordValueLabel wordValueLabel : referenceLabels) {
            wordValueLabel.redisplay();
        }
    }

    @Override
    public String getTextForTitle() {
        return "Aggregator for " + objectAggregator.type();
    }

    @Override
    protected void createView() {
        frame().setContentPane(contentPane);
        contentPane.removeAll();
        contentPane.setLayout(new BorderLayout());

        contentPane.add(new TextLabel(inspection(), objectAggregator.count() + " instances occupying " + objectAggregator.size().toLong() + " bytes"), BorderLayout.NORTH);

        final JPanel view = new InspectorPanel(inspection(), new SpringLayout());

        final Iterator<Reference> iterator = objectAggregator.instances(maxVM());

        int index = 0;
        while (index != start) {
            iterator.next();
            ++index;
        }

        referenceLabels = new WordValueLabel[(end + 1) - start];
        int i = 0;
        while (index <= end) {
            final WordValueLabel referenceLabel = new WordValueLabel(inspection(), WordValueLabel.ValueMode.REFERENCE, iterator.next().toOrigin(), null);
            referenceLabels[i++] = referenceLabel;
            view.add(referenceLabel);
            ++index;
        }

        SpringUtilities.makeCompactGrid(view, 1);

        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), view);
        final Dimension dim = scrollPane.getPreferredSize();
        scrollPane.setPreferredSize(new Dimension(Math.min(200, dim.width), Math.min(400, dim.height)));
        contentPane.add(scrollPane, BorderLayout.CENTER);

        contentPane.add(createController(), BorderLayout.SOUTH);
    }

    private ObjectAggregatorInspector(Inspection inspection, ObjectAggregator objectAggregator) {
        super(inspection, ReferenceValue.from(objectAggregator.type()));
        this.objectAggregator = objectAggregator;
        this.end = Math.min(100, objectAggregator.count() - 1);
        contentPane = new InspectorPanel(inspection);
        createFrame(null);
        inspection.gui().moveToMiddle(this);
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
