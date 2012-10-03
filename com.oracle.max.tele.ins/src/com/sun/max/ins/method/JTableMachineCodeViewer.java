/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.ins.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.LocationLabel.AsAddressWithPosition;
import com.sun.max.ins.object.*;
import com.sun.max.ins.util.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.value.*;

/**
 * A table-based viewer for an (immutable) section of {@link MaxMachineCodeRoutine} in the VM.
 * Supports visual effects for execution state, and permits user selection
 * of instructions for various purposes (e.g. set breakpoint).
 */
public class JTableMachineCodeViewer extends MachineCodeViewer {

    private static final int TRACE_VALUE = 2;

    private final MaxPlatform.ISA isa;
    private final Inspection inspection;
    private final InspectorView view;
    private final MachineCodeTable table;
    private final MachineCodeTableModel tableModel;
    private final MachineCodeViewPreferences instanceViewPreferences;
    private final TableColumn[] columns;
    private final OperandsRenderer operandsRenderer;
    private final SourceLineRenderer sourceLineRenderer;
    private final Color defaultBackgroundColor;
    private final Color safepointBackgroundColor;

    public JTableMachineCodeViewer(Inspection inspection, MethodView parent, MaxMachineCodeRoutine machineCode) {
        super(inspection, parent, machineCode);
        this.inspection = inspection;
        this.view = parent;
        //inspection.vm().bootImage().header.
        this.isa = inspection.vm().platform().getISA();
        this.operandsRenderer = new OperandsRenderer();
        this.sourceLineRenderer = new SourceLineRenderer();
        this.tableModel = new MachineCodeTableModel(inspection, machineCode);
        this.columns = new TableColumn[MachineCodeColumnKind.values().length];
        instanceViewPreferences = new MachineCodeViewPreferences(MachineCodeViewPreferences.globalPreferences(inspection())) {
            @Override
            public void setIsVisible(MachineCodeColumnKind columnKind, boolean visible) {
                super.setIsVisible(columnKind, visible);
                table.getInspectorTableColumnModel().setColumnVisible(columnKind.ordinal(), visible);
                JTableColumnResizer.adjustColumnPreferredWidths(table);
                refresh(true);
            }
        };
        final MachineCodeTableColumnModel tableColumnModel = new MachineCodeTableColumnModel(instanceViewPreferences);
        this.table = new MachineCodeTable(inspection, tableModel, tableColumnModel);
        defaultBackgroundColor = this.table.getBackground();
        safepointBackgroundColor = preference().style().darken2(defaultBackgroundColor);
        createView();
    }

