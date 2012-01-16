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
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.max.ins.*;
import com.sun.max.ins.constant.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;

/**
 * A table-based viewer for an (immutable) block of bytecodes.
 * Supports visual effects for execution state, and permits user selection
 * of instructions for various purposes (e.g. set breakpoint)
 */
public class JTableBytecodeViewer extends BytecodeViewer {

    /** Maximum literal string length displayed directly in operand field. */
    public static final int MAX_BYTECODE_OPERAND_DISPLAY = 15;

    private final Inspection inspection;
    private final BytecodeTable table;
    private final BytecodeTableModel tableModel;
    private final BytecodeViewPreferences instanceViewPreferences;

    public JTableBytecodeViewer(Inspection inspection, MethodView parent, TeleClassMethodActor teleClassMethodActor, MaxCompilation compilation) {
        super(inspection, parent, teleClassMethodActor, compilation);
        this.inspection = inspection;
        tableModel = new BytecodeTableModel(inspection, bytecodeInstructions());
        instanceViewPreferences = new BytecodeViewPreferences(BytecodeViewPreferences.globalPreferences(inspection())) {
            @Override
            public void setIsVisible(BytecodeColumnKind columnKind, boolean visible) {
                super.setIsVisible(columnKind, visible);
                table.getInspectorTableColumnModel().setColumnVisible(columnKind.ordinal(), visible);
                JTableColumnResizer.adjustColumnPreferredWidths(table);
                refresh(true);
            }
            @Override
            public void setOperandDisplayMode(PoolConstantLabel.Mode mode) {
                super.setOperandDisplayMode(mode);
                tableModel.fireTableDataChanged();
            }
        };
        final BytecodeTableColumnModel tableColumnModel = new BytecodeTableColumnModel(instanceViewPreferences);
        table = new BytecodeTable(inspection, tableModel, tableColumnModel);
        createView();
    }

