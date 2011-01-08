/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
     * An optional string that can be prepended to every label's text.
     */
    private String textPrefix;


    /**
     * An optional string that can be appended to every label's text.
     */
    private String textSuffix;

    /**
     * An optional string that can be prepended to every label tooltip text.
     */
    private String toolTipPrefix;


    /**
     * An optional string that can be appended to every label tooltip text.
     */
    private String toolTipSuffix;

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

    public final MaxVM vm() {
        return inspection.vm();
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
     * Sets text to be prepended to every subsequently "wrapped" label text, with an additional
     * space inserted between.
     *
     * @param textPrefix prefix for every text display.
     * @see #setWrappedText(String)
     */
    public final void setTextPrefix(String textPrefix) {
        if (textPrefix == null || textPrefix.equals("")) {
            this.textPrefix = null;
        } else {
            this.textPrefix = textPrefix + " ";
        }
    }

    /**
     * Sets text to be appended to every subsequently "wrapped" label text, with an additional
     * space inserted between.
     *
     * @param textSuffix suffix for every text display.
     * @see #setWrappedText(String)
     */
    public final void setTextSuffix(String textSuffix) {
        if (textSuffix == null || textSuffix.equals("")) {
            this.textSuffix = null;
        } else {
            this.textSuffix = " " + textSuffix;
        }
    }

    /**
     * Sets the label's text to the specified string, but wrapped
     * by an optional prefix and an optional suffix.
     *
     * @param text the text to be wrapped and set as the label's text.
     * @see #setTextPrefix(String)
     * @see #setTextSuffix(String)
     * @see #setText(String)
     */
    public void setWrappedText(String text) {
        String wrappedText = text;
        wrappedText = textPrefix == null ? wrappedText : textPrefix + wrappedText;
        wrappedText = textSuffix == null ? wrappedText : wrappedText + textSuffix;
        super.setText(wrappedText);
    }

    /**
     * Sets text to be prepended to every subsequently "wrapped" tooltip, with an additional
     * space inserted between.
     *
     * @param toolTipPrefix prefix for every tooltip display.
     * @see #setWrappedToolTipText(String)
     */
    public final void setToolTipPrefix(String toolTipPrefix) {
        if (toolTipPrefix == null || toolTipPrefix.equals("")) {
            this.toolTipPrefix = null;
        } else {
            this.toolTipPrefix = toolTipPrefix + " ";
        }
    }

    /**
     * Sets text to be appended to every subsequently "wrapped" tooltip, with an additional
     * space inserted between.
     *
     * @param toolTipSuffix suffix for every tooltip display.
     * @see #setWrappedToolTipText(String)
     */
    public final void setToolTipSuffix(String toolTipSuffix) {
        if (toolTipSuffix == null || toolTipSuffix.equals("")) {
            this.toolTipSuffix = null;
        } else {
            this.toolTipSuffix = " " + toolTipSuffix;
        }
    }

    /**
     * Sets the label's tool tip text to the specified string, but wrapped
     * by an optional prefix and an optional suffix.
     *
     * @param toolTipText the text to be wrapped and set as the label's tool tip text.
     * @see #setToolTipPrefix(String)
     * @see #setToolTispSuffix(String)
     * @see #setToolTipText(String)
     */
    public final void setWrappedToolTipText(String toolTipText) {
        String wrappedText = toolTipText;
        wrappedText = toolTipPrefix == null ? wrappedText : toolTipPrefix + wrappedText;
        wrappedText = toolTipSuffix == null ? wrappedText : wrappedText + toolTipSuffix;
        super.setToolTipText(wrappedText);
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
     * @return the dragSourceEnabled
     */
    protected boolean isDragSourceEnabled() {
        return dragSourceEnabled;
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
