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
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.type.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A panel that displays the header information of a Maxine low level heap object.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
class ObjectHeaderInspector extends InspectorPanel {

    private final AppendableSequence<InspectorLabel> _labels = new ArrayListSequence<InspectorLabel>(20);
    private final TeleObject _teleObject;
    private final ObjectInspector _parent;

    ObjectHeaderInspector(final Inspection inspection, final TeleObject teleObject, ObjectInspector parent, AppendableSequence<ValueLabel> valueLabels) {
        super(inspection, new SpringLayout());
        _teleObject = teleObject;
        _parent = parent;

        setOpaque(true);
        setBackground(style().defaultBackgroundColor());

        final Address origin = teleObject.getCurrentOrigin();

        // First line:  pointer to hub
        final TeleHub teleHub = teleObject.getTeleHub();

        final int hubReferenceOffset = teleVM().layoutScheme().generalLayout().getOffsetFromOrigin(HeaderField.HUB).toInt();

        if (_parent.showAddresses()) {
            addLabel(new LocationLabel.AsAddressWithOffset(inspection, hubReferenceOffset, origin));
        }
        if (_parent.showOffsets()) {
            addLabel(new LocationLabel.AsOffset(inspection, hubReferenceOffset, origin));
        }
        if (_parent.showType()) {
            addLabel(new ClassActorLabel(inspection(), JavaTypeDescriptor.forJavaClass(teleHub.hub().getClass())));
        }
        addLabel(new TextLabel(inspection(), "hub"));
        addLabel(new WordValueLabel(inspection(), ValueMode.REFERENCE, teleHub.getCurrentOrigin()));
        if (_parent.showMemoryRegion()) {
            final ValueLabel memoryRegionValueLabel = new MemoryRegionValueLabel(inspection()) {
                @Override
                public Value fetchValue() {
                    final TeleHub hub = _teleObject.getTeleHub();
                    return WordValue.from(hub.getCurrentOrigin().asWord());
                }
            };
            addLabel(memoryRegionValueLabel);
            valueLabels.append(memoryRegionValueLabel);
        }

        // Second line:  "misc" word
        final int miscWordOffset = teleVM().layoutScheme().generalLayout().getOffsetFromOrigin(HeaderField.MISC).toInt();
        if (_parent.showAddresses()) {
            addLabel(new LocationLabel.AsAddressWithOffset(inspection(), miscWordOffset, origin));
        }
        if (_parent.showOffsets()) {
            addLabel(new LocationLabel.AsOffset(inspection(), miscWordOffset, origin));
        }
        if (_parent.showType()) {
            addLabel(new ClassActorLabel(inspection(), JavaTypeDescriptor.WORD));
        }
        addLabel(new TextLabel(inspection(), "misc"));
        final ValueLabel miscValueLabel = new MiscWordLabel(inspection(), teleObject);
        addLabel(miscValueLabel);
        valueLabels.append(miscValueLabel);
        if (_parent.showMemoryRegion()) {
            addLabel(new PlainLabel(inspection(), ""));
        }

        // Third line:  array information
        final int arrayLengthOffset = teleVM().layoutScheme().arrayHeaderLayout().getOffsetFromOrigin(HeaderField.LENGTH).toInt();
        if (teleObject instanceof TeleArrayObject) {
            final TeleArrayObject teleArrayObject = (TeleArrayObject) teleObject;
            if (_parent.showAddresses()) {
                addLabel(new LocationLabel.AsAddressWithOffset(inspection(), arrayLengthOffset, origin));
            }
            if (_parent.showOffsets()) {
                addLabel(new LocationLabel.AsOffset(inspection(), arrayLengthOffset, origin));
            }
            if (_parent.showType()) {
                addLabel(new ClassActorLabel(inspection(), JavaTypeDescriptor.INT));
            }
            addLabel(new TextLabel(inspection(), "length"));
            // Assume length never changes
            addLabel(new DataLabel.IntAsDecimal(inspection(), teleArrayObject.getLength()));
            if (_parent.showMemoryRegion()) {
                addLabel(new PlainLabel(inspection(), ""));
            }
        } else if (teleObject instanceof TeleHybridObject) {
            final TeleHybridObject teleHybridObject = (TeleHybridObject) teleObject;
            if (_parent.showAddresses()) {
                addLabel(new LocationLabel.AsAddressWithOffset(inspection(), arrayLengthOffset, origin));
            }
            if (_parent.showOffsets()) {
                addLabel(new LocationLabel.AsOffset(inspection(), arrayLengthOffset, origin));
            }
            if (_parent.showType()) {
                addLabel(new ClassActorLabel(inspection(), JavaTypeDescriptor.INT));
            }
            addLabel(new TextLabel(inspection(), "length"));
            // Assume length never changes
            addLabel(new DataLabel.IntAsDecimal(inspection(), teleHybridObject.readArrayLength()));
            if (_parent.showMemoryRegion()) {
                addLabel(new PlainLabel(inspection(), ""));
            }
        }
        final int columns = parent.numberOfTupleColumns();
        SpringUtilities.makeCompactGrid(this, getComponentCount() / columns, columns, 0, 0, 10, 2);
    }

    public void refresh(long epoch, boolean force) {
        // Handled by parent via {@link ObjectInspector#valueLabels()}.
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

}
