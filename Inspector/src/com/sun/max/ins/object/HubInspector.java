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

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.method.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;

/**
 * An object inspector specialized for displaying a Maxine low-level {@link Hybrid} object in the {@link TeleVM},
 * constructed using {@link HybridLayout}, representing a {@link Hub}.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public class HubInspector extends ObjectInspector<HubInspector> {

    private InspectorTable _fieldsTable;
    private ArrayElementsTable _vTableTable;
    private ArrayElementsTable _iTableTable;
    private ArrayElementsTable _mTableTable;
    private ArrayElementsTable _refMapTable;

    private final InspectorMenuItems _classMethodInspectorMenuItems;

    HubInspector(Inspection inspection, Residence residence, TeleObject teleObject) {
        super(inspection, residence, teleObject);
        final TeleClassMethodActor teleClassMethodActor = teleObject.getTeleClassMethodActorForObject();
        createFrame(null);
        if (teleClassMethodActor != null) {
            // the object is, or is associated with a ClassMethodActor.
            _classMethodInspectorMenuItems = new ClassMethodMenuItems(inspection(), teleClassMethodActor);
            frame().add(_classMethodInspectorMenuItems);
        } else {
            _classMethodInspectorMenuItems = null;
        }
    }

    @Override
    protected synchronized void createView(long epoch) {
        super.createView(epoch);
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        panel.setBackground(style().defaultBackgroundColor());
        _fieldsTable = new ObjectFieldsTable(this, teleObject().getFieldActors());
        final JScrollPane fieldsScrollPane = new JScrollPane(_fieldsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        fieldsScrollPane.setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, style().defaultBorderColor()));
        fieldsScrollPane.setBackground(style().defaultBackgroundColor());
        fieldsScrollPane.setOpaque(true);

        panel.add(fieldsScrollPane);

        final TeleHub teleHub = (TeleHub) teleObject();
        final Hub hub = teleHub.hub();
        if (hub.vTableLength() > 0) {
            final int vTableStartIndex = Hub.vTableStartIndex();
            _vTableTable = new ArrayElementsTable(this, Kind.WORD, teleVM().layoutScheme().wordArrayLayout().getElementOffsetFromOrigin(vTableStartIndex).toInt(),
                            vTableStartIndex,
                            hub.vTableLength(),
                            "V",
                            WordValueLabel.ValueMode.CALL_ENTRY_POINT);
            final JScrollPane vTableScrollPane = new JScrollPane(_vTableTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            vTableScrollPane.setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, style().defaultBorderColor()));
            vTableScrollPane.setBackground(style().defaultBackgroundColor());
            vTableScrollPane.setOpaque(true);
            panel.add(vTableScrollPane);
        }
        if (hub.iTableLength() > 0) {
            final int iTableStartIndex = hub.iTableStartIndex();
            _iTableTable = new ArrayElementsTable(this, Kind.WORD, teleVM().layoutScheme().wordArrayLayout().getElementOffsetFromOrigin(iTableStartIndex).toInt(),
                            iTableStartIndex,
                            hub.iTableLength(),
                            "I",
                            WordValueLabel.ValueMode.ITABLE_ENTRY);
            final JScrollPane iTableScrollPane = new JScrollPane(_iTableTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            iTableScrollPane.setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, style().defaultBorderColor()));
            iTableScrollPane.setBackground(style().defaultBackgroundColor());
            iTableScrollPane.setOpaque(true);
            panel.add(iTableScrollPane);
        }
        if (hub.mTableLength() > 0) {
            final int mTableStartIndex = teleVM().fields().Hub_mTableStartIndex.readInt(teleObject().reference());
            _mTableTable = new ArrayElementsTable(this, Kind.INT, teleVM().layoutScheme().intArrayLayout().getElementOffsetFromOrigin(mTableStartIndex).toInt(),
                            mTableStartIndex,
                            teleVM().fields().Hub_mTableLength.readInt(teleObject().reference()),
                            "M",
                            WordValueLabel.ValueMode.WORD);
            final JScrollPane mTableScrollPane = new JScrollPane(_mTableTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            mTableScrollPane.setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, style().defaultBorderColor()));
            mTableScrollPane.setBackground(style().defaultBackgroundColor());
            mTableScrollPane.setOpaque(true);
            panel.add(mTableScrollPane);
        }
        if (hub.referenceMapLength() > 0) {
            final int referenceMapStartIndex = teleVM().fields().Hub_referenceMapStartIndex.readInt(teleObject().reference());
            _refMapTable = new ArrayElementsTable(this, Kind.INT, teleVM().layoutScheme().intArrayLayout().getElementOffsetFromOrigin(referenceMapStartIndex).toInt(),
                            referenceMapStartIndex,
                            teleVM().fields().Hub_referenceMapLength.readInt(teleObject().reference()),
                            "R",
                            WordValueLabel.ValueMode.WORD);
            final JScrollPane refMapScrollPane = new JScrollPane(_refMapTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            refMapScrollPane.setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, style().defaultBorderColor()));
            refMapScrollPane.setBackground(style().defaultBackgroundColor());
            refMapScrollPane.setOpaque(true);
            panel.add(refMapScrollPane);
        }

//        final JScrollPane scrollPane = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//        scrollPane.setBackground(style().defaultBackgroundColor());
//        scrollPane.setOpaque(true);
//        frame().getContentPane().add(scrollPane);

        frame().getContentPane().add(panel);
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        super.refreshView(epoch, force);
        _fieldsTable.refresh(epoch, force);
        if (_iTableTable != null) {
            _iTableTable.refresh(epoch, force);
        }
        if (_vTableTable != null) {
            _vTableTable.refresh(epoch, force);
        }
        if (_mTableTable != null) {
            _mTableTable.refresh(epoch, force);
        }
        if (_refMapTable != null) {
            _refMapTable.refresh(epoch, force);
        }
        if (_classMethodInspectorMenuItems != null) {
            _classMethodInspectorMenuItems.refresh(epoch, force);
        }
    }


}
