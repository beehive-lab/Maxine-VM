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

/**
 * A panel that displays the head information of a Maxine low level object.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
class ObjectHeaderInspector extends InspectorPanel {

    // first line: pointer to hub
    private final InspectorLabel _hubAddressLabel;
    private final InspectorLabel _hubOffsetLabel;
    private final InspectorLabel _hubTypeLabel;
    private final InspectorLabel _hubNameLabel;
    private final InspectorLabel _hubValueLabel;

    // Second line:  "misc" word
    private final InspectorLabel _miscAddressLabel;
    private final InspectorLabel _miscOffsetLabel;
    private final InspectorLabel _miscTypeLabel;
    private final InspectorLabel _miscNameLabel;
    private final InspectorLabel _miscValueLabel;

    // Third line:  array information
    private final InspectorLabel _arrayLengthAddressLabel;
    private final InspectorLabel _arrayLengthOffsetLabel;
    private final InspectorLabel _arrayLengthTypeLabel;
    private final InspectorLabel _arrayLengthNameLabel;
    private final InspectorLabel _arrayLengthValueLabel;

    private final TeleObject _teleObject;
    private final ObjectInspector _parent;

    ObjectHeaderInspector(final Inspection inspection, final TeleObject teleObject, ObjectInspector parent) {
        super(inspection, new SpringLayout());
        _teleObject = teleObject;
        _parent = parent;

        setOpaque(true);
        setBackground(style().defaultBackgroundColor());

        final Address origin = teleObject.getCurrentOrigin();

        // First line:  pointer to hub
        final TeleHub teleHub = teleObject.getTeleHub();

        final int hubReferenceOffset = teleVM().layoutScheme().generalLayout().getOffsetFromOrigin(HeaderField.HUB).toInt();
        _hubAddressLabel = new LocationLabel.AsAddressWithOffset(inspection, hubReferenceOffset, origin);
        if (_parent.showAddresses()) {
            add(_hubAddressLabel);
        }
        _hubOffsetLabel = new LocationLabel.AsOffset(inspection, hubReferenceOffset, origin);
        if (_parent.showOffsets()) {
            add(_hubOffsetLabel);
        }
        _hubTypeLabel = new ClassActorLabel(inspection(), JavaTypeDescriptor.forJavaClass(teleHub.hub().getClass()));
        if (_parent.showType()) {
            add(_hubTypeLabel);
        }
        _hubNameLabel = new TextLabel(inspection(), "hub");
        add(_hubNameLabel);
        _hubValueLabel = new WordValueLabel(inspection(), ValueMode.REFERENCE, teleHub.getCurrentOrigin());
        add(_hubValueLabel);

        // Second line:  "misc" word
        final int miscWordOffset = teleVM().layoutScheme().generalLayout().getOffsetFromOrigin(HeaderField.MISC).toInt();
        _miscAddressLabel = new LocationLabel.AsAddressWithOffset(inspection(), miscWordOffset, origin);
        if (_parent.showAddresses()) {
            add(_miscAddressLabel);
        }
        _miscOffsetLabel = new LocationLabel.AsOffset(inspection(), miscWordOffset, origin);
        if (_parent.showOffsets()) {
            add(_miscOffsetLabel);
        }
        _miscTypeLabel = new ClassActorLabel(inspection(), JavaTypeDescriptor.WORD);
        if (_parent.showType()) {
            add(_miscTypeLabel);
        }
        _miscNameLabel = new TextLabel(inspection(), "misc");
        add(_miscNameLabel);
        _miscValueLabel = new MiscWordLabel(inspection(), teleObject);
        add(_miscValueLabel);

        // Third line:  array information
        final int arrayLengthOffset = teleVM().layoutScheme().arrayHeaderLayout().getOffsetFromOrigin(HeaderField.LENGTH).toInt();
        if (teleObject instanceof TeleArrayObject) {
            final TeleArrayObject teleArrayObject = (TeleArrayObject) teleObject;
            _arrayLengthAddressLabel = new LocationLabel.AsAddressWithOffset(inspection(), arrayLengthOffset, origin);
            if (_parent.showAddresses()) {
                add(_arrayLengthAddressLabel);
            }
            _arrayLengthOffsetLabel = new LocationLabel.AsOffset(inspection(), arrayLengthOffset, origin);
            if (_parent.showOffsets()) {
                add(_arrayLengthOffsetLabel);
            }
            _arrayLengthTypeLabel = new ClassActorLabel(inspection(), JavaTypeDescriptor.INT);
            if (_parent.showType()) {
                add(_arrayLengthTypeLabel);
            }
            _arrayLengthNameLabel = new TextLabel(inspection(), "length");
            add(_arrayLengthNameLabel);
            // Assume length never changes
            _arrayLengthValueLabel = new DataLabel.IntAsDecimal(inspection(), teleArrayObject.getLength());
            add(_arrayLengthValueLabel);
        } else if (teleObject instanceof TeleHybridObject) {
            final TeleHybridObject teleHybridObject = (TeleHybridObject) teleObject;
            _arrayLengthAddressLabel = new LocationLabel.AsAddressWithOffset(inspection(), arrayLengthOffset, origin);
            if (_parent.showAddresses()) {
                add(_arrayLengthAddressLabel);
            }
            _arrayLengthOffsetLabel = new LocationLabel.AsOffset(inspection(), arrayLengthOffset, origin);
            if (_parent.showOffsets()) {
                add(_arrayLengthOffsetLabel);
            }
            _arrayLengthTypeLabel = new ClassActorLabel(inspection(), JavaTypeDescriptor.INT);
            if (_parent.showType()) {
                add(_arrayLengthTypeLabel);
            }
            _arrayLengthNameLabel = new TextLabel(inspection(), "length");
            add(_arrayLengthNameLabel);
            // Assume length never changes
            _arrayLengthValueLabel = new DataLabel.IntAsDecimal(inspection(), teleHybridObject.readArrayLength());
            add(_arrayLengthValueLabel);
        } else {
            _arrayLengthAddressLabel = null;
            _arrayLengthOffsetLabel = null;
            _arrayLengthTypeLabel = null;
            _arrayLengthNameLabel = null;
            _arrayLengthValueLabel = null;
        }

        final int columns = parent.numberOfTupleColumns();
        SpringUtilities.makeCompactGrid(this, getComponentCount() / columns, columns, 0, 0, 10, 2);
    }

    public void refresh(long epoch, boolean force) {
        _miscValueLabel.refresh(epoch, force);
    }

    public void redisplay() {

        setOpaque(true);
        setBackground(style().defaultBackgroundColor());

        // first line: pointer to hub
        _hubAddressLabel.redisplay();
        _hubOffsetLabel.redisplay();
        _hubTypeLabel.redisplay();
        _hubNameLabel.redisplay();
        _hubValueLabel.redisplay();

        // Second line:  "misc" word
        _miscAddressLabel.redisplay();
        _miscOffsetLabel.redisplay();
        _miscTypeLabel.redisplay();
        _miscNameLabel.redisplay();
        _miscValueLabel.redisplay();

        if (_teleObject instanceof TeleHybridObject || _teleObject instanceof TeleArrayObject) {
            // Third line:  array information
            _arrayLengthAddressLabel.redisplay();
            _arrayLengthOffsetLabel.redisplay();
            _arrayLengthTypeLabel.redisplay();
            _arrayLengthNameLabel.redisplay();
            _arrayLengthValueLabel.redisplay();
        }
    }
}
