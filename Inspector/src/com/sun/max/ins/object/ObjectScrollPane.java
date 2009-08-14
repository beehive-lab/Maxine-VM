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
    public static ObjectScrollPane createArrayElementsPane(ObjectInspector objectInspector, TeleArrayObject teleArrayObject) {
        final int length = teleArrayObject.getLength();
        final ArrayClassActor arrayClassActor = (ArrayClassActor) teleArrayObject.classActorForType();
        final Kind kind = arrayClassActor.componentClassActor().kind;
        final WordValueLabel.ValueMode valueMode = kind == Kind.REFERENCE ? WordValueLabel.ValueMode.REFERENCE : WordValueLabel.ValueMode.WORD;
        final int arrayOffsetFromOrigin = arrayClassActor.kind.arrayLayout(Layout.layoutScheme()).getElementOffsetFromOrigin(0).toInt();
        final ArrayElementsTable arrayElementsTable = new ArrayElementsTable(objectInspector, kind, arrayOffsetFromOrigin, 0, length, "", valueMode);
        return new ObjectScrollPane(objectInspector.inspection(), arrayElementsTable);
    }

    /**
     * @return a new {@link JScrollPane} displaying the fields of a {@link TeleTupleObject} ; never null;
     */
    public static ObjectScrollPane createFieldsPane(ObjectInspector objectInspector, TeleTupleObject teleTupleObject) {
        final ObjectFieldsTable inspectorTable = new ObjectFieldsTable(objectInspector, teleTupleObject.getFieldActors());
        return new ObjectScrollPane(objectInspector.inspection(), inspectorTable);
    }

    /**
     * @return a new {@link JScrollPane} displaying the fields of a {@link TeleHub} object; never null;
     */
    public static ObjectScrollPane createFieldsPane(ObjectInspector objectInspector, TeleHub teleHub) {
        final ObjectFieldsTable inspectorTable = new ObjectFieldsTable(objectInspector, teleHub.getFieldActors());
        return new ObjectScrollPane(objectInspector.inspection(), inspectorTable);
    }

    /**
     * @return a new {@link JScrollPane} displaying the "vTable" of a {@link TeleHub}object; null if the table is empty.
     */
    public static ObjectScrollPane createVTablePane(ObjectInspector objectInspector, TeleHub teleHub) {
        if (teleHub.vTableLength() == 0) {
            return null;
        }
        final InspectorTable table = new ArrayElementsTable(objectInspector,
                        teleHub.vTableKind(),
                        teleHub.vTableOffset().toInt(),
                        teleHub.vTableStartIndex(),
                        teleHub.vTableLength(),
                        "V",
                        WordValueLabel.ValueMode.CALL_ENTRY_POINT);
        return new ObjectScrollPane(objectInspector.inspection(), table);
    }

    /**
     * @return a new {@link JScrollPane} displaying the "iTable" of a {@link TeleHub} object; null if the table is empty.
     */
    public static ObjectScrollPane createITablePane(ObjectInspector objectInspector, TeleHub teleHub) {
        if (teleHub.iTableLength() == 0) {
            return null;
        }
        final InspectorTable table = new ArrayElementsTable(objectInspector,
                        teleHub.iTableKind(),
                        teleHub.iTableOffset().toInt(),
                        teleHub.iTableStartIndex(),
                        teleHub.iTableLength(),
                        "I",
                        WordValueLabel.ValueMode.ITABLE_ENTRY);
        return new ObjectScrollPane(objectInspector.inspection(), table);
    }

    /**
     * @return a new {@link JScrollPane} displaying the "mTable" of a {@link TeleHub} object; null if the table is empty.
     */
    public static ObjectScrollPane createMTablePane(ObjectInspector objectInspector, TeleHub teleHub) {
        if (teleHub.mTableLength() == 0) {
            return null;
        }
        final InspectorTable table = new ArrayElementsTable(objectInspector,
                        teleHub.mTableKind(),
                        teleHub.mTableOffset().toInt(),
                        teleHub.mTableStartIndex(),
                        teleHub.mTableLength(),
                        "M",
                        WordValueLabel.ValueMode.WORD);
        return new ObjectScrollPane(objectInspector.inspection(), table);
    }

    /**
     * @return a new {@link JScrollPane}  displaying the reference map of the {@link TeleHub}; null if the map is empty.
     */
    public static ObjectScrollPane createRefMapPane(ObjectInspector objectInspector, TeleHub teleHub) {
        if (teleHub.hub().referenceMapLength == 0) {
            return null;
        }
        final InspectorTable table = new ArrayElementsTable(objectInspector,
                        teleHub.referenceMapKind(),
                        teleHub.referenceMapOffset().toInt(),
                        teleHub.referenceMapStartIndex(),
                        teleHub.referenceMapLength(),
                        "R",
                        WordValueLabel.ValueMode.WORD);
        return new ObjectScrollPane(objectInspector.inspection(), table);
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
        final int displayRows = Math.min(style().memoryTableMaxDisplayRows(), inspectorTable.getRowCount()) + 1;
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