    @Override
    protected void createView() {
        super.createView();

        // Set up toolbar
        // TODO (mlvdv) implement remaining debugging controls in Bytecodes view
        // the disabled ones haven't been adapted for bytecode-based debugging
        JButton button = new InspectorButton(inspection, actions().toggleBytecodeBreakpoint());
        button.setToolTipText(button.getText());
        button.setText(null);
        final InspectorStyle style = preference().style();
        button.setIcon(style.debugToggleBreakpointbuttonIcon());
        button.setEnabled(false);
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugStepOver());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style.debugStepOverButtonIcon());
        button.setEnabled(false);
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugSingleStep());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style.debugStepInButtonIcon());
        button.setEnabled(false);
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugReturnFromFrame());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style.debugStepOutButtonIcon());
        button.setEnabled(haveMachineCodeAddresses());
        toolBar().add(button);

        button = new InspectorButton(inspection, actions().debugRunToSelectedInstruction());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style.debugRunToCursorButtonIcon());
        button.setEnabled(haveMachineCodeAddresses());
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

        toolBar().add(new TextLabel(inspection(), "Bytecodes"));

        toolBar().add(Box.createHorizontalGlue());

        addSearchButton();

        addActiveRowsButton();

        final JButton viewOptionsButton = new InspectorButton(inspection, new AbstractAction("View...") {
            public void actionPerformed(ActionEvent actionEvent) {
                final BytecodeViewPreferences globalPreferences = BytecodeViewPreferences.globalPreferences(inspection());
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<BytecodeColumnKind>(inspection(), "Bytecodes View Options", instanceViewPreferences, globalPreferences);
            }
        });
        viewOptionsButton.setToolTipText("Bytecodes view options");
        viewOptionsButton.setText(null);
        viewOptionsButton.setIcon(style.generalPreferencesIcon());
        toolBar().add(viewOptionsButton);

        toolBar().add(Box.createHorizontalGlue());
        addCodeViewCloseButton();

        final JScrollPane scrollPane = new InspectorScrollPane(inspection, table);
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
        final int bci = tableModel.rowToInstruction(row).bci;
        focus().setCodeLocation(vm().codeLocationFactory().createBytecodeLocation(teleClassMethodActor(), bci, "bytecode view set focus"), false);
    }

    @Override
    protected RowTextMatcher getRowTextSearcher() {
        return new TableRowTextMatcher(inspection, table);
    }

   /**
     * Global code selection has changed.
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

    // TODO (mlvdv) Extract the table class from the viewer
    private final class BytecodeTable extends InspectorTable {

        BytecodeTable(Inspection inspection, InspectorTableModel tableModel, InspectorTableColumnModel tableColumnModel) {
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
        public void valueChanged(ListSelectionEvent e) {
            // The selection in the table has changed; might have happened via user action (click, arrow) or
            // as a side effect of a focus change.
            super.valueChanged(e);
            if (!e.getValueIsAdjusting()) {
                final int selectedRow = getSelectedRow();
                final BytecodeTableModel bytecodeTableModel = (BytecodeTableModel) getModel();
                if (selectedRow >= 0 && selectedRow < bytecodeTableModel.getRowCount()) {
                    final BytecodeInstruction bytecodeInstruction = bytecodeTableModel.rowToInstruction(selectedRow);
                    final Address machineCodeFirstAddress = bytecodeInstruction.machineCodeFirstAddress;
                    final int bci = bytecodeInstruction.bci;
                    if (machineCodeFirstAddress.isZero()) {
                        focus().setCodeLocation(vm().codeLocationFactory().createBytecodeLocation(teleClassMethodActor(), bci, "bytecode view"));
                    } else {
                        try {
                            focus().setCodeLocation(vm().codeLocationFactory().createMachineCodeLocation(machineCodeFirstAddress, teleClassMethodActor(), bci, "bytecode view"), true);
                        } catch (InvalidCodeAddressException e1) {
                        }
                    }
                }
            }
        }

        /**
         * {@inheritDoc}.
         * <br>
         * color text specially if the IP or a call return site is at this row.
         */
        @Override
        public Color cellForegroundColor(int row, int col) {
            final InspectorStyle style = preference().style();
            return isInstructionPointer(row) ? style.debugIPTextColor() : (isCallReturn(row) ? style.debugCallReturnTextColor() : null);
        }

        /**
         * @param col TODO
         * @return Color to be used for the background of all row labels; may have special overrides in future, as for compiled code
         */
        public Color cellBackgroundColor(int row, int col) {
            final int[] searchMatchingRows = getSearchMatchingRows();
            if (searchMatchingRows != null) {
                for (int matchingRow : searchMatchingRows) {
                    if (row == matchingRow) {
                        return preference().style().searchMatchedBackground();
                    }
                }
            }
            return table.getBackground();
        }

        public boolean updateCodeFocus(MaxCodeLocation codeLocation) {
            final int oldSelectedRow = getSelectedRow();
            final BytecodeTableModel model = (BytecodeTableModel) getModel();
            int focusRow = -1;
            if (codeLocation.hasTeleClassMethodActor()) {
                if (codeLocation.teleClassMethodActor().classMethodActor() == teleClassMethodActor().classMethodActor()) {
                    focusRow = model.findRowAtBCI(codeLocation.bci());
                }
            } else if (codeLocation.hasMethodKey()) {
                // Shouldn't happen, but...
                if (codeLocation.methodKey().equals(methodKey())) {
                    focusRow = model.findRowAtBCI(0);
                }
            } else if (codeLocation.hasAddress()) {
                if (compilation() != null && compilation().contains(codeLocation.address())) {
                    focusRow = model.findRow(codeLocation.address());
                }
            }
            if (focusRow >= 0) {
                // View contains the focus; ensure it is selected and visible
                if (focusRow != oldSelectedRow) {
                    updateSelection(focusRow);
                }
                scrollToRows(focusRow, focusRow);
                return true;
            }
            // View doesn't contain the focus; clear any old selection
            if (oldSelectedRow >= 0) {
                clearSelection();
            }
            return false;
        }
    }

    private final class BytecodeTableColumnModel extends InspectorTableColumnModel<BytecodeColumnKind> {

        BytecodeTableColumnModel(BytecodeViewPreferences instanceViewPreferences) {
            super(BytecodeColumnKind.values().length, instanceViewPreferences);
            addColumn(BytecodeColumnKind.TAG, new TagRenderer(inspection), null);
            addColumn(BytecodeColumnKind.NUMBER, new NumberRenderer(), null);
            addColumn(BytecodeColumnKind.BCI, new BCIRenderer(), null);
            addColumn(BytecodeColumnKind.INSTRUCTION, new InstructionRenderer(), null);
            addColumn(BytecodeColumnKind.OPERAND1, new OperandRenderer(), null);
            addColumn(BytecodeColumnKind.OPERAND2, new OperandRenderer(), null);
            addColumn(BytecodeColumnKind.SOURCE_LINE, new SourceLineRenderer(), null);
            addColumn(BytecodeColumnKind.BYTES, new BytesRenderer(), null);
        }
    }

    private final class BytecodeTableModel extends InspectorTableModel {

        private List<BytecodeInstruction> bytecodeInstructions;

        public BytecodeTableModel(Inspection inspection, List<BytecodeInstruction> bytecodeInstructions) {
            super(inspection);
            this.bytecodeInstructions = bytecodeInstructions;
        }

        public int getColumnCount() {
            return BytecodeColumnKind.values().length;
        }

        public int getRowCount() {
            return bytecodeInstructions().size();
        }

        public Object getValueAt(int row, int col) {
            final BytecodeInstruction instruction = rowToInstruction(row);
            switch (BytecodeColumnKind.values()[col]) {
                case TAG:
                    return null;
                case NUMBER:
                    return row;
                case BCI:
                    return new Integer(instruction.bci);
                case INSTRUCTION:
                    return instruction.opcode;
                case OPERAND1:
                    return instruction.operand1;
                case OPERAND2:
                    return instruction.operand2;
                case SOURCE_LINE:
                    return new CiCodePos(null, teleClassMethodActor().classMethodActor(), instruction.bci);
                case BYTES:
                    return instruction.instructionBytes;
                default:
                    throw new RuntimeException("Column out of range: " + col);
            }
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            switch (BytecodeColumnKind.values()[col]) {
                case TAG:
                    return Object.class;
                case NUMBER:
                    return Integer.class;
                case BCI:
                    return Integer.class;
                case INSTRUCTION:
                    return int.class;
                case OPERAND1:
                case OPERAND2:
                case SOURCE_LINE:
                    return Object.class;
                case BYTES:
                    return byte[].class;
                default:
                    throw new RuntimeException("Column out of range: " + col);
            }
        }

        @Override
        public String getRowDescription(int row) {
            return "Instruction " + row + " (" + Bytecodes.nameOf(rowToInstruction(row).opcode) + ")";
        }

        public BytecodeInstruction rowToInstruction(int row) {
            return bytecodeInstructions.get(row);
        }

        /**
         * @param bci bytecode index: a position (in bytes) in this block of bytecodes
         * @return the row in this block of bytecodes containing an instruction starting at this position, -1 if none
         */
        public int findRowAtBCI(int bci) {
            for (BytecodeInstruction instruction : bytecodeInstructions) {
                if (instruction.bci == bci) {
                    return instruction.row;
                }
            }
            return -1;
        }

        /**
         * @param address a code address in the VM
         * @return the row in this block of bytecodes containing an
         *  instruction whose associated compiled code starts at the address, -1 if none.
         */
        public int findRow(Address address) {
            if (haveMachineCodeAddresses()) {
                for (BytecodeInstruction instruction : bytecodeInstructions) {
                    int row = instruction.row;
                    if (rowContainsAddress(row, address)) {
                        return row;
                    }
                    row++;
                }
            }
            return -1;
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
        } else {
            component.setOpaque(false);
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
            final MaxBreakpoint bytecodeBreakpoint = getBytecodeBreakpointAtRow(row);
            final List<MaxBreakpoint> machineCodeBreakpoints = getMachineCodeBreakpointsAtRow(row);
            if (bytecodeBreakpoint != null) {
                toolTipSB.append("<br>breakpont set @");
                toolTipSB.append(bytecodeBreakpoint);
                toolTipSB.append("; ");
                toolTipSB.append(bytecodeBreakpoint.isEnabled() ? ", enabled" : ", disabled");
                if (bytecodeBreakpoint.isEnabled()) {
                    setBorder(style.debugEnabledBytecodeBreakpointTagBorder());
                } else {
                    setBorder(style.debugDisabledBytecodeBreakpointTagBorder());
                }
            } else if (machineCodeBreakpoints.size() > 0) {
                boolean enabled = false;
                for (MaxBreakpoint machineCodeBreakpoint : machineCodeBreakpoints) {
                    toolTipSB.append(machineCodeBreakpoint);
                    toolTipSB.append("; ");
                    enabled = enabled || machineCodeBreakpoint.isEnabled();
                }
                if (enabled) {
                    setBorder(style.debugEnabledMachineCodeBreakpointTagBorder());
                } else {
                    setBorder(style.debugDisabledMachineCodeBreakpointTagBorder());
                }
            } else {
                setBorder(null);
            }
            setWrappedToolTipHtmlText(toolTipSB.toString());
            setBackgroundForRow(this, row);
            return this;
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

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final BytecodeTable bytecodeTable = (BytecodeTable) table;
            setValue(row);
            setToolTipText(tableModel.getRowDescription(row));
            setBackgroundForRow(this, row);
            setForeground(bytecodeTable.cellForegroundColor(row, col));
            return this;
        }
    }

    private final class BCIRenderer extends LocationLabel.AsPosition implements TableCellRenderer {
        private int bci;

        public BCIRenderer() {
            super(inspection, 0);
            setToolTipPrefix("Instruction");
            bci = 0;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final BytecodeTable bytecodeTable = (BytecodeTable) table;
            final Integer bci = (Integer) value;
            if (bci == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            if (this.bci != bci) {
                this.bci = bci;
                setValue(bci);
            }
            setToolTipPrefix(tableModel.getRowDescription(row) + " location<br>");
            setToolTipSuffix(" bytes from beginning");
            setBackgroundForRow(this, row);
            setForeground(bytecodeTable.cellForegroundColor(row, col));
            return this;
        }
    }

    private final class InstructionRenderer extends BytecodeMnemonicLabel implements TableCellRenderer {

        public InstructionRenderer() {
            super(inspection, -1);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final BytecodeTable bytecodeTable = (BytecodeTable) table;
            if (value == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            final int opcode = (Integer) value;
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>");
            setValue(opcode);
            setBackgroundForRow(this, row);
            setForeground(bytecodeTable.cellForegroundColor(row, col));
            return this;
        }
    }

    private final class OperandRenderer implements  TableCellRenderer, Prober {

        public OperandRenderer() {
        }

        public Component getTableCellRendererComponent(JTable table, Object tableValue, boolean isSelected, boolean hasFocus, int row, int col) {
            final BytecodeTable bytecodeTable = (BytecodeTable) table;
            InspectorLabel renderer = null;
            if (tableValue instanceof InspectorLabel) {
                // BytecodePrinter returns a label component for simple values
                renderer = (InspectorLabel) tableValue;
                renderer.setToolTipPrefix(tableModel.getRowDescription(row) + " operand:<br>");
                renderer.setForeground(bytecodeTable.cellForegroundColor(row, col));
            } else if (tableValue instanceof Integer) {
                // BytecodePrinter returns index of a constant pool entry, when that's the operand
                final int index = ((Integer) tableValue).intValue();
                renderer =  PoolConstantLabel.make(inspection(), index, localConstantPool(), teleConstantPool(), instanceViewPreferences.operandDisplayMode());
                if (renderer.getForeground() == null) {
                    renderer.setForeground(bytecodeTable.cellForegroundColor(row, col));
                }
                renderer.setToolTipPrefix(tableModel.getRowDescription(row) + " operand:<br>Constant pool reference = ");
                renderer.setFont(preference().style().bytecodeOperandFont());
            } else if (tableValue == null) {
                return gui().getUnavailableDataTableCellRenderer();
            } else {
                InspectorError.unexpected("unrecognized table value at row=" + row + ", col=" + col);
            }
            setBackgroundForRow(renderer, row);
            return renderer;
        }

        public void redisplay() {
        }

        public void refresh(boolean force) {
        }
    }

    private final class SourceLineRenderer extends PlainLabel implements TableCellRenderer {
        private CiCodePos lastCodePos;
        SourceLineRenderer() {
            super(JTableBytecodeViewer.this.inspection(), null);
            addMouseListener(new InspectorMouseClickAdapter(inspection()) {
                @Override
                public void procedure(final MouseEvent mouseEvent) {
                    final CiCodePos codePos = lastCodePos;
                    if (codePos != null) {
                        inspection().viewSourceExternally(codePos);
                    }
                }
            });
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final CiCodePos codePos = (CiCodePos) value;
            if (codePos == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>");
            ClassMethodActor method = (ClassMethodActor) codePos.method;
            final String sourceFileName = method.holder().sourceFileName;
            final int lineNumber = method.sourceLineNumber(codePos.bci);
            if (sourceFileName != null && lineNumber >= 0) {
                setText(String.valueOf(lineNumber));
                setWrappedToolTipHtmlText("Source location =<br>" + sourceFileName + ":" + lineNumber);
            } else {
                setText("");
                setWrappedToolTipHtmlText("Source line not available");
            }
            setBackgroundForRow(this, row);
            lastCodePos = codePos;
            return this;
        }
    }

    private final class BytesRenderer extends DataLabel.ByteArrayAsHex implements TableCellRenderer {
        BytesRenderer() {
            super(inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setBackgroundForRow(this, row);
            final BytecodeTable bytecodeTable = (BytecodeTable) table;
            setForeground(bytecodeTable.cellForegroundColor(row, col));
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>as bytes = ");
            setValue((byte[]) value);
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