    @Override
    protected void createView() {
        super.createView();

        // Set up toolbar
        JButton button = new InspectorButton(inspection, actions().toggleMachineCodeBreakpoint());
        button.setToolTipText(button.getText());
        button.setText(null);
        final InspectorStyle style = preference().style();
        button.setIcon(style.debugToggleBreakpointbuttonIcon());
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugStepOver());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style.debugStepOverButtonIcon());
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugSingleStep());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style.debugStepInButtonIcon());
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugReturnFromFrame());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style.debugStepOutButtonIcon());
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugRunToSelectedInstruction());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style.debugRunToCursorButtonIcon());
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugResume());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style.debugContinueButtonIcon());
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugPause());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style.debugPauseButtonIcon());
        toolBar().add(button);

        toolBar().add(Box.createHorizontalGlue());

        toolBar().add(new JLabel("Machine Code"));

        toolBar().add(Box.createHorizontalGlue());

        addActiveRowsButton();

        addSearchButton();

        final JButton viewOptionsButton = new InspectorButton(inspection(), new AbstractAction("View...") {
            public void actionPerformed(ActionEvent actionEvent) {
                final MachineCodeViewPreferences globalPreferences = MachineCodeViewPreferences.globalPreferences(inspection());
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<MachineCodeColumnKind>(inspection(), "Machine Code View Options", instanceViewPreferences, globalPreferences);
            }
        });
        viewOptionsButton.setToolTipText("Machine code view options");
        viewOptionsButton.setText(null);
        viewOptionsButton.setIcon(style.generalPreferencesIcon());
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
        focus().setCodeLocation(machineCodeInfo().instructionLocation(row));
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
        table.setRowHeight(preference().style().codeTableRowHeight());
        invalidate();
        repaint();
    }

    private MaxMachineCodeInfo machineCodeInfo() {
        return machineCode().getMachineCodeInfo();
    }

    /**
     * A table specialized for displaying a block of disassembled machine code, one instruction per line.
     */
    private final class MachineCodeTable extends InspectorTable {

        // TODO (mlvdv) Extract the table class

        MachineCodeTable(Inspection inspection, MachineCodeTableModel tableModel, MachineCodeTableColumnModel tableColumnModel) {
            super(inspection, tableModel, tableColumnModel);
            setFillsViewportHeight(true);
            final InspectorStyle style = preference().style();
            setShowHorizontalLines(style.codeTableShowHorizontalLines());
            setShowVerticalLines(style.codeTableShowVerticalLines());
            setIntercellSpacing(style.codeTableIntercellSpacing());
            setRowHeight(style.codeTableRowHeight());
            setRowSelectionAllowed(true);
            setColumnSelectionAllowed(true);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        @Override
        protected void mouseButton1Clicked(int row, int col, MouseEvent mouseEvent) {
            if (mouseEvent.getClickCount() > 1) {
                // Depends on the first click selecting the row, and that changing the current
                // code location focus to the location under the mouse event.
                actions().toggleMachineCodeBreakpoint().perform();
            }
        }

        @Override
        protected InspectorPopupMenu getPopupMenu(int row, int col, MouseEvent mouseEvent) {
            if (col == ObjectColumnKind.TAG.ordinal()) {
                final InspectorPopupMenu menu = new InspectorPopupMenu();
                final MachineCodeTableModel machineCodeTableModel = (MachineCodeTableModel) getModel();
                final MaxCodeLocation codeLocation = machineCodeTableModel.rowToLocation(row);
                menu.add(actions().debugRunToInstructionWithBreakpoints(codeLocation, "Run to this instruction"));
                menu.add(actions().debugRunToInstruction(codeLocation, "Run to this instruction (ignoring breakpoints)"));
                menu.add(actions().toggleMachineCodeBreakpoint(codeLocation, "Toggle breakpoint (double-click)"));
                menu.add(actions().setMachineCodeBreakpoint(codeLocation, "Set breakpoint"));
                menu.add(actions().removeMachineCodeBreakpoint(codeLocation, "Unset breakpoint"));
                menu.add(views().activateSingletonViewAction(ViewKind.DEBUG_INFO));
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
                final MachineCodeTableModel machineCodeTableModel = (MachineCodeTableModel) getModel();
                if (selectedRow >= 0 && selectedRow < machineCodeTableModel.getRowCount()) {
                    focus().setCodeLocation(machineCodeTableModel.rowToLocation(selectedRow));
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
                final Address machineCodeInstructionAddress = focus().codeLocation().address();
                if (machineCode().contains(machineCodeInstructionAddress)) {
                    final MachineCodeTableModel model = (MachineCodeTableModel) getModel();
                    final int row = model.findRow(machineCodeInstructionAddress);
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

        public InspectorView getView() {
            return view;
        }

    }

    private final class MachineCodeTableColumnModel extends InspectorTableColumnModel<MachineCodeColumnKind> {

        private MachineCodeTableColumnModel(MachineCodeViewPreferences viewPreferences) {
            super(MachineCodeColumnKind.values().length, viewPreferences);
            final Address startAddress = tableModel.machineCode.getMachineCodeInfo().length() == 0 ? Address.zero() : tableModel.rowToInstruction(0).address;
            addColumn(MachineCodeColumnKind.TAG, new TagRenderer(inspection), null);
            addColumn(MachineCodeColumnKind.NUMBER, new NumberRenderer(), null);
            addColumn(MachineCodeColumnKind.ADDRESS, new AddressRenderer(startAddress), null);
            addColumn(MachineCodeColumnKind.POSITION, new PositionRenderer(startAddress), null);
            addColumn(MachineCodeColumnKind.LABEL, new LabelRenderer(startAddress), null);
            addColumn(MachineCodeColumnKind.INSTRUCTION, new InstructionRenderer(inspection), null);
            addColumn(MachineCodeColumnKind.OPERANDS, operandsRenderer, null);
            addColumn(MachineCodeColumnKind.SOURCE_LINE, sourceLineRenderer, null);
            addColumn(MachineCodeColumnKind.BYTES, new BytesRenderer(inspection), null);
        }
    }

    /**
     * Data model representing a block of disassembled code, one row per instruction.
     */
    private final class MachineCodeTableModel extends InspectorTableModel {

        final MaxMachineCodeRoutine machineCode;

        public MachineCodeTableModel(Inspection inspection, MaxMachineCodeRoutine machineCode) {
            super(inspection);
            assert machineCode != null;
            this.machineCode = machineCode;
        }

        public int getColumnCount() {
            return MachineCodeColumnKind.values().length;
        }

        public int getRowCount() {
            return machineCode.getMachineCodeInfo().length();
        }

        public Object getValueAt(int row, int col) {
            final TargetCodeInstruction machineCodeInstruction = rowToInstruction(row);
            switch (MachineCodeColumnKind.values()[col]) {
                case TAG:
                    return null;
                case NUMBER:
                    return row;
                case ADDRESS:
                    return machineCodeInstruction.address;
                case POSITION:
                    return machineCodeInstruction.position;
                case LABEL:
                    final String label = machineCodeInstruction.label;
                    return label != null ? label + ":" : "";
                case INSTRUCTION:
                    return machineCodeInstruction.mnemonic;
                case OPERANDS:
                    return machineCodeInstruction.operands;
                case SOURCE_LINE:
                    return "";
                case BYTES:
                    return machineCodeInstruction.bytes;
                default:
                    throw new RuntimeException("Column out of range: " + col);
            }
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            switch (MachineCodeColumnKind.values()[col]) {
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
            return machineCode.getMachineCodeInfo().instruction(row);
        }

        public MaxCodeLocation rowToLocation(int row) {
            return machineCode.getMachineCodeInfo().instructionLocation(row);
        }

        /**
         * @param address a code address in the VM.
         * @return the row in this block of code containing an instruction starting at the address, -1 if none.
         */
        public int findRow(Address address) {
            final MaxMachineCodeInfo machineCodeInfo = machineCode.getMachineCodeInfo();
            for (int row = 0; row < machineCodeInfo.length(); row++) {
                final TargetCodeInstruction machineCodeInstruction = machineCodeInfo.instruction(row);
                if (machineCodeInstruction.address.equals(address)) {
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
        final InspectorStyle style = preference().style();
        return isInstructionPointer(row) ? style.debugIPTextColor() : (isCallReturn(row) ? style.debugCallReturnTextColor() : null);
    }

    private void setBorderForRow(JComponent component, int row) {
        if (machineCodeInfo().isBytecodeBoundary(row)) {
            component.setBorder(preference().style().defaultPaneTopBorder());
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
            component.setBackground(preference().style().searchMatchedBackground());
        } else if (machineCodeInfo().isSafepoint(row)) {
            component.setOpaque(true);
            component.setBackground(safepointBackgroundColor);
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
            final InspectorStyle style = preference().style();
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
                    setIcon(style.debugIPTagIcon());
                    setForeground(style.debugIPTagColor());
                } else {
                    setIcon(style.debugCallReturnTagIcon());
                    setForeground(style.debugCallReturnTagColor());
                }
            } else {
                setIcon(null);
                setForeground(null);
            }
            setText(rowToTagText(row));
            final MaxBreakpoint machineCodeBreakpoint = getMachineCodeBreakpointAtRow(row);
            if (machineCodeBreakpoint != null) {
                toolTipSB.append("<br>breakpoint set @ ");
                toolTipSB.append(machineCodeBreakpoint.codeLocation().address().to0xHexString());
                toolTipSB.append(machineCodeBreakpoint.isEnabled() ? ", enabled" : ", disabled");
                if (machineCodeBreakpoint.isEnabled()) {
                    setBorder(style.debugEnabledMachineCodeBreakpointTagBorder());
                } else {
                    setBorder(style.debugDisabledMachineCodeBreakpointTagBorder());
                }
            } else if (machineCodeInfo().isBytecodeBoundary(row)) {
                setBorder(style.defaultPaneTopBorder());
            } else {
                setBorder(null);
            }
            setWrappedToolTipHtmlText(toolTipSB.toString());
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

        public Component getTableCellRendererComponent(JTable table, Object ignore, boolean isSelected, boolean hasFocus, int row, int column) {
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
            if (value == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
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
            if (value == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
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
            final String label = (String) value;
            final Integer position = (Integer) tableModel.getValueAt(row, MachineCodeColumnKind.POSITION.ordinal());
            setLocation(label, position);
            setWrappedToolTipHtmlText(tableModel.getRowDescription(row));
            final InspectorStyle style = preference().style();
            setFont(style.defaultFont());
            setBackgroundForRow(this, row);
            //setForeground(getRowTextColor(row));

            if (isInstructionPointer(row)) {
                setForeground(style.debugIPTextColor());
            } else if (isCallReturn(row)) {
                setForeground(style.debugCallReturnTextColor());
            } else {
                setForeground(null);
            }
            setBorderForRow(this, row);
            return this;
        }
    }

    private final class InstructionRenderer extends MachineCodeLabel implements TableCellRenderer {
        InstructionRenderer(Inspection inspection) {
            super(inspection, "");
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            final String mnemonic = (String) value;
            setBackgroundForRow(this, row);
            setForeground(cellForegroundColor(row, column));
            setText(mnemonic);
            setWrappedToolTipHtmlText(tableModel.getRowDescription(row) + "<br>ISA = " + isa.name());
            setBorderForRow(this, row);
            return this;
        }
    }

    private interface LiteralRenderer {
        WordValueLabel render(Inspection inspection, String literalLoadText, Address literalAddress);
    }

    static final LiteralRenderer AMD64_LITERAL_RENDERER = new LiteralRenderer() {
        public WordValueLabel render(Inspection inspection, String literalLoadText, final Address literalAddress) {
            final WordValueLabel wordValueLabel = new WordValueLabel(inspection, WordValueLabel.ValueMode.LITERAL_REFERENCE, null, true) {
                @Override
                public Value fetchValue() {
                    return vm().memoryIO().readWordValue(literalAddress);
                }
            };
            wordValueLabel.setTextPrefix(literalLoadText.substring(0, literalLoadText.indexOf("[")).trim());
            wordValueLabel.setToolTipSuffix(" from RIP " + literalLoadText.substring(literalLoadText.indexOf("["), literalLoadText.length()));
            wordValueLabel.setWordDataFont(inspection.preference().style().defaultBoldFont());
            wordValueLabel.updateText();
            return wordValueLabel;
        }
    };

    static final LiteralRenderer SPARC_LITERAL_RENDERER = new LiteralRenderer() {
        public WordValueLabel render(Inspection inspection, String literalLoadText, final Address literalAddress) {
            final WordValueLabel wordValueLabel = new WordValueLabel(inspection, WordValueLabel.ValueMode.LITERAL_REFERENCE, null, true) {
                @Override
                public Value fetchValue() {
                    return vm().memoryIO().readWordValue(literalAddress);
                }
            };
            wordValueLabel.setTextSuffix(literalLoadText.substring(literalLoadText.indexOf(",")));
            wordValueLabel.setToolTipSuffix(" from " + literalLoadText.substring(0, literalLoadText.indexOf(",")));
            wordValueLabel.setWordDataFont(inspection.preference().style().defaultBoldFont());
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

        private CiCodePos lastCodePos;

        SourceLineRenderer() {
            super(JTableMachineCodeViewer.this.inspection(), null);
            addMouseListener(new InspectorMouseClickAdapter(inspection()) {
                @Override
                public void procedure(final MouseEvent mouseEvent) {
                    final CiCodePos frame = lastCodePos;
                    if (frame != null) {
                        final InspectorPopupMenu menu = new InspectorPopupMenu();
                        for (CiCodePos location = frame; location != null; location = location.caller) {
                            final StackTraceElement stackTraceElement = location.method.toStackTraceElement(location.bci);
                            final String fileName = stackTraceElement.getFileName();
                            if (fileName != null) {
                                final int lineNumber = stackTraceElement.getLineNumber();
                                if (lineNumber > 0) {
                                    if (vm().findJavaSourceFile(((ClassMethodActor) location.method).codeAttribute().cp.holder()) != null) {
                                        final CiCodePos codePosCopy = location;
                                        menu.add(new AbstractAction("Open " + fileName + " at line " + lineNumber) {
                                            public void actionPerformed(ActionEvent e) {
                                                inspection().viewSourceExternally(codePosCopy);
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

        public Component getTableCellRendererComponent(JTable table, Object ignore, boolean isSelected, boolean hasFocus, int row, int column) {
            CiDebugInfo debugInfo = machineCodeInfo().debugInfoAt(row);
            final CiCodePos codePos = debugInfo == null ? null : debugInfo.codePos;
            setText("");
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>");
            setWrappedToolTipHtmlText("Source location not available");
            setBackgroundForRow(this, row);
            if (codePos != null) {
                final StackTraceElement stackTraceElement = codePos.method.toStackTraceElement(codePos.bci);
                final StringBuilder stackTrace = new StringBuilder("<table cellpadding=\"1%\"><tr><td></td><td>").append(toolTipText(stackTraceElement)).append("</td></tr>");
                StackTraceElement top = stackTraceElement;
                for (CiCodePos caller = codePos.caller; caller != null; caller = caller.caller) {
                    StackTraceElement parentSTE = caller.method.toStackTraceElement(caller.bci);
                    stackTrace.append("<tr><td>--&gt;&nbsp;</td><td>").append(toolTipText(parentSTE)).append("</td></tr>");
                    top = parentSTE;
                }
                setWrappedToolTipHtmlText("Source location = " + stackTrace.append("</table>").toString());
                setText(String.valueOf(top.getLineNumber()));
            }
            lastCodePos = codePos;
            setBorderForRow(this, row);
            return this;
        }
    }

    private final class OperandsRenderer implements TableCellRenderer, Prober {
        private InspectorLabel[] inspectorLabels = new InspectorLabel[machineCodeInfo().length()];
        private MachineCodeLabel machineCodeLabel = new MachineCodeLabel(inspection, "");
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
            machineCodeLabel.redisplay();
        }

        public Component getTableCellRendererComponent(JTable table, Object ignore, boolean isSelected, boolean hasFocus, int row, int column) {
            InspectorLabel renderer = inspectorLabels[row];
            if (renderer == null) {
                final TargetCodeInstruction machineCodeInstruction = tableModel.rowToInstruction(row);
                final String text = machineCodeInstruction.operands;
                if (machineCodeInstruction.targetAddress != null && !machineCode().contains(machineCodeInstruction.targetAddress)) {
                    final WordValueLabel wordValueLabel = new WordValueLabel(inspection, WordValueLabel.ValueMode.CALL_ENTRY_POINT, machineCodeInstruction.targetAddress, table, true);
                    wordValueLabel.setToolTipPrefix(tableModel.getRowDescription(row) + ": operand = ");
                    wordValueLabel.setWordDataFont(inspection.preference().style().defaultBoldFont());
                    renderer = wordValueLabel;
                    inspectorLabels[row] = renderer;
                } else if (machineCodeInstruction.literalSourceAddress != null) {
                    final Address literalAddress = machineCodeInstruction.literalSourceAddress.asAddress();
                    renderer = literalRenderer.render(inspection, text, literalAddress);
                    renderer.setToolTipPrefix(tableModel.getRowDescription(row) + ": operand = ");
                    inspectorLabels[row] = renderer;
                } else {
                    MaxMachineCodeInfo machineCodeInfo = machineCodeInfo();
                    final RiMethod callee = machineCodeInfo.calleeAt(row);
                    if (machineCodeInstruction.mnemonic.contains("call") && callee != null) {
                        renderer = new TextLabel(inspection, CiUtil.format("%h.%n()", callee));
                        renderer.setToolTipPrefix(tableModel.getRowDescription(row) + ":");
                        renderer.setToolTipSuffix("<br>" + CiUtil.format("%r %H.%n(%p)", callee));
                        renderer.setWrappedToolTipHtmlText("<br>operands = " + text);
                        renderer.setForeground(cellForegroundColor(row, column));
                    } else {
                        if (machineCodeInfo.isNativeCall(row)) {
                            renderer = new TextLabel(inspection, "<native function>");
                            renderer.setToolTipPrefix(tableModel.getRowDescription(row) + ":");
                            renderer.setWrappedToolTipHtmlText("<br>operands = " + text);
                            renderer.setForeground(cellForegroundColor(row, column));
                        } else {
                            renderer = machineCodeLabel;
                            renderer.setToolTipPrefix(tableModel.getRowDescription(row) + ":");
                            renderer.setText(text);
                            renderer.setWrappedToolTipHtmlText("<br>operands = " + text);
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
            if (value == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            final byte[] bytes = (byte[]) value;
            setBackgroundForRow(this, row);
            setForeground(cellForegroundColor(row, column));
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>as bytes = ");
            setValue(bytes);
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

