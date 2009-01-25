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

    private ObjectFieldsPanel _fieldsPanel;
    private ObjectArrayPanel _vTablePanel;
    private ObjectArrayPanel _iTablePanel;
    private ObjectArrayPanel _mTablePanel;
    private ObjectArrayPanel _refMapPanel;

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
        _fieldsPanel = new ObjectFieldsPanel(this, getFieldActors());
        panel.add(_fieldsPanel);

        final TeleHub teleHub = (TeleHub) teleObject();
        final Hub hub = teleHub.hub();
        if (hub.vTableLength() > 0) {
            final int vTableStartIndex = Hub.vTableStartIndex();
            _vTablePanel = new ObjectArrayPanel(this, Kind.WORD, teleVM().layoutScheme().wordArrayLayout().getElementOffsetFromOrigin(vTableStartIndex).toInt(),
                            vTableStartIndex,
                            hub.vTableLength(),
                            "V",
                            WordValueLabel.ValueMode.CALL_ENTRY_POINT);
            panel.add(_vTablePanel);
        }
        if (hub.iTableLength() > 0) {
            final int iTableStartIndex = hub.iTableStartIndex();
            _iTablePanel = new ObjectArrayPanel(this, Kind.WORD, teleVM().layoutScheme().wordArrayLayout().getElementOffsetFromOrigin(iTableStartIndex).toInt(),
                            iTableStartIndex,
                            hub.iTableLength(),
                            "I",
                            WordValueLabel.ValueMode.ITABLE_ENTRY);
            panel.add(_iTablePanel);
        }
        if (hub.mTableLength() > 0) {
            final int mTableStartIndex = teleVM().fields().Hub_mTableStartIndex.readInt(teleObject().reference());
            _mTablePanel = new ObjectArrayPanel(this, Kind.INT, teleVM().layoutScheme().intArrayLayout().getElementOffsetFromOrigin(mTableStartIndex).toInt(),
                            mTableStartIndex,
                            teleVM().fields().Hub_mTableLength.readInt(teleObject().reference()),
                            "M",
                            WordValueLabel.ValueMode.WORD);
            panel.add(_mTablePanel);
        }
        if (hub.referenceMapLength() > 0) {
            final int referenceMapStartIndex = teleVM().fields().Hub_referenceMapStartIndex.readInt(teleObject().reference());
            _refMapPanel = new ObjectArrayPanel(this, Kind.INT, teleVM().layoutScheme().intArrayLayout().getElementOffsetFromOrigin(referenceMapStartIndex).toInt(),
                            referenceMapStartIndex,
                            teleVM().fields().Hub_referenceMapLength.readInt(teleObject().reference()),
                            "R",
                            WordValueLabel.ValueMode.WORD);
            panel.add(_refMapPanel);
        }

        final JScrollPane scrollPane = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBackground(style().defaultBackgroundColor());
        scrollPane.setOpaque(true);
        frame().getContentPane().add(scrollPane);
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        super.refreshView(epoch, force);
        _fieldsPanel.refresh(epoch, force);
        if (_iTablePanel != null) {
            _iTablePanel.refresh(epoch, force);
        }
        if (_vTablePanel != null) {
            _vTablePanel.refresh(epoch, force);
        }
        if (_mTablePanel != null) {
            _mTablePanel.refresh(epoch, force);
        }
        if (_refMapPanel != null) {
            _refMapPanel.refresh(epoch, force);
        }
        if (_classMethodInspectorMenuItems != null) {
            _classMethodInspectorMenuItems.refresh(epoch, force);
        }
    }


}
