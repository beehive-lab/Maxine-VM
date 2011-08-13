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
import com.sun.max.ins.view.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.vm.value.*;

/**
 * A label specialized for use in the {@link AbstractView}.
 * <br>
 * Labels can act as a source for drag and drop operations.
 */
public abstract class InspectorLabel extends JLabel implements InspectionHolder, TextSearchable, Prober {

    private static final int TRACE_VALUE = 2;

    /**
     * Support for labels that can act as a source for a drag and drop gesture.
     *
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
    private String textPrefix = "";


    /**
     * An optional string that can be appended to every label's text.
     */
    private String textSuffix = "";

    /**
     * An optional string that can be prepended to every label tooltip text.
     */
    private String toolTipPrefix = "";


    /**
     * An optional string that can be appended to every label tooltip text.
     */
    private String toolTipSuffix = "";

    /**
     * Translates an int into hex text prefixed with "0x, e.g. "0x4ef".
     *
     * @param intValue an int
     * @return string describing the int as hex with "0x" prefix
     */
    public static final String intTo0xHex(int intValue) {
        return "0x" + Integer.toHexString(intValue);
    }

    /**
     * Translates an integer into decimal text with plus/minus, e.g. "-2", "0", or "+3"
     *
     * @return integer as a decimal text string with a plus/minus prefix if not zero.
     */
    public static final String intToPlusMinusDecimal(int intValue) {
        return (intValue >= 0 ? "+" : "") + Integer.toString(intValue);
    }

    /**
     * Translates an integer into decimal text, followed by prefixed hex equivalent, e.g. "22(0x16)"
     *
     * @return string describing the integer in both decimal and hex, with no "+" prefix.
     */
    public static final String intToDecimalAndHex(int intValue) {
        return Integer.toString(intValue) + "(" + intTo0xHex(intValue) + ")";
    }

    /**
     * Translates an integer into decimal text with plus/minus, followed by prefixed, unpadded
     * hex equivalent, e.g. "+22(0x16)"
     *
     * @return string describing the relative location in both decimal and hex, with a "+" prefix when non-negative
     */
    public static final String intToPlusMinusDecimalAndHex(int intValue) {
        return intToPlusMinusDecimal(intValue) + "(" + intTo0xHex(intValue) + ")";
    }

    /**
     * Translates a long into decimal text with plus/minus, e.g. "-2", "0", or "+3"
     *
     * @return long as a decimal text string with a plus/minus prefix if not zero.
     */
    public static final String longToPlusMinusDecimal(long longValue) {
        return (longValue >= 0 ? "+" : "") + Long.toString(longValue);
    }

    /**
     * Translates a long into hex text prefixed with "0x, e.g. "0x4ef".
     *
     * @param longValue a long
     * @return string describing the long as hex with "0x" prefix
     */
    public static final String longTo0xHex(long longValue) {
        return "0x" + Long.toHexString(longValue);
    }

    /**
     * Translates a long into decimal text, followed by prefixed hex equivalent, e.g. "22(0x16)"
     *
     * @return string describing the long in both decimal and hex, with no "+" prefix.
     */
    public static final String longToDecimalAndHex(long longValue) {
        return Long.toString(longValue) + "(" + longTo0xHex(longValue) + ")";
    }

