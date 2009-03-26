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
package com.sun.max.ins.debug;

import java.awt.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.tele.debug.*;

/**
 * A panel for displaying the values in one of VM thread local storage areas associated
 * with a VM thread.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class ThreadLocalsPanel extends InspectorPanel {

    private final ThreadLocalsHeaderPanel _threadLocalsHeaderPanel;
    private final ThreadLocalsTable _threadLocalsTable;
    final TeleVMThreadLocalValues _teleVMThreadLocalValues;

    ThreadLocalsPanel(ThreadLocalsInspector threadLocalsInspector, TeleVMThreadLocalValues values, ThreadLocalsViewPreferences preferences) {
        super(threadLocalsInspector.inspection());
        _threadLocalsHeaderPanel = new ThreadLocalsHeaderPanel(inspection(), values);
        _threadLocalsTable = new ThreadLocalsTable(threadLocalsInspector, values, preferences);
        _teleVMThreadLocalValues = values;

        setLayout(new BorderLayout());
        add(_threadLocalsHeaderPanel, BorderLayout.NORTH);

        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), _threadLocalsTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void refresh(long epoch, boolean force) {
        if (isShowing()) {
            _threadLocalsHeaderPanel.refresh(epoch, force);
            _threadLocalsTable.refresh(epoch, force);
        }
    }

    @Override
    public void redisplay() {
        _threadLocalsHeaderPanel.redisplay();
        _threadLocalsTable.redisplay();
    }

    private final class ThreadLocalsHeaderPanel extends InspectorPanel {

        private final AppendableSequence<InspectorLabel> _labels = new LinkSequence<InspectorLabel>();

        public ThreadLocalsHeaderPanel(Inspection inspection, TeleVMThreadLocalValues values) {
            super(inspection);
            addInspectorLabel(new TextLabel(inspection, "start: "));
            addInspectorLabel(new WordValueLabel(inspection, ValueMode.WORD, values.start()));
            add(Box.createHorizontalGlue());
            addInspectorLabel(new TextLabel(inspection, "end: "));
            addInspectorLabel(new WordValueLabel(inspection, ValueMode.WORD, values.end()));
            add(Box.createHorizontalGlue());
            addInspectorLabel(new TextLabel(inspection, "size: "));
            addInspectorLabel(new DataLabel.IntAsDecimal(inspection, values.size().toInt()));
        }

        private void addInspectorLabel(InspectorLabel label) {
            add(label);
            _labels.append(label);
        }

        @Override
        public void redisplay() {
            for (InspectorLabel label : _labels) {
                label.redisplay();
            }
        }

        @Override
        public void refresh(long epoch, boolean force) {
            for (InspectorLabel label : _labels) {
                label.refresh(epoch, force);
            }
        }
    }
}
