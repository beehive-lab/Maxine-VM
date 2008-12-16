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

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.constant.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;

/**
 * A table-based viewer for an (immutable) section of {@link TargetCode} in the {@link TeleVM}.
 * Supports visual effects for execution state, and permits user selection
 * of instructions for various purposes (e.g. set breakpoint).
 *
 * @author Mick Jordan
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public class JTableTargetCodeViewer extends TargetCodeViewer {

    /**
     * Defines the columns supported by the inspector; the view includes one of each
     * kind.  The visibility of them, however, may be changed by the user.
     */
    private enum ColumnKind {
        TAG("Tag", "Tags:  IP, stack return, breakpoints", true, 20) {
            @Override
            public boolean canBeMadeInvisible() {
                return false;
            }
        },
        NUMBER("No.", "Index of instruction in the method", false, 15),
        ADDRESS("Addr.", "Memory address of target instruction start", false, -1),
        POSITION("Pos.", "Position in bytes of target instruction start", false, 20),
        LABEL("Label", "Labels synthesized during disassembly", true, -1),
        INSTRUCTION("Instr.", "Instruction mnemonic", true, -1),
        OPERANDS("Operands", "Instruction operands", true, -1),
        SOURCE_LINE("Line", "Line number in source code (may be approximate)", true, -1),
        BYTES("Bytes", "Instruction bytes", false, -1);

        private final String _label;
        private final String _toolTipText;
        private final boolean _defaultVisibility;
        private final int _minWidth;

        private ColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
            _label = label;
            _toolTipText = toolTipText;
            _defaultVisibility = defaultVisibility;
            _minWidth = minWidth;
            assert defaultVisibility || canBeMadeInvisible();
        }

        /**
         * @return text to appear in the column header
         */
        public String label() {
            return _label;
        }

        /**
         * @return text to appear in the column header's toolTip, null if none specified
         */
        public String toolTipText() {
            return _toolTipText;
        }

        /**
         * @return whether this column kind should be allowed to be made invisible.
         */
        public boolean canBeMadeInvisible() {
            return true;
        }

        /**
         * @return whether this column should be visible by default.
         */
        public boolean defaultVisibility() {
            return _defaultVisibility;
        }

        /**
         * @return minimum width allowed for this column when resized by user; -1 if none specified.
         */
        public int minWidth() {
            return _minWidth;
        }

        @Override
        public String toString() {
            return _label;
        }

        public static final IndexedSequence<ColumnKind> VALUES = new ArraySequence<ColumnKind>(values());
    }

    public static class Preferences extends TableColumnVisibilityPreferences<ColumnKind> {
        public Preferences(TableColumnVisibilityPreferences<ColumnKind> otherPreferences) {
            super(otherPreferences);
        }

        public Preferences(Inspection inspection) {
            super(inspection, "targetCodeInspectorPrefs", ColumnKind.class, ColumnKind.VALUES);
        }

        @Override
        protected boolean canBeMadeInvisible(ColumnKind columnType) {
            return columnType.canBeMadeInvisible();
        }

        @Override
        protected boolean defaultVisibility(ColumnKind columnType) {
            return columnType.defaultVisibility();
        }

        @Override
        protected String label(ColumnKind columnType) {
            return columnType.label();
        }
    }

    private static Preferences _globalPreferences;

    public static synchronized Preferences globalPreferences(Inspection inspection) {
        if (_globalPreferences == null) {
            _globalPreferences = new Preferences(inspection);
        }
        return _globalPreferences;
    }

    private final Inspection _inspection;
    private final JTable _table;
    private final MyTableModel _model;
    private final MyTableColumnModel _columnModel;
    private final TableColumn[] _columns;
    private final OperandsRenderer _operandsRenderer;
    private final SourceLineRenderer _sourceLineRenderer;

    public JTableTargetCodeViewer(Inspection inspection, MethodInspector parent, TeleTargetRoutine teleTargetRoutine) {
        super(inspection, parent, teleTargetRoutine);
        _inspection = inspection;
        _operandsRenderer = new OperandsRenderer();
        _sourceLineRenderer = new SourceLineRenderer();
        _model = new MyTableModel();
        _columns = new TableColumn[ColumnKind.VALUES.length()];
        _columnModel = new MyTableColumnModel();
        _table = new MyTable(_model, _columnModel);
        createView(teleVM().teleProcess().epoch());
    }

    @Override
    protected void createView(long epoch) {
        super.createView(epoch);

        _table.setOpaque(true);
        _table.setBackground(style().defaultBackgroundColor());
        _table.setFillsViewportHeight(true);
        _table.setShowHorizontalLines(false);
        _table.setShowVerticalLines(false);
        _table.setIntercellSpacing(new Dimension(0, 0));
        _table.setRowHeight(20);
        _table.setRowSelectionAllowed(true);
        _table.setColumnSelectionAllowed(true);
        _table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _table.addMouseListener(new MyInspectorMouseClickAdapter(_inspection));

        // Set up toolbar
        JButton button = new JButton(_inspection.actions().toggleTargetCodeBreakpoint());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugToggleBreakpointbuttonIcon());
        toolBar().add(button);

        button = new JButton(_inspection.actions().debugStepOver());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugStepOverButtonIcon());
        toolBar().add(button);

        button = new JButton(_inspection.actions().debugSingleStep());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugStepInButtonIcon());
        toolBar().add(button);

        button = new JButton(_inspection.actions().debugReturnFromFrame());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugStepOutButtonIcon());
        toolBar().add(button);

        button = new JButton(_inspection.actions().debugRunToInstruction());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugRunToCursorButtonIcon());
        toolBar().add(button);

        button = new JButton(_inspection.actions().debugResume());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugContinueButtonIcon());
        toolBar().add(button);

        button = new JButton(_inspection.actions().debugPause());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugPauseButtonIcon());
        toolBar().add(button);

        toolBar().add(Box.createHorizontalGlue());

        toolBar().add(new JLabel("Target Code"));

        toolBar().add(Box.createHorizontalGlue());

        addActiveRowsButton();

        addSearchButton();

        final JButton viewOptionsButton = new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                new TableColumnVisibilityPreferences.Dialog<ColumnKind>(inspection(), "TargetCode View Options", _columnModel.preferences(), globalPreferences(inspection()));
            }
        });
        viewOptionsButton.setText("View...");
        viewOptionsButton.setToolTipText("Target code view options");
        toolBar().add(viewOptionsButton);

        toolBar().add(Box.createHorizontalGlue());
        addCodeViewCloseButton();

        final JScrollPane scrollPane = new JScrollPane(_table);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(style().defaultBackgroundColor());
        add(scrollPane, BorderLayout.CENTER);

        refresh(epoch, true);
        JTableColumnResizer.adjustColumnPreferredWidths(_table);
    }

    @Override
    protected int getRowCount() {
        return _table.getRowCount();
    }

    @Override
    protected int getSelectedRow() {
        return _table.getSelectedRow();
    }

    @Override
    protected void setFocusAtRow(int row) {
        _inspection.focus().setCodeLocation(new TeleCodeLocation(teleVM(), targetCodeInstructionAt(row).address()), false);
    }

    @Override
    protected RowTextSearcher getRowTextSearcher() {
        return new TableRowTextSearcher(_inspection, _table);
    }

    /**
     * Global code selection has been set; return true iff the view contains selection.
     * Update even when the selection is set to the same value, because we want
     * that to force a scroll to make the selection visible.
     */
    @Override
    public boolean updateCodeFocus(TeleCodeLocation teleCodeLocation) {
        final int oldSelectedRow = getSelectedRow();
        if (teleCodeLocation.hasTargetCodeLocation()) {
            final Address targetCodeInstructionAddress = inspection().focus().codeLocation().targetCodeInstructionAddresss();
            if (teleTargetRoutine().targetCodeRegion().contains(targetCodeInstructionAddress)) {
                final int row = addressToRow(targetCodeInstructionAddress);
                if (row >= 0) {
                    if (row != oldSelectedRow) {
                        _table.getSelectionModel().setSelectionInterval(row, row);
                    }
                    scrollToRows(row, row);
                    return true;
                }
            }
        }
        if (oldSelectedRow >= 0) {
            _table.getSelectionModel().clearSelection();
        }
        return false;
    }

    private void scrollToRows(int firstRow, int lastRow) {
        assert firstRow <= lastRow;
        final int rowHeight = _table.getHeight() / instructions().length();
        final int width = _table.getWidth() - 2;
        final int height = rowHeight - 2;
        // Create a rectangle in the table view to use as a scroll target; include
        // the row immediately before and the row immediately after so that the row of interest
        // doesn't land at the very beginning or very end of the view, if possible.
        final Rectangle rectangle = new Rectangle(0, (firstRow - 1) * rowHeight, width, 3 * height);
        // System.out.println("row=" + firstRow + " rect=" + rectangle);
        _table.scrollRectToVisible(rectangle);
    }

    private final class MyTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return ColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return instructions().length();
        }

        public Object getValueAt(int row, int col) {
            final TargetCodeInstruction targetCodeInstruction = targetCodeInstructionAt(row);
            switch (ColumnKind.VALUES.get(col)) {
                case TAG:
                    return null;
                case NUMBER:
                    return row;
                case ADDRESS:
                    return targetCodeInstruction.address();
                case POSITION:
                    return targetCodeInstruction.position();
                case LABEL:
                    final String label = targetCodeInstruction.label();
                    return label != null ? label + ":" : "";
                case INSTRUCTION:
                    return targetCodeInstruction.mnemonic();
                case OPERANDS:
                    return targetCodeInstruction.operands();
                case SOURCE_LINE:
                    return "";
                case BYTES:
                    return targetCodeInstruction.bytes();
                default:
                    throw new RuntimeException("Column out of range: " + col);
            }
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            switch (ColumnKind.VALUES.get(col)) {
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
    }

    private final class MyTable extends JTable {

        MyTable(TableModel model, TableColumnModel tableColumnModel) {
            super(model, tableColumnModel);
        }

        @Override
        public void paintChildren(Graphics g) {
            super.paintChildren(g);
            final int row = getSelectedRow();
            if (row >= 0) {
                g.setColor(style().debugSelectedCodeBorderColor());
                g.drawRect(0, row * _table.getRowHeight(row), getWidth() - 1, _table.getRowHeight(row) - 1);
            }
        }

        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new JTableHeader(_columnModel) {
                @Override
                public String getToolTipText(MouseEvent mouseEvent) {
                    final Point p = mouseEvent.getPoint();
                    final int index = _columnModel.getColumnIndexAtX(p.x);
                    final int modelIndex = _columnModel.getColumn(index).getModelIndex();
                    return ColumnKind.VALUES.get(modelIndex).toolTipText();
                }
            };
        }
    }

    private final class MyTableColumnModel extends DefaultTableColumnModel {

        private final Preferences _preferences;

        private MyTableColumnModel() {
            _preferences = new Preferences(JTableTargetCodeViewer.globalPreferences(inspection())) {
                @Override
                public void setIsVisible(ColumnKind columnKind, boolean visible) {
                    super.setIsVisible(columnKind, visible);
                    final int col = columnKind.ordinal();
                    if (visible) {
                        addColumn(_columns[col]);
                    } else {
                        removeColumn(_columns[col]);
                    }
                    JTableColumnResizer.adjustColumnPreferredWidths(_table);
                    refresh(teleVM().teleProcess().epoch(), true);
                }
            };

            createColumn(ColumnKind.TAG, new TagRenderer());
            createColumn(ColumnKind.NUMBER, new NumberRenderer());
            createColumn(ColumnKind.ADDRESS, new AddressRenderer(targetCodeInstructionAt(0).address()));
            createColumn(ColumnKind.POSITION, new PositionRenderer(targetCodeInstructionAt(0).address()));
            createColumn(ColumnKind.LABEL, new LabelRenderer(targetCodeInstructionAt(0).address()));
            createColumn(ColumnKind.INSTRUCTION, new InstructionRenderer(_inspection));
            createColumn(ColumnKind.OPERANDS, _operandsRenderer);
            createColumn(ColumnKind.SOURCE_LINE, _sourceLineRenderer);
            createColumn(ColumnKind.BYTES, new BytesRenderer(_inspection));
        }

        private Preferences preferences() {
            return _preferences;
        }

        private void createColumn(ColumnKind columnKind, TableCellRenderer renderer) {
            final int col = columnKind.ordinal();
            _columns[col] = new TableColumn(col, 0, renderer, null);
            _columns[col].setHeaderValue(columnKind.label());
            _columns[col].setMinWidth(columnKind.minWidth());
            if (_preferences.isVisible(columnKind)) {
                addColumn(_columns[col]);
            }
            _columns[col].setIdentifier(columnKind);
        }
    }

    /**
     * Return the appropriate color for displaying the row's text depending on whether the instruction pointer is at
     * this row.
     *
     * @param row the row to check
     * @return the color to be used
     */
    private Color getRowTextColor(int row) {
        return isInstructionPointer(row) ? style().debugIPTextColor() : (isCallReturn(row) ? style().debugCallReturnTextColor() : style().defaultCodeColor());
    }

    private final class MyInspectorMouseClickAdapter extends InspectorMouseClickAdapter {
        MyInspectorMouseClickAdapter(Inspection inspection) {
            super(inspection);
        }
        @Override
        public void procedure(final MouseEvent mouseEvent) {
            final int selectedRow = getSelectedRow();
            final int selectedColumn = _table.getSelectedColumn();
            if (selectedRow != -1 && selectedColumn != -1) {
                // Left button selects a table cell; also cause a code selection at the row.
                if (MaxineInspector.mouseButtonWithModifiers(mouseEvent) == MouseEvent.BUTTON1) {
                    _inspection.focus().setCodeLocation(new TeleCodeLocation(teleVM(), targetCodeInstructionAt(selectedRow).address()), true);
                }
            }
            // Locate the renderer under the event location, and pass along the mouse click if appropriate
            final Point p = mouseEvent.getPoint();
            final int hitColumnIndex = _table.columnAtPoint(p);
            final int hitRowIndex = _table.rowAtPoint(p);
            if ((hitColumnIndex != -1) && (hitRowIndex != -1)) {
                final TableCellRenderer tableCellRenderer = _table.getCellRenderer(hitRowIndex, hitColumnIndex);
                final Object cellValue = _table.getValueAt(hitRowIndex, hitColumnIndex);
                final Component component = tableCellRenderer.getTableCellRendererComponent(_table, cellValue, false, true, hitRowIndex, hitColumnIndex);
                if (component != null) {
                    component.dispatchEvent(mouseEvent);
                }
            }
        }
    }

    private final class TagRenderer extends JLabel implements TableCellRenderer, TextSearchable {
        public Component getTableCellRendererComponent(JTable table, Object ignore, boolean isSelected, boolean hasFocus, int row, int col) {
            setOpaque(true);
            setBackground(rowToBackgroundColor(row));
            final StringBuilder toolTipText = new StringBuilder(100);
            final StackFrameInfo stackFrameInfo = stackFrameInfo(row);
            if (stackFrameInfo != null) {
                toolTipText.append("Stack ");
                toolTipText.append(stackFrameInfo.position());
                toolTipText.append(":  0x");
                toolTipText.append(stackFrameInfo.frame().instructionPointer().toHexString());
                toolTipText.append("  thread=");
                toolTipText.append(_inspection.nameDisplay().longName(stackFrameInfo.thread()));
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
            final TeleTargetBreakpoint teleTargetBreakpoint = getTargetBreakpointAtRow(row);
            if (teleTargetBreakpoint != null) {
                toolTipText.append(teleTargetBreakpoint);
                if (teleTargetBreakpoint.isEnabled()) {
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
    }

    private final class NumberRenderer extends PlainLabel implements TableCellRenderer {

        public NumberRenderer() {
            super(_inspection, "");
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(row);
            setToolTipText("Instruction no. " + row + "in method");
            setBackground(rowToBackgroundColor(row));
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class AddressRenderer extends LocationLabel.AsAddressWithPosition implements TableCellRenderer {

        private final Address _entryAddress;

        AddressRenderer(Address entryAddress) {
            super(_inspection, 0, entryAddress);
            _entryAddress = entryAddress;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Address address = (Address) value;
            setValue(address.minus(_entryAddress).toInt());
            setColumns(getText().length() + 1);
            setBackground(rowToBackgroundColor(row));
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class PositionRenderer extends LocationLabel.AsPosition implements TableCellRenderer {
        private int _position;

        public PositionRenderer(Address entryAddress) {
            super(_inspection, 0, entryAddress);
            _position = 0;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Integer position = (Integer) value;
            if (_position != position) {
                _position = position;
                setValue(position);
            }
            setBackground(rowToBackgroundColor(row));
            setForeground(getRowTextColor(row));
            return this;
        }
    }


    private final class LabelRenderer extends LocationLabel.AsTextLabel implements TableCellRenderer {

        public LabelRenderer(Address entryAddress) {
            super(_inspection, entryAddress);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Integer position = (Integer) _model.getValueAt(row, ColumnKind.POSITION.ordinal());
            setLocation(value.toString(), position);
            setFont(style().defaultTextFont());
            setBackground(rowToBackgroundColor(row));
            setForeground(getRowTextColor(row));
            return this;
        }
    }


    private final class InstructionRenderer extends TargetCodeLabel implements TableCellRenderer {
        InstructionRenderer(Inspection inspection) {
            super(inspection, "");
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setBackground(rowToBackgroundColor(row));
            setForeground(getRowTextColor(row));
            final String string = value.toString();
            setText(string);
            setColumns(string.length() + 1);
            return this;
        }
    }


    private interface LiteralRenderer {
        WordValueLabel render(Inspection inspection, String literalLoadText, Word literal);
    }

    static final LiteralRenderer AMD64_LITERAL_RENDERER = new LiteralRenderer() {
        public WordValueLabel render(Inspection inspection, String literalLoadText, Word literal) {
            final WordValueLabel wordValueLabel = new WordValueLabel(inspection, WordValueLabel.ValueMode.LITERAL_REFERENCE, literal);
            wordValueLabel.setPrefix(literalLoadText.substring(0, literalLoadText.indexOf("[")));
            wordValueLabel.setToolTipSuffix(" from RIP " + literalLoadText.substring(literalLoadText.indexOf("["), literalLoadText.length()));
            wordValueLabel.updateText();
            return wordValueLabel;
        }
    };

    static final LiteralRenderer SPARC_LITERAL_RENDERER = new LiteralRenderer() {
        public WordValueLabel render(Inspection inspection, String literalLoadText, Word literal) {
            final WordValueLabel wordValueLabel = new WordValueLabel(inspection, WordValueLabel.ValueMode.LITERAL_REFERENCE, literal);
            wordValueLabel.setSuffix(literalLoadText.substring(literalLoadText.indexOf(",")));
            wordValueLabel.setToolTipSuffix(" from " + literalLoadText.substring(0, literalLoadText.indexOf(",")));
            wordValueLabel.updateText();
            return wordValueLabel;
        }
    };

    LiteralRenderer getLiteralRenderer(Inspection inspection) {
        final ProcessorKind processorKind = inspection.teleVM().vmConfiguration().platform().processorKind();
        switch (processorKind.instructionSet()) {
            case AMD64:
                return AMD64_LITERAL_RENDERER;
            case SPARC:
                return SPARC_LITERAL_RENDERER;
            case ARM:
            case PPC:
            case IA32:
                Problem.unimplemented();
                return null;
        }
        ProgramError.unknownCase();
        return null;
    }

    private final class SourceLineRenderer extends PlainLabel implements TableCellRenderer {
        private BytecodeLocation _lastBytecodeLocation;
        SourceLineRenderer() {
            super(JTableTargetCodeViewer.this.inspection(), null);
            addMouseListener(new InspectorMouseClickAdapter(inspection()) {
                @Override
                public void procedure(final MouseEvent mouseEvent) {
                    final BytecodeLocation bytecodeLocation = _lastBytecodeLocation;
                    if (bytecodeLocation != null) {
                        inspection().viewSourceExternally(bytecodeLocation);
                    }
                }
            });
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final BytecodeLocation bytecodeLocation = rowToBytecodeLocation(row);
            setText("");
            setToolTipText("Source line not available");
            setBackground(rowToBackgroundColor(row));
            if (bytecodeLocation != null) {
                final String sourceFileName = bytecodeLocation.sourceFileName();
                final int lineNumber = bytecodeLocation.sourceLineNumber();
                if (sourceFileName != null && lineNumber >= 0) {
                    setText(String.valueOf(lineNumber));
                    setToolTipText(sourceFileName + ":" + lineNumber);
                }
            }
            _lastBytecodeLocation = bytecodeLocation;
            return this;
        }
    }

    private final class OperandsRenderer implements TableCellRenderer, Prober {
        private InspectorLabel[] _wordValueLabels = new InspectorLabel[instructions().length()];
        private TargetCodeLabel _targetCodeLabel = new TargetCodeLabel(_inspection, "");
        private LiteralRenderer _literalRenderer = getLiteralRenderer(_inspection);

        public void refresh(long epoch, boolean force) {
            for (InspectorLabel wordValueLabel : _wordValueLabels) {
                if (wordValueLabel != null) {
                    wordValueLabel.refresh(epoch, force);
                }
            }
        }

        @Override
        public void redisplay() {
            for (InspectorLabel wordValueLabel : _wordValueLabels) {
                if (wordValueLabel != null) {
                    wordValueLabel.redisplay();
                }
            }
            _targetCodeLabel.redisplay();
        }

        public Component getTableCellRendererComponent(JTable table, Object ignore, boolean isSelected, boolean hasFocus, int row, int col) {
            InspectorLabel inspectorLabel = _wordValueLabels[row];
            if (inspectorLabel == null) {
                final TargetCodeInstruction targetCodeInstruction = targetCodeInstructionAt(row);
                final String text = targetCodeInstruction.operands();
                if (targetCodeInstruction._targetAddress != null && !teleTargetRoutine().targetCodeRegion().contains(targetCodeInstruction._targetAddress)) {
                    inspectorLabel = new WordValueLabel(_inspection, WordValueLabel.ValueMode.CALL_ENTRY_POINT, targetCodeInstruction._targetAddress.asWord());
                    _wordValueLabels[row] = inspectorLabel;
                } else if (targetCodeInstruction._literalSourceAddress != null) {
                    final Word word = teleVM().teleProcess().dataAccess().readWord(targetCodeInstruction._literalSourceAddress);
                    inspectorLabel = _literalRenderer.render(_inspection, text, word);
                    _wordValueLabels[row] = inspectorLabel;
                } else if (rowToCalleeIndex(row) >= 0) {
                    final PoolConstantLabel poolConstantLabel = PoolConstantLabel.make(_inspection, rowToCalleeIndex(row), localConstantPool(), teleConstantPool(), PoolConstantLabel.Mode.TERSE);
                    poolConstantLabel.setToolTipPrefix(text);
                    inspectorLabel = poolConstantLabel;
                    inspectorLabel.setForeground(getRowTextColor(row));
                } else {
                    inspectorLabel = _targetCodeLabel;
                    inspectorLabel.setText(text);
                    inspectorLabel.setToolTipText(null);
                    inspectorLabel.setForeground(getRowTextColor(row));
                }
            }
            inspectorLabel.setBackground(rowToBackgroundColor(row));
            return inspectorLabel;
        }

    }


    private final class BytesRenderer extends DataLabel.ByteArrayAsHex implements TableCellRenderer {
        BytesRenderer(Inspection inspection) {
            super(inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setBackground(rowToBackgroundColor(row));
            setForeground(getRowTextColor(row));
            setValue((byte[]) value);
            return this;
        }
    }

    @Override
    protected void updateView(long epoch, boolean force) {
        _operandsRenderer.refresh(epoch, force);
    }

    public void redisplay() {
        _operandsRenderer.redisplay();
        for (int col = _columnModel.getColumnCount() - 1; col >= 0; --col) {
            final TableCellRenderer renderer = _table.getCellRenderer(0, col);
            if (renderer instanceof InspectorLabel) {
                final InspectorLabel inspectorLabel = (InspectorLabel) renderer;
                inspectorLabel.redisplay();
            }
        }
        invalidate();
        repaint();
    }

}

