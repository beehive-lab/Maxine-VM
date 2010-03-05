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

import java.awt.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.type.*;

/**
 * A factory class that creates pane components, each of which displays a specific part of a Maxine low-level heap object in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class ObjectScrollPane extends InspectorScrollPane {

    /**
     * @return a new {@link JScrollPane} displaying the fields of a {@link TeleArrayObject}; never null;
     */
    public static ObjectScrollPane createArrayElementsPane(Inspection inspection, TeleArrayObject teleArrayObject, ObjectViewPreferences instanceViewPreferences) {
        final int length = teleArrayObject.getLength();
        final ArrayClassActor arrayClassActor = (ArrayClassActor) teleArrayObject.classActorForType();
        final Kind kind = arrayClassActor.componentClassActor().kind;
        final WordValueLabel.ValueMode valueMode = kind.isReference ? WordValueLabel.ValueMode.REFERENCE : WordValueLabel.ValueMode.WORD;
        final Offset arrayOffsetFromOrigin = arrayClassActor.kind.arrayLayout(Layout.layoutScheme()).getElementOffsetFromOrigin(0);
        final ArrayElementsTable arrayElementsTable = new ArrayElementsTable(inspection, teleArrayObject, kind, teleArrayObject.componentType(), arrayOffsetFromOrigin, 0, length, "", valueMode, instanceViewPreferences);
        return new ObjectScrollPane(inspection, arrayElementsTable);
    }

    /**
     * @return a new {@link JScrollPane} displaying the fields of a {@link TeleTupleObject} ; never null;
     */
    public static ObjectScrollPane createFieldsPane(Inspection inspection, TeleTupleObject teleTupleObject, ObjectViewPreferences instanceViewPreferences) {
        final ObjectFieldsTable inspectorTable = new ObjectFieldsTable(inspection, teleTupleObject, teleTupleObject.getFieldActors(), instanceViewPreferences);
        return new ObjectScrollPane(inspection, inspectorTable);
    }

    /**
     * @return a new {@link JScrollPane} displaying the fields of a {@link TeleHub} object; never null;
     */
    public static ObjectScrollPane createFieldsPane(Inspection inspection, TeleHub teleHub, ObjectViewPreferences instanceViewPreferences) {
        final ObjectFieldsTable inspectorTable = new ObjectFieldsTable(inspection, teleHub, teleHub.getFieldActors(), instanceViewPreferences);
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
        final int displayRows = Math.min(style().memoryTableMaxDisplayRows(), inspectorTable.getRowCount()) + 2;
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
