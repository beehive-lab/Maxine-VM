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
package com.sun.max.ins.method;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.constant.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.LocationLabel.AsAddressWithPosition;
import com.sun.max.ins.object.*;
import com.sun.max.ins.util.*;
import com.sun.max.ins.value.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.MaxMachineCode.InstructionMap;
import com.sun.max.tele.method.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.value.*;

/**
 * A table-based viewer for an (immutable) section of {@link TargetCode} in the VM.
 * Supports visual effects for execution state, and permits user selection
 * of instructions for various purposes (e.g. set breakpoint).
 *
 * @author Mick Jordan
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public class JTableTargetCodeViewer extends TargetCodeViewer {

    private static final int TRACE_VALUE = 2;

    private final MaxPlatform.ISA isa;
    private final Inspection inspection;
    private final TargetCodeTable table;
    private final TargetCodeTableModel tableModel;
    private final TargetCodeViewPreferences instanceViewPreferences;
    private final TableColumn[] columns;
    private final OperandsRenderer operandsRenderer;
    private final SourceLineRenderer sourceLineRenderer;
    private final Color defaultBackgroundColor;
    private final Color stopBackgroundColor;

    public JTableTargetCodeViewer(Inspection inspection, MethodInspector parent, MaxMachineCode machineCode) {
        super(inspection, parent, machineCode);
        this.inspection = inspection;
        //inspection.vm().bootImage().header.
        this.isa = inspection.vm().platform().getISA();
        this.operandsRenderer = new OperandsRenderer();
        this.sourceLineRenderer = new SourceLineRenderer();
        this.tableModel = new TargetCodeTableModel(inspection, machineCode);
        this.columns = new TableColumn[TargetCodeColumnKind.values().length];
        instanceViewPreferences = new TargetCodeViewPreferences(TargetCodeViewPreferences.globalPreferences(inspection())) {
            @Override
            public void setIsVisible(TargetCodeColumnKind columnKind, boolean visible) {
                super.setIsVisible(columnKind, visible);
                table.getInspectorTableColumnModel().setColumnVisible(columnKind.ordinal(), visible);
                JTableColumnResizer.adjustColumnPreferredWidths(table);
                refresh(true);
            }
        };
        final TargetCodeTableColumnModel tableColumnModel = new TargetCodeTableColumnModel(instanceViewPreferences);
        this.table = new TargetCodeTable(inspection, tableModel, tableColumnModel);
        defaultBackgroundColor = this.table.getBackground();
        stopBackgroundColor = style().darken2(defaultBackgroundColor);
        createView();
    }

    @Override
    protected void createView() {
        super.createView();

        // Set up toolbar
        JButton button = new InspectorButton(inspection, actions().toggleTargetCodeBreakpoint());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugToggleBreakpointbuttonIcon());
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugStepOver());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugStepOverButtonIcon());
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugSingleStep());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugStepInButtonIcon());
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugReturnFromFrame());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugStepOutButtonIcon());
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugRunToSelectedInstruction());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugRunToCursorButtonIcon());
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugResume());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugContinueButtonIcon());
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugPause());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugPauseButtonIcon());
        toolBar().add(button);

        toolBar().add(Box.createHorizontalGlue());

        toolBar().add(new JLabel("Target Code"));

        toolBar().add(Box.createHorizontalGlue());

        addActiveRowsButton();

        addSearchButton();

        final JButton viewOptionsButton = new InspectorButton(inspection(), new AbstractAction("View...") {
            public void actionPerformed(ActionEvent actionEvent) {
                final TargetCodeViewPreferences globalPreferences = TargetCodeViewPreferences.globalPreferences(inspection());
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<TargetCodeColumnKind>(inspection(), "TargetCode View Options", instanceViewPreferences, globalPreferences);
            }
        });
        viewOptionsButton.setToolTipText("Target code view options");
        viewOptionsButton.setText(null);
        viewOptionsButton.setIcon(style().generalPreferencesIcon());
        toolBar().add(viewOptionsButton);

        toolBar().add(Box.createHorizontalGlue());
        addCodeViewCloseButton();

        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), table);
        add(scrollPane, BorderLayout.CENTER);

        refresh(true);
        JTableColumnResizer.adjustColumnPreferredWidths(table);
    }

    @Override
    protected int getRowCount() {
        return table.getRowCount();
    }

    @Override
    protected int getSelectedRow() {
        return table.getSelectedRow();
    }

    @Override
    protected void setFocusAtRow(int row) {
        focus().setCodeLocation(instructionMap().instructionLocation(row));
    }

    @Override
    protected RowTextMatcher getRowTextSearcher() {
        return new TableRowTextMatcher(inspection, table);
    }

    /**
     * Global code selection has been set; return true iff the view contains selection.
     * Update even when the selection is set to the same value, because we want
     * that to force a scroll to make the selection visible.
     */
    @Override
    public boolean updateCodeFocus(MaxCodeLocation codeLocation) {
        return table.updateCodeFocus(codeLocation);
    }

    @Override
    public void refresh(boolean force) {
        super.refresh(force);
        table.refresh(force);
//      updateSize();
    }

    @Override
    public void redisplay() {
        super.redisplay();
        table.redisplay();
        // TODO (mlvdv)  code view hack for style changes
        table.setRowHeight(style().codeTableRowHeight());
        invalidate();
        repaint();
    }

    private InstructionMap instructionMap() {
        return machineCode().getInstructionMap();
    }

    /**
     * A table specialized for displaying a block of disassembled target code, one instruction per line.
     */
    private final class TargetCodeTable extends InspectorTable {

        // TODO (mlvdv) Extract the table class

        TargetCodeTable(Inspection inspection, TargetCodeTableModel tableModel, TargetCodeTableColumnModel tableColumnModel) {
            super(inspection, tableModel, tableColumnModel);
            setFillsViewportHeight(true);
            setShowHorizontalLines(style().codeTableShowHorizontalLines());
            setShowVerticalLines(style().codeTableShowVerticalLines());
            setIntercellSpacing(style().codeTableIntercellSpacing());
            setRowHeight(style().codeTableRowHeight());
            setRowSelectionAllowed(true);
            setColumnSelectionAllowed(true);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        @Override
        protected void mouseButton1Clicked(int row, int col, MouseEvent mouseEvent) {
            if (mouseEvent.getClickCount() > 1) {
                // Depends on the first click selecting the row, and that changing the current
                // code location focus to the location under the mouse event.7
                actions().toggleTargetCodeBreakpoint().perform();
            }
        }

        @Override
        protected InspectorPopupMenu getPopupMenu(int row, int col, MouseEvent mouseEvent) {
            if (col == ObjectColumnKind.TAG.ordinal()) {
                final InspectorPopupMenu menu = new InspectorPopupMenu();
                final TargetCodeTableModel targetCodeTableModel = (TargetCodeTableModel) getModel();
                final MaxCodeLocation codeLocation = targetCodeTableModel.rowToLocation(row);
                menu.add(actions().debugRunToInstructionWithBreakpoints(codeLocation, "Run to this instruction"));
                menu.add(actions().debugRunToInstruction(codeLocation, "Run to this instruction (ignoring breakpoints)"));
                menu.add(actions().toggleTargetCodeBreakpoint(codeLocation, "Toggle breakpoint (double-click)"));
                menu.add(actions().setTargetCodeBreakpoint(codeLocation, "Set breakpoint"));
                menu.add(actions().removeTargetCodeBreakpoint(codeLocation, "Unset breakpoint"));
                return menu;
            }
            return null;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            // The selection in the table has changed; might have happened via user action (click, arrow) or
            // as a side effect of a focus change.
            super.valueChanged(e);
            if (!e.getValueIsAdjusting()) {
                final int selectedRow = getSelectedRow();
                final TargetCodeTableModel targetCodeTableModel = (TargetCodeTableModel) getModel();
                if (selectedRow >= 0 && selectedRow < targetCodeTableModel.getRowCount()) {
                    focus().setCodeLocation(targetCodeTableModel.rowToLocation(selectedRow));
                }
            }
        }

        /**
         * Global code selection has been set; return true iff the view contains selection.
         * Update even when the selection is set to the same value, because we want
         * that to force a scroll to make the selection visible.
         */
        public boolean updateCodeFocus(MaxCodeLocation codeLocation) {
            final int oldSelectedRow = getSelectedRow();
            if (codeLocation != null && codeLocation.hasAddress()) {
                final Address targetCodeInstructionAddress = focus().codeLocation().address();
                if (machineCode().contains(targetCodeInstructionAddress)) {
                    final TargetCodeTableModel model = (TargetCodeTableModel) getModel();
                    final int row = model.findRow(targetCodeInstructionAddress);
                    if (row >= 0) {
                        if (row != oldSelectedRow) {
                            updateSelection(row);
                            Trace.line(TRACE_VALUE, tracePrefix() + "changeSelection " + row);
                        }
                        scrollToRows(row, row);
                        Trace.line(TRACE_VALUE, tracePrefix() + " scroll to row " + row);
                        return true;
                    }
                }
            }
            // View doesn't contain the focus; clear any old selection
            if (oldSelectedRow >= 0) {
                clearSelection();
            }
            return false;
        }
    }

    private final class TargetCodeTableColumnModel extends InspectorTableColumnModel<TargetCodeColumnKind> {

        private TargetCodeTableColumnModel(TargetCodeViewPreferences viewPreferences) {
            super(TargetCodeColumnKind.values().length, viewPreferences);
            final Address startAddress = tableModel.rowToInstruction(0).address;
            addColumn(TargetCodeColumnKind.TAG, new TagRenderer(inspection), null);
            addColumn(TargetCodeColumnKind.NUMBER, new NumberRenderer(), null);
            addColumn(TargetCodeColumnKind.ADDRESS, new AddressRenderer(startAddress), null);
            addColumn(TargetCodeColumnKind.POSITION, new PositionRenderer(startAddress), null);
            addColumn(TargetCodeColumnKind.LABEL, new LabelRenderer(startAddress), null);
            addColumn(TargetCodeColumnKind.INSTRUCTION, new InstructionRenderer(inspection), null);
            addColumn(TargetCodeColumnKind.OPERANDS, operandsRenderer, null);
            addColumn(TargetCodeColumnKind.SOURCE_LINE, sourceLineRenderer, null);
            addColumn(TargetCodeColumnKind.BYTES, new BytesRenderer(inspection), null);
        }
    }

    /**
     * Data model representing a block of disassembled code, one row per instruction.
     */
    private final class TargetCodeTableModel extends InspectorTableModel {

        final MaxMachineCode machineCode;

        public TargetCodeTableModel(Inspection inspection, MaxMachineCode machineCode) {
            super(inspection);
            assert machineCode != null;
            this.machineCode = machineCode;
        }

        public int getColumnCount() {
            return TargetCodeColumnKind.values().length;
        }

        public int getRowCount() {
            return machineCode.getInstructionMap().length();
        }

        public Object getValueAt(int row, int col) {
            final TargetCodeInstruction targetCodeInstruction = rowToInstruction(row);
            switch (TargetCodeColumnKind.values()[col]) {
                case TAG:
                    return null;
                case NUMBER:
                    return row;
                case ADDRESS:
                    return targetCodeInstruction.address;
                case POSITION:
                    return targetCodeInstruction.position;
                case LABEL:
                    final String label = targetCodeInstruction.label;
                    return label != null ? label + ":" : "";
                case INSTRUCTION:
                    return targetCodeInstruction.mnemonic;
                case OPERANDS:
                    return targetCodeInstruction.operands;
                case SOURCE_LINE:
                    return "";
                case BYTES:
                    return targetCodeInstruction.bytes;
                default:
                    throw new RuntimeException("Column out of range: " + col);
            }
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            switch (TargetCodeColumnKind.values()[col]) {
                case TAG:
                    return Object.class;
                case NUMBER:
                    return Integer.class;
                case ADDRESS:
                    return Address.class;
                case POSITION:
                    return Integer.class;
                case LABEL:
                case INSTRUCTION:
                case OPERANDS:
                case SOURCE_LINE:
                    return String.class;
                case BYTES:
                    return byte[].class;
                default:
                    throw new RuntimeException("Column out of range: " + col);
            }
        }

        @Override
        public String getRowDescription(int row) {
            return "Instruction " + row + " (" + rowToInstruction(row).mnemonic + ")";
        }

        public TargetCodeInstruction rowToInstruction(int row) {
            return machineCode.getInstructionMap().instruction(row);
        }

        public MaxCodeLocation rowToLocation(int row) {
            return machineCode.getInstructionMap().instructionLocation(row);
        }

        /**
         * @param address a code address in the VM.
         * @return the row in this block of code containing an instruction starting at the address, -1 if none.
         */
        public int findRow(Address address) {
            final InstructionMap instructionMap = machineCode.getInstructionMap();
            for (int row = 0; row < instructionMap.length(); row++) {
                final TargetCodeInstruction targetCodeInstruction = instructionMap.instruction(row);
                if (targetCodeInstruction.address.equals(address)) {
                    return row;
                }
            }
            return -1;
        }
    }

    /**
     * Return the appropriate color for displaying the row's text depending on whether the instruction pointer is at
     * this row.
     *
     * @param row the row to check
     * @param col TODO
     * @return the color to be used
     */
    public Color cellForegroundColor(int row, int col) {
        return isInstructionPointer(row) ? style().debugIPTextColor() : (isCallReturn(row) ? style().debugCallReturnTextColor() : null);
    }

    private void setBorderForRow(JComponent component, int row) {
        if (instructionMap().isBytecodeBoundary(row)) {
            component.setBorder(style().defaultPaneTopBorder());
        } else {
            component.setBorder(null);
        }
    }

    /**
     * Sets the background of a cell rendering component, depending on the row context.
     * <br>
     * Makes the renderer transparent if there is no special background needed.
     */
    private void setBackgroundForRow(JComponent component, int row) {

        if (isSearchMatchRow(row)) {
            component.setOpaque(true);
            component.setBackground(style().searchMatchedBackground());
        } else if (instructionMap().isStop(row)) {
            component.setOpaque(true);
            component.setBackground(stopBackgroundColor);
        } else {
            component.setOpaque(false);
            //component.setBackground(getBackground());
        }
    }

    private final class TagRenderer extends InspectorLabel implements TableCellRenderer, TextSearchable, Prober {

        public TagRenderer(Inspection inspection) {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object ignore, boolean isSelected, boolean hasFocus, int row, int col) {
            setToolTipPrefix(tableModel.getRowDescription(row));
            final StringBuilder toolTipSB = new StringBuilder(100);
            final MaxStackFrame stackFrame = stackFrame(row);
            if (stackFrame != null) {
                if (stackFrame.position() == 0) {
                    toolTipSB.append("<br>IP (stack frame 0) in thread ");
                } else {
                    toolTipSB.append("<br>Call return (frame ");
                    toolTipSB.append(stackFrame.position());
                    toolTipSB.append(") in thread ");
                }
                toolTipSB.append(inspection.nameDisplay().longName(stackFrame.stack().thread()));
                toolTipSB.append(" points here");
                if (stackFrame.isTop()) {
                    setIcon(style().debugIPTagIcon());
                    setForeground(style().debugIPTagColor());
                } else {
                    setIcon(style().debugCallReturnTagIcon());
                    setForeground(style().debugCallReturnTagColor());
                }
            } else {
                setIcon(null);
                setForeground(null);
            }
            setText(rowToTagText(row));
            final MaxBreakpoint targetBreakpoint = getTargetBreakpointAtRow(row);
            if (targetBreakpoint != null) {
                toolTipSB.append("<br>breakpoint set @ ");
                toolTipSB.append(targetBreakpoint.codeLocation().address().to0xHexString());
                toolTipSB.append(targetBreakpoint.isEnabled() ? ", enabled" : ", disabled");
                if (targetBreakpoint.isEnabled()) {
                    setBorder(style().debugEnabledTargetBreakpointTagBorder());
                } else {
                    setBorder(style().debugDisabledTargetBreakpointTagBorder());
                }
            } else if (instructionMap().isBytecodeBoundary(row)) {
                setBorder(style().defaultPaneTopBorder());
            } else {
                setBorder(null);
            }
            setWrappedToolTipText(toolTipSB.toString());
            setBackgroundForRow(this, row);
            return this;
        }

        @Override
        public String getSearchableText() {
            return "";
        }

        public void redisplay() {
        }

        public void refresh(boolean force) {
        }
    }

    private final class NumberRenderer extends PlainLabel implements TableCellRenderer {

        public NumberRenderer() {
            super(inspection, "");
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setValue(row);
            setToolTipText(tableModel.getRowDescription(row));
            setBackgroundForRow(this, row);
            setForeground(cellForegroundColor(row, column));
            setBorderForRow(this, row);
            return this;
        }
    }

    private final class AddressRenderer extends AsAddressWithPosition implements TableCellRenderer {

        private final Address entryAddress;

        AddressRenderer(Address entryAddress) {
            super(inspection, 0, entryAddress);
            this.entryAddress = entryAddress;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final Address address = (Address) value;
            setToolTipPrefix(tableModel.getRowDescription(row) + " location<br>address= ");
            setValue(address.minus(entryAddress).asSize().toInt());
            setBackgroundForRow(this, row);
            setForeground(cellForegroundColor(row, column));
            setBorderForRow(this, row);
            return this;
        }
    }

    private final class PositionRenderer extends LocationLabel.AsPosition implements TableCellRenderer {
        private int position;

        public PositionRenderer(Address entryAddress) {
            super(inspection, 0, entryAddress);
            this.position = 0;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final Integer position = (Integer) value;
            if (this.position != position) {
                this.position = position;
                setValue(position);
            }
            setToolTipPrefix(tableModel.getRowDescription(row) + " location<br>address= ");
            setBackgroundForRow(this, row);
            setForeground(cellForegroundColor(row, column));
            setBorderForRow(this, row);
            return this;
        }
    }

    private final class LabelRenderer extends LocationLabel.AsTextLabel implements TableCellRenderer {

        public LabelRenderer(Address entryAddress) {
            super(inspection, entryAddress);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final Integer position = (Integer) tableModel.getValueAt(row, TargetCodeColumnKind.POSITION.ordinal());
            setLocation(value.toString(), position);
            setWrappedToolTipText(tableModel.getRowDescription(row));
            setFont(style().defaultFont());
            setBackgroundForRow(this, row);
            //setForeground(getRowTextColor(row));

            if (isInstructionPointer(row)) {
                setForeground(style().debugIPTextColor());
            } else if (isCallReturn(row)) {
                setForeground(style().debugCallReturnTextColor());
            } else {
                setForeground(null);
            }
            setBorderForRow(this, row);
            return this;
        }
    }

    private final class InstructionRenderer extends TargetCodeLabel implements TableCellRenderer {
        InstructionRenderer(Inspection inspection) {
            super(inspection, "");
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setBackgroundForRow(this, row);
            setForeground(cellForegroundColor(row, column));
            final String instructionName = value.toString();
            setText(instructionName);
            setWrappedToolTipText(tableModel.getRowDescription(row) + "<br>ISA = " + isa.name());
            setBorderForRow(this, row);
            return this;
        }
    }

    private interface LiteralRenderer {
        WordValueLabel render(Inspection inspection, String literalLoadText, Address literalAddress);
    }

    static final LiteralRenderer AMD64_LITERAL_RENDERER = new LiteralRenderer() {
        public WordValueLabel render(Inspection inspection, String literalLoadText, final Address literalAddress) {
            final WordValueLabel wordValueLabel = new WordValueLabel(inspection, WordValueLabel.ValueMode.LITERAL_REFERENCE, null) {
                @Override
                public Value fetchValue() {
                    return vm().readWordValue(literalAddress);
                }
            };
            wordValueLabel.setTextPrefix(literalLoadText.substring(0, literalLoadText.indexOf("[")));
            wordValueLabel.setToolTipSuffix(" from RIP " + literalLoadText.substring(literalLoadText.indexOf("["), literalLoadText.length()));
            wordValueLabel.updateText();
            return wordValueLabel;
        }
    };

    static final LiteralRenderer SPARC_LITERAL_RENDERER = new LiteralRenderer() {
        public WordValueLabel render(Inspection inspection, String literalLoadText, final Address literalAddress) {
            final WordValueLabel wordValueLabel = new WordValueLabel(inspection, WordValueLabel.ValueMode.LITERAL_REFERENCE, null) {
                @Override
                public Value fetchValue() {
                    return vm().readWordValue(literalAddress);
                }
            };
            wordValueLabel.setTextSuffix(literalLoadText.substring(literalLoadText.indexOf(",")));
            wordValueLabel.setToolTipSuffix(" from " + literalLoadText.substring(0, literalLoadText.indexOf(",")));
            wordValueLabel.updateText();
            return wordValueLabel;
        }
    };

    LiteralRenderer getLiteralRenderer(Inspection inspection) {
        switch (isa) {
            case AMD64:
                return AMD64_LITERAL_RENDERER;
            case SPARC:
                return SPARC_LITERAL_RENDERER;
            case ARM:
            case PPC:
            case IA32:
                InspectorError.unimplemented();
                return null;
        }
        InspectorError.unknownCase();
        return null;
    }

    private final class SourceLineRenderer extends PlainLabel implements TableCellRenderer {

        private BytecodeLocation lastBytecodeLocation;

        SourceLineRenderer() {
            super(JTableTargetCodeViewer.this.inspection(), null);
            addMouseListener(new InspectorMouseClickAdapter(inspection()) {
                @Override
                public void procedure(final MouseEvent mouseEvent) {
                    final BytecodeLocation bytecodeLocation = lastBytecodeLocation;
                    if (bytecodeLocation != null) {
                        final InspectorPopupMenu menu = new InspectorPopupMenu();
                        for (BytecodeLocation location = bytecodeLocation; location != null; location = location.parent()) {
                            final StackTraceElement stackTraceElement = location.toStackTraceElement();
                            final String fileName = stackTraceElement.getFileName();
                            if (fileName != null) {
                                final int lineNumber = stackTraceElement.getLineNumber();
                                if (lineNumber > 0) {
                                    if (vm().findJavaSourceFile(location.classMethodActor.holder()) != null) {
                                        final BytecodeLocation locationCopy = location;
                                        menu.add(new AbstractAction("Open " + fileName + " at line " + lineNumber) {
                                            public void actionPerformed(ActionEvent e) {
                                                inspection().viewSourceExternally(locationCopy);
                                            }
                                        });
                                    }
                                }
                            }
                        }
                        if (menu.getComponentCount() > 0) {
                            menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                        }
                    }
                }
            });
        }

        private String toolTipText(StackTraceElement stackTraceElement) {
            String s = stackTraceElement.toString();
            final int openParen = s.indexOf('(');
            String methodName = stackTraceElement.getMethodName().replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            s = Classes.getSimpleName(stackTraceElement.getClassName()) + "." + methodName + s.substring(openParen);
            final String text = s;
            return text;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final BytecodeLocation bytecodeLocation = instructionMap().bytecodeLocation(row);
            setText("");
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>");
            setWrappedToolTipText("Source location not available");
            setBackgroundForRow(this, row);
            if (bytecodeLocation != null) {
                final StackTraceElement stackTraceElement = bytecodeLocation.toStackTraceElement();
                final StringBuilder stackTrace = new StringBuilder("<table cellpadding=\"1%\"><tr><td></td><td>").append(toolTipText(stackTraceElement)).append("</td></tr>");
                StackTraceElement top = stackTraceElement;
                for (BytecodeLocation parent = bytecodeLocation.parent(); parent != null; parent = parent.parent()) {
                    StackTraceElement parentSTE = parent.toStackTraceElement();
                    stackTrace.append("<tr><td>--&gt;&nbsp;</td><td>").append(toolTipText(parentSTE)).append("</td></tr>");
                    top = parentSTE;
                }
                setWrappedToolTipText("Source location = " + stackTrace.append("</table>").toString());
                setText(String.valueOf(top.getLineNumber()));
            }
            lastBytecodeLocation = bytecodeLocation;
            setBorderForRow(this, row);
            return this;
        }
    }

    private final class OperandsRenderer implements TableCellRenderer, Prober {
        private InspectorLabel[] inspectorLabels = new InspectorLabel[instructionMap().length()];
        private TargetCodeLabel targetCodeLabel = new TargetCodeLabel(inspection, "");
        private LiteralRenderer literalRenderer = getLiteralRenderer(inspection);

        public void refresh(boolean force) {
            for (InspectorLabel wordValueLabel : inspectorLabels) {
                if (wordValueLabel != null) {
                    wordValueLabel.refresh(force);
                }
            }
        }

        public void redisplay() {
            for (InspectorLabel wordValueLabel : inspectorLabels) {
                if (wordValueLabel != null) {
                    wordValueLabel.redisplay();
                }
            }
            targetCodeLabel.redisplay();
        }

        public Component getTableCellRendererComponent(JTable table, Object ignore, boolean isSelected, boolean hasFocus, int row, int column) {
            InspectorLabel renderer = inspectorLabels[row];
            if (renderer == null) {
                final TargetCodeInstruction targetCodeInstruction = tableModel.rowToInstruction(row);
                final String text = targetCodeInstruction.operands;
                if (targetCodeInstruction.targetAddress != null && !machineCode().contains(targetCodeInstruction.targetAddress)) {
                    renderer = new WordValueLabel(inspection, WordValueLabel.ValueMode.CALL_ENTRY_POINT, targetCodeInstruction.targetAddress, table);
                    renderer.setToolTipPrefix(tableModel.getRowDescription(row) + ": operand = ");
                    inspectorLabels[row] = renderer;
                } else if (targetCodeInstruction.literalSourceAddress != null) {
                    final Address literalAddress = targetCodeInstruction.literalSourceAddress.asAddress();
                    renderer = literalRenderer.render(inspection, text, literalAddress);
                    renderer.setToolTipPrefix(tableModel.getRowDescription(row) + ": operand = ");
                    inspectorLabels[row] = renderer;
                } else {
                    InstructionMap instructionMap = instructionMap();
                    if (instructionMap.calleeConstantPoolIndex(row) >= 0 && targetCodeInstruction.mnemonic.contains("call")) {
                        final PoolConstantLabel poolConstantLabel =
                            PoolConstantLabel.make(inspection, instructionMap.calleeConstantPoolIndex(row), localConstantPool(), teleConstantPool(), PoolConstantLabel.Mode.TERSE);
                        poolConstantLabel.setToolTipPrefix(text);
                        poolConstantLabel.redisplay();
                        renderer = poolConstantLabel;
                        renderer.setForeground(cellForegroundColor(row, column));
                    } else {
                        if (instructionMap.isNativeCall(row)) {
                            renderer = new TextLabel(inspection, "<native function>");
                            renderer.setToolTipPrefix(tableModel.getRowDescription(row) + ":");
                            renderer.setWrappedToolTipText("<br>operands = " + text);
                            renderer.setForeground(cellForegroundColor(row, column));
                        } else {
                            renderer = targetCodeLabel;
                            renderer.setToolTipPrefix(tableModel.getRowDescription(row) + ":");
                            renderer.setText(text);
                            renderer.setWrappedToolTipText("<br>operands = " + text);
                            renderer.setForeground(cellForegroundColor(row, column));
                        }
                    }
                }

            }
            setBackgroundForRow(renderer, row);
            setBorderForRow(renderer, row);
            return renderer;
        }

    }

    private final class BytesRenderer extends DataLabel.ByteArrayAsHex implements TableCellRenderer {
        BytesRenderer(Inspection inspection) {
            super(inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setBackgroundForRow(this, row);
            setForeground(cellForegroundColor(row, column));
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>as bytes = ");
            setValue((byte[]) value);
            setBorderForRow(this, row);
            return this;
        }
    }

    @Override
    public void print(String name) {
        final MessageFormat header = new MessageFormat(name);
        final MessageFormat footer = new MessageFormat(vm().entityName() + ": " + codeViewerKindName() + "  Printed: " + new Date() + " -- Page: {0, number, integer}");
        try {
            table.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (PrinterException printerException) {
            gui().errorMessage("Print failed: " + printerException.getMessage());
        }
    }
}

