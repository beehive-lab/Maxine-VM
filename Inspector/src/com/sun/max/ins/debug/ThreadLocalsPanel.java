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
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.vm.runtime.*;

/**
 * A panel for displaying the values in one of VM thread local storage areas associated
 * with a VM thread.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class ThreadLocalsPanel extends InspectorPanel {

    private final ThreadLocalsHeaderPanel threadLocalsHeaderPanel;
    private final ThreadLocalsTable threadLocalsTable;
    final TeleThreadLocalValues teleVMThreadLocalValues;

    ThreadLocalsPanel(Inspection inspection, MaxThread thread, TeleThreadLocalValues values, ThreadLocalsViewPreferences preferences) {
        super(inspection);
        threadLocalsHeaderPanel = new ThreadLocalsHeaderPanel(inspection, values);
        threadLocalsTable = new ThreadLocalsTable(inspection, thread, values, preferences);
        teleVMThreadLocalValues = values;

        setLayout(new BorderLayout());
        add(threadLocalsHeaderPanel, BorderLayout.NORTH);

        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), threadLocalsTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void refresh(boolean force) {
        if (isShowing()) {
            threadLocalsHeaderPanel.refresh(force);
            threadLocalsTable.refresh(force);
        }
    }

    @Override
    public void redisplay() {
        threadLocalsHeaderPanel.redisplay();
        threadLocalsTable.redisplay();
    }

    /**
     * @return the state with which the values displayed in the panel are associated.
     */
    public Safepoint.State getSafepointState() {
        return teleVMThreadLocalValues.safepointState();
    }

    public InspectorTable getTable() {
        return threadLocalsTable;
    }


    private final class ThreadLocalsHeaderPanel extends InspectorPanel {

        private final AppendableSequence<InspectorLabel> labels = new LinkSequence<InspectorLabel>();

        public ThreadLocalsHeaderPanel(Inspection inspection, TeleThreadLocalValues values) {
            super(inspection);
            addInspectorLabel(new TextLabel(inspection, "start: "));
            addInspectorLabel(new WordValueLabel(inspection, ValueMode.WORD, values.start(), this));
            add(Box.createHorizontalGlue());
            addInspectorLabel(new TextLabel(inspection, "end: "));
            addInspectorLabel(new WordValueLabel(inspection, ValueMode.WORD, values.end(), this));
            add(Box.createHorizontalGlue());
            addInspectorLabel(new TextLabel(inspection, "size: "));
            addInspectorLabel(new DataLabel.IntAsDecimal(inspection, values.size().toInt()));
        }

        private void addInspectorLabel(InspectorLabel label) {
            add(label);
            labels.append(label);
        }

        @Override
        public void redisplay() {
            for (InspectorLabel label : labels) {
                label.redisplay();
            }
        }

        @Override
        public void refresh(boolean force) {
            for (InspectorLabel label : labels) {
                label.refresh(force);
            }
        }
    }
}
