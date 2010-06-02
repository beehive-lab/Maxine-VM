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
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.tele.*;
import com.sun.max.vm.runtime.*;

/**
 * A panel for displaying the values of thread local variables in one of VM thread local areas associated
 * with a VM thread.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class ThreadLocalsAreaPanel extends InspectorPanel {

    private final MaxThreadLocalsArea threadLocalsArea;
    private final ThreadLocalsAreaHeaderPanel threadLocalsAreaHeaderPanel;
    private final ThreadLocalsAreaTable threadLocalsAreaTable;

    ThreadLocalsAreaPanel(Inspection inspection, MaxThread thread, MaxThreadLocalsArea threadLocalsArea, ThreadLocalsViewPreferences preferences) {
        super(inspection);
        this.threadLocalsArea = threadLocalsArea;
        threadLocalsAreaHeaderPanel = new ThreadLocalsAreaHeaderPanel(inspection, threadLocalsArea);
        threadLocalsAreaTable = new ThreadLocalsAreaTable(inspection, threadLocalsArea, preferences);

        setLayout(new BorderLayout());
        add(threadLocalsAreaHeaderPanel, BorderLayout.NORTH);

        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), threadLocalsAreaTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void refresh(boolean force) {
        if (isShowing()) {
            threadLocalsAreaHeaderPanel.refresh(force);
            threadLocalsAreaTable.refresh(force);
        }
    }

    @Override
    public void redisplay() {
        threadLocalsAreaHeaderPanel.redisplay();
        threadLocalsAreaTable.redisplay();
    }

    /**
     * @return the state with which the values displayed in the panel are associated.
     */
    public Safepoint.State getSafepointState() {
        return threadLocalsArea.safepointState();
    }

    public InspectorTable getTable() {
        return threadLocalsAreaTable;
    }

    private final class ThreadLocalsAreaHeaderPanel extends InspectorPanel {

        private final List<InspectorLabel> labels = new ArrayList<InspectorLabel>();

        public ThreadLocalsAreaHeaderPanel(Inspection inspection, MaxThreadLocalsArea threadLocalsArea) {
            super(inspection);
            addInspectorLabel(new TextLabel(inspection, "start: "));
            final MaxMemoryRegion memoryRegion = threadLocalsArea.memoryRegion();
            addInspectorLabel(new WordValueLabel(inspection, ValueMode.WORD, memoryRegion.start(), this));
            add(Box.createHorizontalGlue());
            addInspectorLabel(new TextLabel(inspection, "end: "));
            addInspectorLabel(new WordValueLabel(inspection, ValueMode.WORD, memoryRegion.end(), this));
            add(Box.createHorizontalGlue());
            addInspectorLabel(new TextLabel(inspection, "size: "));
            addInspectorLabel(new DataLabel.IntAsDecimal(inspection, memoryRegion.size().toInt()));
        }

        private void addInspectorLabel(InspectorLabel label) {
            add(label);
            labels.add(label);
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