    /**
     * Translates a sequence of bytes into space-separated text surrounded by brackets, e.g. "[0F FF A0]"
     */
    public static final String bytesToByteString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        final StringBuilder result = new StringBuilder(100);
        String prefix = "[";
        for (byte b : bytes) {
            result.append(prefix);
            result.append(String.format("%02X", b));
            prefix = " ";
        }
        result.append("]");
        return result.toString();
    }

    /**
     * Translates a VM {@code Value} into decimal text, followed by prefixed
     * hex equivalent, e.g. "+22(0x16)"
     *
     * @param value
     * @return string describing the value in both decimal and hex
     */
    public static final String valueToDecimalAndHex(Value value) {
        return Long.toString(value.toLong()) + "(" + value.toWord().to0xHexString() + ")";
    }

    public static final String valueToFloatText(Value value) {
        return Float.toString(Float.intBitsToFloat((int) (value.toLong() & 0xffffffffL))) + "f";
    }

    public static final String valueToDoubleText(Value value) {
        return Double.toString(Double.longBitsToDouble(value.toLong())) + "d";
    }

    /**
     * Translates a string that may legitimately contain the characters '<' and '>',
     * replacing every instance characters with HTML special character codes so that they
     * will be displayed correctly.  Should not be used on any string that already contains
     * HTML tags, because the method does not discriminate.
     *
     * @param text a text string
     * @return a text string with all occurrences of '<' and '>' by HTML special character codes.
     */
    public static final String htmlify(String text) {
        return text == null ? null : text.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    /**
     * A label for use in the Inspector, by default not opaque.
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
     * A label for use in the Inspector.
     * @param text label text
     */
    public InspectorLabel(Inspection inspection, String text) {
        this(inspection, text, null);
    }

    /**
     * A label for use in the Inspector.
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

    public final InspectionViews views() {
        return inspection.views();
    }

    public final InspectionActions actions() {
        return inspection.actions();
    }

    public String getSearchableText() {
        return getText();
    }

    /**
     * Sets text to be prepended to every subsequently "wrapped" label text
     *.
     * @param textPrefix prefix for every text display.
     * @see #setWrappedText(String)
     */
    public final void setTextPrefix(String textPrefix) {
        this.textPrefix = textPrefix == null ? "" : textPrefix;
        //redisplay();
    }

    /**
     * Sets text to be appended to every subsequently "wrapped" label text, with an additional
     * space inserted between.
     *
     * @param textSuffix suffix for every text display.
     * @see #setWrappedText(String)
     */
    public final void setTextSuffix(String textSuffix) {
        if (textSuffix != null && !textSuffix.equals("")) {
            this.textSuffix = " " + textSuffix;
        } else {
            this.textSuffix = "";
        }
        //redisplay();
    }

    /**
     * Sets the label's text to the specified string, wrapped
     * by an optional prefix and an optional suffix.
     *
     * @param text the text to be wrapped and set as the label's text.
     * @see #setTextPrefix(String)
     * @see #setTextSuffix(String)
     * @see #setText(String)
     */
    public void setWrappedText(String text) {
        //System.out.println(" pre=" + textPrefix + " txt=" + text + " suf=" + textSuffix);
        super.setText(textPrefix + text + textSuffix);
    }

    /**
     * Sets the label's text to the specified string, but starting
     * with an {@code <html>} tag, and wrapped
     * by an optional prefix and an optional suffix.
     * Note that any text appearing in this context should be
     * filtered by {@link #htmlify(String)} so that angle bracket
     * characters will be rendered correctly.
     *
     * @param text the text to be wrapped and set as the label's text.
     * @see #setTextPrefix(String)
     * @see #setTextSuffix(String)
     * @see #setText(String)
     */
    public void setWrappedHtmlText(String text) {
        //System.out.println("<html>" + " pre=" + textPrefix + " txt=" + text + " suf=" + textSuffix);
        super.setText("<html>" + textPrefix + text + textSuffix);
        //super.setText(textPrefix + text + textSuffix);
    }

    /**
     * Sets text to be prepended to every subsequently "wrapped" tooltip, with an additional
     * space inserted between.  Be sure to set the prefix <i>before</i> an event (such as a
     * value change) that causes the tool tip text to be regenerated.
     *
     * @param toolTipPrefix prefix for every tooltip display.
     * @see #setWrappedToolTipHtmlText(String)
     */
    public final void setToolTipPrefix(String toolTipPrefix) {
        if (toolTipPrefix != null && !toolTipPrefix.equals("")) {
            this.toolTipPrefix = toolTipPrefix + " ";
        } else {
            this.toolTipPrefix = "";
        }
        //redisplay();
    }

    /**
     * Sets text to be appended to every subsequently "wrapped" tooltip, with an additional
     * space inserted between.
     *
     * @param toolTipSuffix suffix for every tooltip display.
     * @see #setWrappedToolTipHtmlText(String)
     */
    public final void setToolTipSuffix(String toolTipSuffix) {
        if (toolTipSuffix != null && !toolTipSuffix.equals("")) {
            this.toolTipSuffix = " " + toolTipSuffix;
        } else {
            this.toolTipSuffix = "";
        }
        //redisplay();
    }

    /**
     * Sets the label's tool tip text to the specified string, but starting
     * with an {@code <html>} tag, and wrapped
     * by an optional prefix and an optional suffix.
     *
     * @param toolTipText the text to be wrapped and set as the label's tool tip text.
     * @see #setToolTipPrefix(String)
     * @see #setToolTipSuffix(String)
     * @see #setToolTipText(String)
     */
    public final void setWrappedToolTipHtmlText(String toolTipText) {
        //System.out.println("<html>" + toolTipPrefix + toolTipText + toolTipSuffix);
        super.setToolTipText("<html>" + toolTipPrefix + toolTipText + toolTipSuffix);
    }

    /**
     * @return the text string assigned to the label with all HTML tags removed.
     */
    public final String getTextDeHtmlify() {
        return getText().replaceAll("\\<.*?\\>", "");
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
