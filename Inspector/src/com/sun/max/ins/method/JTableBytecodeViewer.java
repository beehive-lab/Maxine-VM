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
package com.sun.max.ins.method;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.constant.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;

/**
 * A table-based viewer for an (immutable) block of bytecodes.
 * Supports visual effects for execution state, and permits user selection
 * of instructions for various purposes (e.g. set breakpoint)
 *
 * @author Michael Van De Vanter
 */
public class JTableBytecodeViewer extends BytecodeViewer {

    /** Maximum literal string length displayed directly in operand field. */
    public static final int MAX_BYTECODE_OPERAND_DISPLAY = 15;

    private final Inspection inspection;
    private final BytecodeTable table;
    private final BytecodeTableModel model;
    private final BytecodeTableColumnModel columnModel;
    private final TableColumn[] columns;
    private PoolConstantLabel.Mode operandDisplayMode;

    public JTableBytecodeViewer(Inspection inspection, MethodInspector parent, TeleClassMethodActor teleClassMethodActor, TeleTargetMethod teleTargetMethod) {
        super(inspection, parent, teleClassMethodActor, teleTargetMethod);
        this.inspection = inspection;
        model = new BytecodeTableModel(bytecodeInstructions());
        columns = new TableColumn[BytecodeColumnKind.VALUES.length()];
        columnModel = new BytecodeTableColumnModel();
        table = new BytecodeTable(inspection, model, columnModel);
        operandDisplayMode = BytecodeViewerPreferences.globalPreferences(inspection()).operandDisplayMode();
        createView();
    }

    @Override
    protected void createView() {
        super.createView();

        // Set up toolbar
        // TODO (mlvdv) implement remaining debugging controls in Bytecode view
        // the disabled ones haven't been adapted for bytecode-based debugging
        JButton button = new InspectorButton(inspection, inspection.actions().toggleBytecodeBreakpoint());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugToggleBreakpointbuttonIcon());
        button.setEnabled(false);
        toolBar().add(button);

