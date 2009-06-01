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
import com.sun.max.ins.memory.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.type.*;


/**
 * A factory class that creates pane components, each of which displays a specific part of a Maxine low-level heap object in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class ObjectPane extends InspectorScrollPane {

    /**
     * @return a new {@link JScrollPane} displaying the fields of a {@link TeleArrayObject}; never null;
     */
    public static ObjectPane createArrayElementsPane(ObjectInspector objectInspector, TeleArrayObject teleArrayObject) {
        final int length = teleArrayObject.getLength();
        final ArrayClassActor arrayClassActor = (ArrayClassActor) teleArrayObject.classActorForType();
        final Kind kind = arrayClassActor.componentClassActor().kind();
        final WordValueLabel.ValueMode valueMode = kind == Kind.REFERENCE ? WordValueLabel.ValueMode.REFERENCE : WordValueLabel.ValueMode.WORD;
        final int arrayOffsetFromOrigin = arrayClassActor.arrayLayout().getElementOffsetFromOrigin(0).toInt();
        final ArrayElementsTable arrayElementsTable = new ArrayElementsTable(objectInspector, kind, arrayOffsetFromOrigin, 0, length, "", valueMode);
        return new ObjectPane(objectInspector.inspection(), arrayElementsTable);
    }

    /**
     * @return a new {@link JScrollPane} displaying the fields of a {@link TeleTupleObject} ; never null;
     */
    public static ObjectPane createFieldsPane(ObjectInspector objectInspector, TeleTupleObject teleTupleObject) {
        final ObjectFieldsTable inspectorTable = new ObjectFieldsTable(objectInspector, teleTupleObject.getFieldActors());
        return new ObjectPane(objectInspector.inspection(), inspectorTable);
    }

    /**
     * @return a new {@link JScrollPane} displaying the fields of a {@link TeleHub} object; never null;
     */
    public static ObjectPane createFieldsPane(ObjectInspector objectInspector, TeleHub teleHub) {
        final ObjectFieldsTable inspectorTable = new ObjectFieldsTable(objectInspector, teleHub.getFieldActors());
        return new ObjectPane(objectInspector.inspection(), inspectorTable);
    }

    /**
     * @return a new {@link JScrollPane} displaying the "vTable" of a {@link TeleHub}object; null if the table is empty.
     */
    public static ObjectPane createVTablePane(ObjectInspector objectInspector, TeleHub teleHub) {
        final Hub hub = teleHub.hub();
        if (hub.vTableLength() == 0) {
            return null;
        }
        final int vTableStartIndex = Hub.vTableStartIndex();
        final InspectorTable table = new ArrayElementsTable(objectInspector, Kind.WORD,
                        objectInspector.inspection().maxVM().vmConfiguration().layoutScheme().wordArrayLayout().getElementOffsetFromOrigin(vTableStartIndex).toInt(),
                        vTableStartIndex,
                        hub.vTableLength(),
                        "V",
                        WordValueLabel.ValueMode.CALL_ENTRY_POINT);
        return new ObjectPane(objectInspector.inspection(), table);
    }

    /**
     * @return a new {@link JScrollPane} displaying the "iTable" of a {@link TeleHub} object; null if the table is empty.
     */
    public static ObjectPane createITablePane(ObjectInspector objectInspector, TeleHub teleHub) {
        final Hub hub = teleHub.hub();
        if (hub.iTableLength() == 0) {
            return null;
        }
        final int iTableStartIndex = hub.iTableStartIndex();
        final InspectorTable table = new ArrayElementsTable(objectInspector, Kind.WORD,
                        objectInspector.inspection().maxVM().vmConfiguration().layoutScheme().wordArrayLayout().getElementOffsetFromOrigin(iTableStartIndex).toInt(),
                        iTableStartIndex,
                        hub.iTableLength(),
                        "I",
                        WordValueLabel.ValueMode.ITABLE_ENTRY);
        return new ObjectPane(objectInspector.inspection(), table);
    }

    /**
     * @return a new {@link JScrollPane} displaying the "mTable" of a {@link TeleHub} object; null if the table is empty.
     */
    public static ObjectPane createMTablePane(ObjectInspector objectInspector, TeleHub teleHub) {
        if (teleHub.hub().mTableLength() == 0) {
            return null;
        }
        final MaxVM maxVM = objectInspector.inspection().maxVM();
        final int mTableStartIndex = maxVM.fields().Hub_mTableStartIndex.readInt(teleHub.reference());
        final InspectorTable table = new ArrayElementsTable(objectInspector, Kind.INT,
                        maxVM.vmConfiguration().layoutScheme().intArrayLayout().getElementOffsetFromOrigin(mTableStartIndex).toInt(),
                        mTableStartIndex,
                        maxVM.fields().Hub_mTableLength.readInt(teleHub.reference()),
                        "M",
                        WordValueLabel.ValueMode.WORD);
        return new ObjectPane(objectInspector.inspection(), table);
    }

    /**
     * @return a new {@link JScrollPane}  displaying the reference map of the {@link TeleHub}; null if the map is empty.
     */
    public static ObjectPane createRefMapPane(ObjectInspector objectInspector, TeleHub teleHub) {
        if (teleHub.hub().referenceMapLength() == 0) {
            return null;
        }
        final MaxVM maxVM = objectInspector.inspection().maxVM();
        final int referenceMapStartIndex = maxVM.fields().Hub_referenceMapStartIndex.readInt(teleHub.reference());
        final InspectorTable table = new ArrayElementsTable(objectInspector, Kind.INT,
                        maxVM.vmConfiguration().layoutScheme().intArrayLayout().getElementOffsetFromOrigin(referenceMapStartIndex).toInt(),
                        referenceMapStartIndex,
                        maxVM.fields().Hub_referenceMapLength.readInt(teleHub.reference()),
                        "R",
                        WordValueLabel.ValueMode.WORD);
        return new ObjectPane(objectInspector.inspection(), table);
    }

    public static ObjectPane createMemoryWordsPane(ObjectInspector objectInspector, TeleObject teleObject) {
        return new ObjectPane(objectInspector.inspection(), new MemoryWordsTable(objectInspector, teleObject));
    }

    private final InspectorTable _inspectorTable;

    /**
     * Creates a scrollable pane containing the {@link InspectorTable}, with preferred height set to match the size
     * of the table up to a specified limit.
     */
    private ObjectPane(Inspection inspection, InspectorTable inspectorTable) {
        super(inspection, inspectorTable);
        _inspectorTable = inspectorTable;
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
        _inspectorTable.redisplay();
    }

    @Override
    public void refresh(long epoch, boolean force) {
        _inspectorTable.refresh(epoch, force);
    }

}
