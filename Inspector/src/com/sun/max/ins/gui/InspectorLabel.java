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
package com.sun.max.ins.gui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;

/**
 * A label specialized for use in the {@link Inspector}.
 * <br>
 * Labels can act as a source for drag and drop operations.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public abstract class InspectorLabel extends JLabel implements InspectionHolder, TextSearchable, Prober {

    private static final int TRACE_VALUE = 2;

    /**
     * Support for labels that can act as a source for a drag and drop gesture.
     *
     * @author Michael Van De Vanter
     */
    private class InspectorLabelDragSource implements DragGestureListener, DragSourceListener {

        /**
         * The cursor set for the current drag action.
         */
        private Cursor dragCursor = null;

        public InspectorLabelDragSource() {
        }

        public void dragGestureRecognized(DragGestureEvent dge) {
            Trace.line(TRACE_VALUE, InspectorLabel.this.tracePrefix() + "initiating drag=" + dge.getTriggerEvent());
            Transferable transferable = getTransferable();
            if (transferable != null) {
                if (transferable instanceof InspectorTransferable) {
                    final InspectorTransferable inspectorTransferable = (InspectorTransferable) transferable;
                    dragCursor = inspectorTransferable.getDragCursor();
                } else {
                    dragCursor = null;
                }
                Trace.line(TRACE_VALUE, tracePrefix() + "dragging=" + transferable);
                dge.startDrag(null, transferable, this);
            }
        }

        public void dragEnter(DragSourceDragEvent dsde) {
            Trace.line(TRACE_VALUE, tracePrefix() + "Drag Source: dragEnter ");
            dsde.getDragSourceContext().setCursor(dragCursor);
        }

        public void dragExit(DragSourceEvent dsde) {
            Trace.line(TRACE_VALUE, tracePrefix() + "Drag Source: dragExit");
            dsde.getDragSourceContext().setCursor(null);
        }

        public void dragOver(DragSourceDragEvent dsde) {
            Trace.line(TRACE_VALUE, tracePrefix() + "Drag Source: dragOver");
        }

        public void dropActionChanged(DragSourceDragEvent dsde) {
            Trace.line(TRACE_VALUE, tracePrefix() + "Drag Source: dropActionChanged");
        }

        public void dragDropEnd(DragSourceDropEvent dsde) {
            Trace.line(TRACE_VALUE, tracePrefix() + "Drag Source: drop completed, success: "
                + dsde.getDropSuccess());
        }
    }

    private final Inspection inspection;
    private boolean dragSourceEnabled = false;
    private final String tracePrefix;
    /**
     * A label for use in the inspector, by default not opaque.
     *
     * @param text label text
     * @param toolTipText text for ToolTips
     */
    public InspectorLabel(Inspection inspection, String text, String toolTipText) {
        super(text);
        this.inspection = inspection;
        String simpleName = getClass().getSimpleName();
        if (simpleName.equals("")) {
            simpleName = "anonymous InspectorLabel";
        }
        this.tracePrefix = "[" + simpleName + "] ";
        setToolTipText(toolTipText);
        setOpaque(false);
    }

    /**
     * A label for use in the inspector.
     * @param text label text
     */
    public InspectorLabel(Inspection inspection, String text) {
        this(inspection, text, null);
    }

    /**
     * A label for use in the inspector.
     */
    public InspectorLabel(Inspection inspection) {
        this(inspection, null, null);
    }

    public final Inspection inspection() {
        return inspection;
    }

    public final MaxVM maxVM() {
        return inspection.maxVM();
    }

    public final MaxVMState maxVMState() {
        return inspection.maxVMState();
    }

    public final MaxCodeManager codeManager() {
        return inspection.codeManager();
    }

    public final MaxBreakpointFactory breakpointFactory() {
        return inspection.breakpointFactory();
    }

    public final MaxWatchpointFactory watchpointFactory() {
        return inspection.watchpointFactory();
    }

    public final boolean watchpointsEnabled() {
        return inspection.watchpointsEnabled();
    }

    public InspectorGUI gui() {
        return inspection.gui();
    }

    public final InspectorStyle style() {
        return inspection.style();
    }

    public final InspectionFocus focus() {
        return inspection.focus();
    }

    public final InspectionActions actions() {
        return inspection.actions();
    }

    public String getSearchableText() {
        return getText();
    }

    /**
     * Enables support for this label to act as a <strong>source</strong> for drag
     * and drop operations (copy only, not move).
     * <br>
     * Once this has been called, attempts to drag from the label will cause the
     * method {@link #getTransferable()} to be called.
     *
     * @see #getTransferable()
     */
    protected void enableDragSource() {
        Trace.line(TRACE_VALUE, tracePrefix() + "enable drag source");
        if (!dragSourceEnabled) {
            DragSource dragSource = DragSource.getDefaultDragSource();
            dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, new InspectorLabelDragSource());
            dragSourceEnabled = true;
        }
    }

    /**
     * Creates something that can be copied, as the source of a drag and drop operation.
     * <br>
     * Will not be called by a drag gesture starting in the label
     * unless {@link #enableDragSource()} has been previously called.
     * <br>
     * An exception to the above occurs when the label is used as a cell renderer for an
     * {@link InspectorTable}. In that situation, the table's drag and drop mechanism may
     * call this method, whether or not {@link #enableDragSource()} has been called.
     *
     * @return something that can be dragged from this label; null if nothing can be dragged.
     * @see #enableDragSource()
     * @see InspectorTable
     */
    public Transferable getTransferable() {
        return null;
    }

    /**
     * @return default prefix text for trace messages; identifies the class being traced.
     */
    protected String tracePrefix() {
        return tracePrefix;
    }

}
