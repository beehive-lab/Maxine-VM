/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.object;

import java.awt.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.type.*;

/**
 * A factory class that creates pane components, each of which displays a specific part of a low-level heap object in the VM.
 */
public final class ObjectScrollPane extends InspectorScrollPane {

    /**
     * @return a new {@link JScrollPane} displaying the fields of a {@link TeleArrayObject}; never null;
     */
    public static ObjectScrollPane createArrayElementsPane(Inspection inspection, TeleArrayObject teleArrayObject, ObjectViewPreferences instanceViewPreferences) {
        final int length = teleArrayObject.getLength();
        final Kind kind = teleArrayObject.componentKind();
        final WordValueLabel.ValueMode valueMode = kind.isReference ? WordValueLabel.ValueMode.REFERENCE : WordValueLabel.ValueMode.WORD;
        final int arrayOffsetFromOrigin = teleArrayObject.arrayOffsetFromOrigin();
        final ArrayElementsTable arrayElementsTable = new ArrayElementsTable(inspection, teleArrayObject, kind, teleArrayObject.componentType(), arrayOffsetFromOrigin, 0, length, "", valueMode, instanceViewPreferences);
        return new ObjectScrollPane(inspection, arrayElementsTable);
    }

    /**
     * @return a new {@link JScrollPane} displaying the fields of a {@link TeleTupleObject} ; never null;
     */
    public static ObjectScrollPane createFieldsPane(Inspection inspection, TeleTupleObject teleTupleObject, ObjectViewPreferences instanceViewPreferences) {
        final ObjectFieldsTable inspectorTable = new ObjectFieldsTable(inspection, "Object", teleTupleObject, teleTupleObject.getFieldActors(), instanceViewPreferences);
        return new ObjectScrollPane(inspection, inspectorTable);
    }

    /**
     * @return a new {@link JScrollPane} displaying the fields of a {@link TeleHub} object; never null;
     */
    public static ObjectScrollPane createFieldsPane(Inspection inspection, TeleHub teleHub, ObjectViewPreferences instanceViewPreferences) {
        final ObjectFieldsTable inspectorTable = new ObjectFieldsTable(inspection, "Hub", teleHub, teleHub.getFieldActors(), instanceViewPreferences);
        return new ObjectScrollPane(inspection, inspectorTable);
    }

    /**
     * @return a new {@link JScrollPane} displaying the "vTable" of a {@link TeleHub}object; null if the table is empty.
     */
    public static ObjectScrollPane createVTablePane(Inspection inspection, TeleHub teleHub, ObjectViewPreferences instanceViewPreferences) {
        if (teleHub.vTableLength() == 0) {
            return null;
        }
        final InspectorTable table = new ArrayElementsTable(inspection,
                        teleHub,
                        teleHub.vTableKind(),
                        teleHub.vTableType(),
                        teleHub.vTableOffset(),
                        teleHub.vTableStartIndex(),
                        teleHub.vTableLength(),
                        "V",
                        WordValueLabel.ValueMode.CALL_ENTRY_POINT,
                        instanceViewPreferences);
        return new ObjectScrollPane(inspection, table);
    }

    /**
     * @return a new {@link JScrollPane} displaying the "iTable" of a {@link TeleHub} object; null if the table is empty.
     */
    public static ObjectScrollPane createITablePane(Inspection inspection, TeleHub teleHub, ObjectViewPreferences instanceViewPreferences) {
        if (teleHub.iTableLength() == 0) {
            return null;
        }
        final InspectorTable table = new ArrayElementsTable(inspection,
                        teleHub,
                        teleHub.iTableKind(),
                        teleHub.iTableType(),
                        teleHub.iTableOffset(),
                        teleHub.iTableStartIndex(),
                        teleHub.iTableLength(),
                        "I",
                        WordValueLabel.ValueMode.ITABLE_ENTRY,
                        instanceViewPreferences);
        return new ObjectScrollPane(inspection, table);
    }

    /**
     * @return a new {@link JScrollPane} displaying the "mTable" of a {@link TeleHub} object; null if the table is empty.
     */
    public static ObjectScrollPane createMTablePane(Inspection inspection, TeleHub teleHub, ObjectViewPreferences instanceViewPreferences) {
        if (teleHub.mTableLength() == 0) {
            return null;
        }
        final InspectorTable table = new ArrayElementsTable(inspection,
                        teleHub,
                        teleHub.mTableKind(),
                        teleHub.mTableType(),
                        teleHub.mTableOffset(),
                        teleHub.mTableStartIndex(),
                        teleHub.mTableLength(),
                        "M",
                        WordValueLabel.ValueMode.WORD,
                        instanceViewPreferences);
        return new ObjectScrollPane(inspection, table);
    }

    /**
     * @return a new {@link JScrollPane}  displaying the reference map of the {@link TeleHub}; null if the map is empty.
     */
    public static ObjectScrollPane createRefMapPane(Inspection inspection, TeleHub teleHub, ObjectViewPreferences instanceViewPreferences) {
        if (teleHub.hub().referenceMapLength == 0) {
            return null;
        }
        final InspectorTable table = new ArrayElementsTable(inspection,
                        teleHub,
                        teleHub.referenceMapKind(),
                        teleHub.referenceMapType(),
                        teleHub.referenceMapOffset(),
                        teleHub.referenceMapStartIndex(),
                        teleHub.referenceMapLength(),
                        "R",
                        WordValueLabel.ValueMode.WORD,
                        instanceViewPreferences);
        return new ObjectScrollPane(inspection, table);
    }

    private final InspectorTable inspectorTable;

    /**
     * Creates a scrollable pane containing the {@link InspectorTable}, with preferred height set to match the size
     * of the table up to a specified limit.
     */
    private ObjectScrollPane(Inspection inspection, InspectorTable inspectorTable) {
        super(inspection, inspectorTable);
        this.inspectorTable = inspectorTable;
        // Try to size the scroll pane vertically for just enough space, up to a specified maximum;
        // this is empirical, based only the fuzziest notion of how these dimensions work
        final int displayRows = Math.min(preference().style().memoryTableMaxDisplayRows(), inspectorTable.getRowCount()) + 2;
        final int preferredHeight = displayRows * (inspectorTable.getRowHeight() + inspectorTable.getRowMargin()) +
                                                      inspectorTable.getRowMargin()  + inspectorTable.getTableHeader().getHeight();
        final int preferredWidth = inspectorTable.getPreferredScrollableViewportSize().width;
        inspectorTable.setPreferredScrollableViewportSize(new Dimension(preferredWidth, preferredHeight));
    }

    @Override
    public void redisplay() {
        inspectorTable.redisplay();
    }

    @Override
    public void refresh(boolean force) {
        inspectorTable.refresh(force);
    }

}
