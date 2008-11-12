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
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.target.*;

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
        TAG("Tag", true) {
            @Override
            public boolean canBeMadeInvisible() {
                return false;
            }
        },
        ADDRESS("Addr.", false),
        POSITION("Pos.", false),
        LABEL("Label", true),
        INSTRUCTION("Instr.", true),
        OPERANDS("Operands", true),
        SOURCE_LINE("Line", true),
        BYTES("Bytes", false);

        private final String _label;
        private final boolean _defaultVisibility;

        private ColumnKind(String label, boolean defaultVisibility) {
            _label = label;
            _defaultVisibility = defaultVisibility;
            assert defaultVisibility || canBeMadeInvisible();
        }

        public String label() {
            return _label;
        }

        @Override
        public String toString() {
            return _label;
        }

        /**
         * Determines if this column kind can be made invisible.
         */
        public boolean canBeMadeInvisible() {
            return true;
        }

        /**
         * Determines if this column should be visible by default.
         */
        public boolean defaultVisibility() {
            return _defaultVisibility;
        }

        public static final IndexedSequence<ColumnKind> VALUES = new ArraySequence<ColumnKind>(values());
    }

    public static class Preferences extends CodeColumnVisibilityPreferences<ColumnKind> {
        public Preferences(CodeColumnVisibilityPreferences<ColumnKind> otherPreferences) {
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

    /**
     * Sets the minimum width of the columns based on the current values in the table model.
     */
    private Dimension setColumnSizes() {
        // TODO (mlvdv) reduce min width of the TAG column
        final int margin = _columnModel.getColumnMargin();
        final int rowCount = _model.getRowCount();

        int totalWidth = 0;
        int totalHeight = 0;

        for (int i = _columnModel.getColumnCount() - 1; i >= 0; --i) {
            final TableColumn column = _columnModel.getColumn(i);

            final int columnIndex = column.getModelIndex();

            int width = 0;
            int height = 0;

            for (int row = rowCount - 1; row >= 0; --row) {
                final TableCellRenderer renderer = _table.getCellRenderer(row, i);
                final Object value = _model.getValueAt(row, columnIndex);
                final Component c = renderer.getTableCellRendererComponent(_table, value, false, false, row, i);
                final Dimension preferredSize = c.getPreferredSize();
                width = Math.max(width, preferredSize.width);
                height += preferredSize.height;
            }

            assert width > 0;
            column.setMinWidth(width + margin);
            totalWidth += column.getPreferredWidth();
            totalHeight = Math.max(totalHeight, height);
        }

        return new Dimension(totalWidth, totalHeight);
    }

    public JTableTargetCodeViewer(Inspection inspection, MethodInspector parent, TeleTargetRoutine teleTargetRoutine) {
        super(inspection, parent, teleTargetRoutine);
        _inspection = inspection;
        _operandsRenderer = new OperandsRenderer();
        _sourceLineRenderer = new SourceLineRenderer();
        _model = new MyTableModel();
        _columns = new TableColumn[ColumnKind.VALUES.length()];
        _columnModel = new MyTableColumnModel();
        _table = new JTable(_model, _columnModel) {
            @Override
            public void paintChildren(Graphics g) {
                super.paintChildren(g);
                final int row = _table.getSelectionModel().getMinSelectionIndex();
                if (row >= 0) {
                    g.setColor(style().debugSelectedCodeBorderColor());
                    g.drawRect(0, row * _table.getRowHeight(row), getWidth() - 1, _table.getRowHeight(row) - 1);
                }
            }
        };
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

        final Dimension preferredTableSize = setColumnSizes();
        _table.setPreferredScrollableViewportSize(preferredTableSize);

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

        final JButton viewOptionsButton = new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                new CodeColumnVisibilityPreferences.Dialog<ColumnKind>(inspection(), "TargetCode View Options", _columnModel.preferences(), globalPreferences(inspection()));
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
    }

    @Override
    protected int getSelectedRow() {
        return _table.getSelectedRow();
    }

    @Override
    protected void setFocusAtRow(int row) {
        _inspection.focus().setCodeLocation(new TeleCodeLocation(teleVM(), targetCodeInstructionAt(row).address()), false);
    }


    /**
     * Global code selection has been set; return true iff the view contains selection.
     * Update even when the selection is set to the same value, because we want
     * that to force a scroll to make the selection visible.
     */
    @Override
    public boolean updateCodeFocus(TeleCodeLocation teleCodeLocation) {
        final int oldSelectedRow = _table.getSelectionModel().getMinSelectionIndex();
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
                    setColumnSizes();
                    refresh(teleVM().teleProcess().epoch(), true);
                }
            };

            createColumn(ColumnKind.TAG, new TagRenderer());
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

    private final class TagRenderer extends JLabel implements TableCellRenderer {
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
                if (teleTargetBreakpoint.enabled()) {
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
                //setColumns(getText().length() + 1);
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
            setToolTipText(null);
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
        private WordValueLabel[] _wordValueLabels = new WordValueLabel[instructions().length()];
        private TargetCodeLabel _targetCodeLabel = new TargetCodeLabel(_inspection, "");
        private LiteralRenderer _literalRenderer = getLiteralRenderer(_inspection);
        private String[] _calleeNames = new String[instructions().length()];
        private String[] _calleeSignatures = new String[instructions().length()];

        public void refresh(long epoch, boolean force) {
            for (WordValueLabel wordValueLabel : _wordValueLabels) {
                if (wordValueLabel != null) {
                    wordValueLabel.refresh(epoch, force);
                }
            }
        }

        public Inspection inspection() {
            return _inspection;
        }

        @Override
        public void redisplay() {
            for (WordValueLabel wordValueLabel : _wordValueLabels) {
                if (wordValueLabel != null) {
                    wordValueLabel.redisplay();
                }
            }
            _targetCodeLabel.redisplay();
        }

        public OperandsRenderer() {
            for (int row = 0; row < instructions().length(); ++row) {
                final TargetCodeInstruction targetCodeInstruction = targetCodeInstructionAt(row);

                final TeleTargetRoutine teleTargetRoutine = teleTargetRoutine();
                if (teleTargetRoutine instanceof TeleTargetMethod) {
                    final TeleTargetMethod teleTargetMethod = (TeleTargetMethod) teleTargetRoutine;
                    final int stopIndex = teleTargetMethod.getJavaStopIndex(targetCodeInstruction.address());
                    if (stopIndex != -1) {
                        final TargetJavaFrameDescriptor javaFrameDescriptor = teleTargetMethod.getJavaFrameDescriptor(stopIndex);
                        if (javaFrameDescriptor != null) {
                            final BytecodeLocation bytecodeLocation = javaFrameDescriptor.bytecodeLocation();
                            final int[] calleeIndex = {0};
                            final ConstantPool constantPool = bytecodeLocation.classMethodActor().codeAttribute().constantPool();
                            final BytecodeAdapter adapter = new BytecodeAdapter() {
                                private void setCallee(int index) {
                                    if (calleeIndex[0] != 0) {
                                        inspection().errorMessage("more than one callee for bytecode location " + bytecodeLocation, "Target Code Viewer");
                                    }
                                    calleeIndex[0] = index;
                                }
                                @Override
                                protected void invokestatic(int index) {
                                    setCallee(index);
                                }
                                @Override
                                protected void invokespecial(int index) {
                                    setCallee(index);
                                }
                                @Override
                                protected void invokevirtual(int index) {
                                    setCallee(index);
                                }
                                @Override
                                protected void invokeinterface(int index, int count) {
                                    setCallee(index);
                                }
                            };

                            final BytecodeScanner bytecodeScanner = new BytecodeScanner(adapter);
                            try {
                                bytecodeScanner.scanInstruction(bytecodeLocation.getBytecodeBlock());
                                if (calleeIndex[0] != 0) {
                                    final MethodRefConstant methodRef = constantPool.methodAt(calleeIndex[0]);
                                    final String name = methodRef.name(constantPool).string();
                                    _calleeNames[row] = name;
                                    _calleeSignatures[row] = methodRef.holder(constantPool).toJavaString() + "." + name + methodRef.signature(constantPool).toJavaString(false, true);
                                }
                            } catch (Throwable throwable) {
                                inspection().errorMessage("could not scan byte code in " + bytecodeLocation + ": " + throwable, "Target Code Viewer");
                            }
                        }
                    }
                }
            }
        }


        public Component getTableCellRendererComponent(JTable table, Object ignore, boolean isSelected, boolean hasFocus, int row, int col) {
            WordValueLabel wordValueLabel = _wordValueLabels[row];
            if (wordValueLabel == null) {
                final TargetCodeInstruction targetCodeInstruction = targetCodeInstructionAt(row);
                final String text = targetCodeInstruction.operands();
                if (targetCodeInstruction._targetAddress != null && !teleTargetRoutine().targetCodeRegion().contains(targetCodeInstruction._targetAddress)) {
                    wordValueLabel = new WordValueLabel(inspection(), WordValueLabel.ValueMode.CALL_ENTRY_POINT, targetCodeInstruction._targetAddress.asWord());
                    _wordValueLabels[row] = wordValueLabel;
                } else if (targetCodeInstruction._literalSourceAddress != null) {
                    final Word word = teleVM().teleProcess().dataAccess().readWord(targetCodeInstruction._literalSourceAddress);
                    wordValueLabel = _literalRenderer.render(inspection(), text, word);
                    _wordValueLabels[row] = wordValueLabel;
                } else {
                    final String calleeName = _calleeNames[row];
                    if (calleeName == null) {
                        _targetCodeLabel.setText(text);
                        _targetCodeLabel.setToolTipText(null);
                    } else {
                        _targetCodeLabel.setText(calleeName + "()");
                        _targetCodeLabel.setToolTipText(text + " # " +  _calleeSignatures[row]);
                    }
                    _targetCodeLabel.setForeground(getRowTextColor(row));
                    _targetCodeLabel.setBackground(rowToBackgroundColor(row));
                    return _targetCodeLabel;
                }
            }
            wordValueLabel.setBackground(rowToBackgroundColor(row));
            return wordValueLabel;
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

