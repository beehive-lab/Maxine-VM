/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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

    private final MaxThreadLocalsArea tla;
    private final ThreadLocalsAreaHeaderPanel tlaHeaderPanel;
    private final ThreadLocalsAreaTable tlaTable;

    ThreadLocalsAreaPanel(Inspection inspection, MaxThread thread, MaxThreadLocalsArea tla, ThreadLocalsViewPreferences preferences) {
        super(inspection);
        this.tla = tla;
        tlaHeaderPanel = new ThreadLocalsAreaHeaderPanel(inspection, tla);
        tlaTable = new ThreadLocalsAreaTable(inspection, tla, preferences);

        setLayout(new BorderLayout());
        add(tlaHeaderPanel, BorderLayout.NORTH);

        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), tlaTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void refresh(boolean force) {
        if (isShowing()) {
            tlaHeaderPanel.refresh(force);
            tlaTable.refresh(force);
        }
    }

    @Override
    public void redisplay() {
        tlaHeaderPanel.redisplay();
        tlaTable.redisplay();
    }

    /**
     * @return the state with which the values displayed in the panel are associated.
     */
    public Safepoint.State getSafepointState() {
        return tla.safepointState();
    }

    public InspectorTable getTable() {
        return tlaTable;
    }

    private final class ThreadLocalsAreaHeaderPanel extends InspectorPanel {

        private final List<InspectorLabel> labels = new ArrayList<InspectorLabel>();

        public ThreadLocalsAreaHeaderPanel(Inspection inspection, MaxThreadLocalsArea tla) {
            super(inspection);
            addInspectorLabel(new TextLabel(inspection, "start: "));
            final MaxMemoryRegion memoryRegion = tla.memoryRegion();
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
