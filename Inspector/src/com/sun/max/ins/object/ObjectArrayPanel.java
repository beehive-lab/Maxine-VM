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
import com.sun.max.ins.value.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A panel that displays array members in a Maxine low level heap object (in arrays or hybrids) as a list of rows.
 *
 * @author Michael Van De Vanter
 */
@Deprecated
public class ObjectArrayPanel extends InspectorPanel {

    private final AppendableSequence<InspectorLabel> _labels = new ArrayListSequence<InspectorLabel>(20);

    ObjectArrayPanel(final ObjectInspector objectInspector, final Kind kind, int startOffset, int startIndex, int length, String indexPrefix, WordValueLabel.ValueMode wordValueMode) {
        super(objectInspector.inspection(), new SpringLayout());
        setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, style().defaultBorderColor()));
        setOpaque(true);
        setBackground(style().defaultBackgroundColor());

        final TeleObject teleObject = objectInspector.teleObject();
        final Pointer objectOrigin = teleObject.getCurrentOrigin();
        final TeleReference objectReference = teleObject.reference();
        final int size = kind.size();

        for (int i = 0; i < length; i++) {
            final int index = startIndex + i;
            if (!objectInspector.hideNullArrayElements() || !teleVM().getElementValue(kind, objectReference, index).isZero()) {
                if (objectInspector.showAddresses()) {
                    addLabel(new LocationLabel.AsAddressWithOffset(inspection(), startOffset + (i * size), objectOrigin));    // Array member address
                }
                if (objectInspector.showOffsets()) {
                    addLabel(new LocationLabel.AsOffset(inspection(), startOffset + (i * size), objectOrigin));                         // Array member position
                }
                addLabel(new LocationLabel.AsIndex(inspection(), indexPrefix, i, startOffset + (i * size), objectOrigin));      // Array member name

                ValueLabel valueLabel;
                if (kind == Kind.REFERENCE) {
                    valueLabel = new WordValueLabel(inspection(), WordValueLabel.ValueMode.REFERENCE) {
                        @Override
                        public Value fetchValue() {
                            return teleVM().getElementValue(kind, objectReference, index);
                        }
                    };
                } else if (kind == Kind.WORD) {
                    valueLabel = new WordValueLabel(inspection(), wordValueMode) {
                        @Override
                        public Value fetchValue() {
                            return teleVM().getElementValue(kind, objectReference, index);
                        }
                    };
                } else {
                    valueLabel = new PrimitiveValueLabel(inspection(), kind) {
                        @Override
                        public Value fetchValue() {
                            return teleVM().getElementValue(kind, objectReference, index);
                        }
                    };
                }
                addLabel(valueLabel);                                                                                                   // Array member value

                if (objectInspector.showMemoryRegions()) {
                    addLabel(new MemoryRegionValueLabel(inspection()) {
                        @Override
                        public Value fetchValue() {
                            return teleVM().getElementValue(kind, objectReference, index);
                        }
                    });
                }
            }
        }
        SpringUtilities.makeCompactGrid(this, objectInspector.numberOfArrayColumns());
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