        button = new InspectorButton(inspection, inspection.actions().debugStepOver());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugStepOverButtonIcon());
        button.setEnabled(false);
        toolBar().add(button);

        button = new InspectorButton(inspection, inspection.actions().debugSingleStep());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugStepInButtonIcon());
        button.setEnabled(false);
        toolBar().add(button);

        button = new InspectorButton(inspection, inspection.actions().debugReturnFromFrame());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugStepOutButtonIcon());
        button.setEnabled(haveTargetCodeAddresses());
        toolBar().add(button);

        button = new InspectorButton(inspection, inspection.actions().debugRunToSelectedInstruction());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugRunToCursorButtonIcon());
        button.setEnabled(haveTargetCodeAddresses());
        toolBar().add(button);

        button = new InspectorButton(inspection, inspection.actions().debugResume());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugContinueButtonIcon());
        toolBar().add(button);

        button = new InspectorButton(inspection, inspection.actions().debugPause());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugPauseButtonIcon());
        toolBar().add(button);

        toolBar().add(Box.createHorizontalGlue());

        toolBar().add(new TextLabel(inspection(), "Bytecode"));

        toolBar().add(Box.createHorizontalGlue());

        addSearchButton();

        addActiveRowsButton();

        final JButton viewOptionsButton = new InspectorButton(inspection, new AbstractAction("View...") {
            public void actionPerformed(ActionEvent actionEvent) {
                final BytecodeViewerPreferences globalPreferences = BytecodeViewerPreferences.globalPreferences(inspection());
                new TableColumnVisibilityPreferences.Dialog<BytecodeColumnKind>(inspection(), "Bytecode View Options", columnModel.preferences(), globalPreferences);
            }
        });
        viewOptionsButton.setToolTipText("Bytecode view options");
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
        final int position = model.getBytecodeInstruction(row).position();
        inspection.focus().setCodeLocation(maxVM().createCodeLocation(teleClassMethodActor(), position), false);
    }

    @Override
    protected RowTextSearcher getRowTextSearcher() {
        return new TableRowTextSearcher(inspection, table);
    }

   /**
     * Global code selection has changed.
     */
    @Override
    public boolean updateCodeFocus(TeleCodeLocation teleCodeLocation) {
        return table.updateCodeFocus(teleCodeLocation);
    }

    private final class BytecodeTableModel extends AbstractTableModel {

        private AppendableIndexedSequence<BytecodeInstruction> bytecodeInstructions;

        public BytecodeTableModel(AppendableIndexedSequence<BytecodeInstruction> bytecodeInstructions) {
            this.bytecodeInstructions = bytecodeInstructions;
        }

        public int getColumnCount() {
            return BytecodeColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return bytecodeInstructions().length();
        }

        public Object getValueAt(int row, int col) {
            final BytecodeInstruction instruction = getBytecodeInstruction(row);
            switch (BytecodeColumnKind.VALUES.get(col)) {
                case TAG:
                    return null;
                case NUMBER:
                    return row;
                case POSITION:
                    return new Integer(instruction.position);
                case INSTRUCTION:
                    return instruction.opcode;
                case OPERAND1:
                    return instruction.operand1;
                case OPERAND2:
                    return instruction.operand2;
                case SOURCE_LINE:
                    return new BytecodeLocation(teleClassMethodActor().classMethodActor(), instruction.position);
                case BYTES:
                    return instruction.instructionBytes;
                default:
                    throw new RuntimeException("Column out of range: " + col);
            }
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            switch (BytecodeColumnKind.VALUES.get(col)) {
                case TAG:
                    return Object.class;
                case NUMBER:
                    return Integer.class;
                case POSITION:
                    return Integer.class;
                case INSTRUCTION:
                    return Bytecode.class;
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

        public BytecodeInstruction getBytecodeInstruction(int row) {
            return bytecodeInstructions.get(row);
        }

        /**
         * @param position a position (in bytes) in this block of bytecodes
         * @return the row in this block of bytecodes containing an instruction starting at this position, -1 if none
         */
        public int getRowAtPosition(int position) {
            for (BytecodeInstruction instruction : bytecodeInstructions) {
                if (instruction.position() == position) {
                    return instruction.row();
                }
            }
            return -1;
        }

        /**
         * @param address a code address in the VM
         * @return the row in this block of bytecodes containing an
         *  instruction whose associated compiled code starts at the address, -1 if none.
         */
        public int getRowAtAddress(Address address) {
            if (haveTargetCodeAddresses()) {
                for (BytecodeInstruction instruction : bytecodeInstructions) {
                    int row = instruction.row();
                    if (rowContainsAddress(row, address)) {
                        return row;
                    }
                    row++;
                }
            }
            return -1;
        }

    }

    private final class BytecodeTable extends InspectorTable {

        BytecodeTable(Inspection inspection, TableModel model, TableColumnModel tableColumnModel) {
            super(inspection, model, tableColumnModel);
            setFillsViewportHeight(true);
            setShowHorizontalLines(style().codeTableShowHorizontalLines());
            setShowVerticalLines(style().codeTableShowVerticalLines());
            setIntercellSpacing(style().codeTableIntercellSpacing());
            setRowHeight(style().codeTableRowHeight());
            setRowSelectionAllowed(true);
            setColumnSelectionAllowed(true);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            addMouseListener(new TableCellMouseClickAdapter(inspection(), this));
        }

        @Override
        public void paintChildren(Graphics g) {
            // Draw a box around the selected row in the table
            super.paintChildren(g);
            final int row = getSelectedRow();
            if (row >= 0) {
                g.setColor(style().debugSelectedCodeBorderColor());
                g.drawRect(0, row * getRowHeight(row), getWidth() - 1, getRowHeight(row) - 1);
            }
        }

        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new JTableHeader(getColumnModel()) {
                @Override
                public String getToolTipText(MouseEvent mouseEvent) {
                    final Point p = mouseEvent.getPoint();
                    final int index = getColumnModel().getColumnIndexAtX(p.x);
                    final int modelIndex = getColumnModel().getColumn(index).getModelIndex();
                    return BytecodeColumnKind.VALUES.get(modelIndex).toolTipText();
                }
            };
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
                    final BytecodeInstruction bytecodeInstruction = bytecodeTableModel.getBytecodeInstruction(selectedRow);
                    final Address targetCodeFirstAddress = bytecodeInstruction.targetCodeFirstAddress();
                    final int position = bytecodeInstruction.position();
                    inspection().focus().setCodeLocation(maxVM().createCodeLocation(targetCodeFirstAddress, teleClassMethodActor(), position), true);
                }
            }
        }

        public boolean updateCodeFocus(TeleCodeLocation teleCodeLocation) {
            final int oldSelectedRow = getSelectedRow();
            final BytecodeTableModel model = (BytecodeTableModel) getModel();
            if (teleCodeLocation.hasBytecodeLocation()) {
                final BytecodeLocation bytecodeLocation = teleCodeLocation.bytecodeLocation();
                if (bytecodeLocation.classMethodActor() == teleClassMethodActor().classMethodActor()) {
                    final int row = model.getRowAtPosition(bytecodeLocation.bytecodePosition());
                    if (row >= 0) {
                        if (row != oldSelectedRow) {
                            changeSelection(row, row, false, false);
                        }
                        scrollToRows(row, row);
                        return true;
                    }
                }
            } else if (teleCodeLocation.hasTargetCodeLocation()) {
                if (teleTargetMethod() != null && teleTargetMethod().targetCodeRegion().contains(teleCodeLocation.targetCodeInstructionAddresss())) {
                    final int row = model.getRowAtAddress(teleCodeLocation.targetCodeInstructionAddresss());
                    if (row >= 0) {
                        if (row != oldSelectedRow) {
                            changeSelection(row, row, false, false);
                        }
                        scrollToRows(row, row);
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

        public void redisplay() {
            // not used pending further refactoring
        }

        public void refresh(boolean force) {
            // not used pending further refactoring
        }
    }

    private final class BytecodeTableColumnModel extends DefaultTableColumnModel {

        private final BytecodeViewerPreferences preferences;

        BytecodeTableColumnModel() {
            preferences = new BytecodeViewerPreferences(BytecodeViewerPreferences.globalPreferences(inspection())) {
                @Override
                public void setIsVisible(BytecodeColumnKind columnKind, boolean visible) {
                    super.setIsVisible(columnKind, visible);
                    final int col = columnKind.ordinal();
                    if (visible) {
                        addColumn(columns[col]);
                    } else {
                        removeColumn(columns[col]);
                    }
                    JTableColumnResizer.adjustColumnPreferredWidths(table);
                    refresh(true);
                }
            };
            createColumn(BytecodeColumnKind.TAG, new TagRenderer());
            createColumn(BytecodeColumnKind.NUMBER, new NumberRenderer());
            createColumn(BytecodeColumnKind.POSITION, new PositionRenderer());
            createColumn(BytecodeColumnKind.INSTRUCTION, new InstructionRenderer());
            createColumn(BytecodeColumnKind.OPERAND1, new OperandRenderer());
            createColumn(BytecodeColumnKind.OPERAND2, new OperandRenderer());
            createColumn(BytecodeColumnKind.SOURCE_LINE, new SourceLineRenderer());
            createColumn(BytecodeColumnKind.BYTES, new BytesRenderer());
        }

        private void createColumn(BytecodeColumnKind columnKind, TableCellRenderer renderer) {
            final int col = columnKind.ordinal();
            columns[col] = new TableColumn(col, 0, renderer, null);
            columns[col].setHeaderValue(columnKind.label());
            columns[col].setMinWidth(columnKind.minWidth());
            if (preferences.isVisible(columnKind)) {
                addColumn(columns[col]);
            }
            columns[col].setIdentifier(columnKind);
        }

        public BytecodeViewerPreferences preferences() {
            return preferences;
        }
    }

    /**
     * @return a special color use for all text labels on the row, when either at an IP or Call Return; null otherwise.
     */
    private Color getSpecialRowTextColor(int row) {
        return isInstructionPointer(row) ? style().debugIPTextColor() : (isCallReturn(row) ? style().debugCallReturnTextColor() : null);
    }

    /**
     * @return the default color to be used for all text labels on the row
     */
    private Color getRowTextColor(int row) {
        final Color specialColor = getSpecialRowTextColor(row);
        if (specialColor != null) {
            return specialColor;
        }
        return style().bytecodeColor();
    }

    /**
     * @return Color to be used for the background of all row labels; may have special overrides in future, as for Target Code
     */
    private Color getRowBackgroundColor(int row) {
        final IndexedSequence<Integer> searchMatchingRows = getSearchMatchingRows();
        if (searchMatchingRows != null) {
            for (int matchingRow : searchMatchingRows) {
                if (row == matchingRow) {
                    return style().searchMatchedBackground();
                }
            }
        }
        return style().bytecodeBackgroundColor();
    }

    private final class TagRenderer extends JLabel implements TableCellRenderer, TextSearchable, Prober {
        public Component getTableCellRendererComponent(JTable table, Object ignore, boolean isSelected, boolean hasFocus, int row, int col) {
            setOpaque(true);
            setBackground(getRowBackgroundColor(row));
            final StringBuilder toolTipText = new StringBuilder(100);
            final StackFrameInfo stackFrameInfo = stackFrameInfo(row);
            if (stackFrameInfo != null) {
                toolTipText.append("Stack ");
                toolTipText.append(stackFrameInfo.position());
                toolTipText.append(":  0x");
                toolTipText.append(stackFrameInfo.frame().instructionPointer.toHexString());
                toolTipText.append(" thread=");
                toolTipText.append(inspection.nameDisplay().longName(stackFrameInfo.thread()));
                toolTipText.append("; ");
                if (stackFrameInfo.frame().isTopFrame()) {
                    setIcon(style().debugIPTagIcon());
                    setForeground(style().debugIPTagColor());
                } else {
                    setIcon(style().debugCallReturnTagIcon());
                    setForeground(style().debugCallReturnTagColor());
                }
            } else {
                setIcon(style().debugDefaultTagIcon());
                setForeground(style().debugDefaultTagColor());
            }
            setText(rowToTagText(row));
            final TeleBytecodeBreakpoint teleBytecodeBreakpoint = getBytecodeBreakpointAtRow(row);
            final Sequence<TeleTargetBreakpoint> teleTargetBreakpoints = getTargetBreakpointsAtRow(row);
            if (teleBytecodeBreakpoint != null) {
                toolTipText.append(teleBytecodeBreakpoint);
                toolTipText.append("; ");
                if (teleBytecodeBreakpoint.isEnabled()) {
                    setBorder(style().debugEnabledBytecodeBreakpointTagBorder());
                } else {
                    setBorder(style().debugDisabledBytecodeBreakpointTagBorder());
                }
            } else if (teleTargetBreakpoints.length() > 0) {
                boolean enabled = false;
                for (TeleTargetBreakpoint teleTargetBreakpoint : teleTargetBreakpoints) {
                    toolTipText.append(teleTargetBreakpoint);
                    toolTipText.append("; ");
                    enabled = enabled || teleTargetBreakpoint.isEnabled();
                }
                if (enabled) {
                    setBorder(style().debugEnabledTargetBreakpointTagBorder());
                } else {
                    setBorder(style().debugDisabledTargetBreakpointTagBorder());
                }
            } else {
                setBorder(style().debugDefaultTagBorder());
            }
            setToolTipText(toolTipText.toString());
            return this;
        }

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

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(row);
            setToolTipText("Instruction no. " + row + "in method");
            setBackground(getRowBackgroundColor(row));
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class PositionRenderer extends LocationLabel.AsPosition implements TableCellRenderer {
        private int position;

        public PositionRenderer() {
            super(inspection, 0);
            position = 0;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Integer position = (Integer) value;
            if (this.position != position) {
                this.position = position;
                setValue(position);
                // TODO (mlvdv)  does this help make things more compact?
                setColumns(getText().length() + 1);
            }
            setBackground(getRowBackgroundColor(row));
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class InstructionRenderer extends BytecodeMnemonicLabel implements TableCellRenderer {

        public InstructionRenderer() {
            super(inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Bytecode opcode = (Bytecode) value;
            setValue(opcode);
            setBackground(getRowBackgroundColor(row));
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class OperandRenderer implements  TableCellRenderer, Prober {

        public OperandRenderer() {
        }

        public Component getTableCellRendererComponent(JTable table, Object tableValue, boolean isSelected, boolean hasFocus, int row, int col) {
            Component renderer = null;
            if (tableValue instanceof Component) {
                // BytecodePrinter returns a label component for simple values
                renderer = (Component) tableValue;
            } else if (tableValue instanceof Integer) {
                // BytecodePrinter returns index of a constant pool entry, when that's the operand
                final int index = ((Integer) tableValue).intValue();
                renderer =  PoolConstantLabel.make(inspection(), index, localConstantPool(), teleConstantPool(), operandDisplayMode);
                setFont(style().bytecodeOperandFont());
            } else {
                ProgramError.unexpected("unrecognized table value at row=" + row + ", col=" + col);
            }
            renderer.setBackground(getRowBackgroundColor(row));
            final Color specialForegroundColor = getSpecialRowTextColor(row);
            if (specialForegroundColor != null) {
                renderer.setForeground(specialForegroundColor);
            }
            return renderer;
        }

        public void redisplay() {
        }

        public void refresh(boolean force) {
        }
    }

    private final class SourceLineRenderer extends PlainLabel implements TableCellRenderer {
        private BytecodeLocation lastBytecodeLocation;
        SourceLineRenderer() {
            super(JTableBytecodeViewer.this.inspection(), null);
            addMouseListener(new InspectorMouseClickAdapter(inspection()) {
                @Override
                public void procedure(final MouseEvent mouseEvent) {
                    final BytecodeLocation bytecodeLocation = lastBytecodeLocation;
                    if (bytecodeLocation != null) {
                        inspection().viewSourceExternally(bytecodeLocation);
                    }
                }
            });
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final BytecodeLocation bytecodeLocation = (BytecodeLocation) value;
            final String sourceFileName = bytecodeLocation.sourceFileName();
            final int lineNumber = bytecodeLocation.sourceLineNumber();
            if (sourceFileName != null && lineNumber >= 0) {
                setText(String.valueOf(lineNumber));
                setToolTipText(sourceFileName + ":" + lineNumber);
            } else {
                setText("");
                setToolTipText("Source line not available");
            }
            setBackground(getRowBackgroundColor(row));
            lastBytecodeLocation = bytecodeLocation;
            return this;
        }
    }

    private final class BytesRenderer extends DataLabel.ByteArrayAsHex implements TableCellRenderer {
        BytesRenderer() {
            super(inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setBackground(getRowBackgroundColor(row));
            setForeground(getRowTextColor(row));
            setValue((byte[]) value);
            return this;
        }
    }

    @Override
    protected void updateView(boolean force) {
        for (TableColumn column : columns) {
            final Prober prober = (Prober) column.getCellRenderer();
            prober.refresh(force);
        }
    }

    @Override
    public void redisplay() {
        for (TableColumn column : columns) {
            final Prober prober = (Prober) column.getCellRenderer();
            prober.redisplay();
        }
        // TODO (mlvdv)  code view hack for style changes
        table.setRowHeight(style().codeTableRowHeight());
        invalidate();
        repaint();
    }

    @Override
    public void print(String name) {
        final MessageFormat header = new MessageFormat(name);
        final MessageFormat footer = new MessageFormat("Maxine: " + codeViewerKindName() + "  Printed: " + new Date() + " -- Page: {0, number, integer}");
        try {
            table.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (PrinterException printerException) {
            gui().errorMessage("Print failed: " + printerException.getMessage());
        }
    }
}
