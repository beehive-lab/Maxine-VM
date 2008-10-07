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
/*VCSID=d59a24d8-fc69-4366-853d-3be669763fc7*/
package com.sun.max.ins.method;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.constant.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
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
        POSITION("Pos.", true),
        INSTRUCTION("Instr.", true),
        OPERAND1("Operand 1", true),
        OPERAND2("Operand 2", true),
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
        private PoolConstantLabel.Mode _operandDisplayMode;

        public Preferences(Inspection inspection) {
            super(inspection, "bytecodeInspectorPrefs", ColumnKind.class, ColumnKind.VALUES);
            final OptionTypes.EnumType<PoolConstantLabel.Mode> optionType = new OptionTypes.EnumType<PoolConstantLabel.Mode>(PoolConstantLabel.Mode.class);
            _operandDisplayMode = inspection.settings().get(_saveSettingsListener, "operandDisplayMode", optionType, PoolConstantLabel.Mode.JAVAP);
        }

        public Preferences(Preferences otherPreferences) {
            super(otherPreferences);
            _operandDisplayMode = otherPreferences._operandDisplayMode;
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

        public PoolConstantLabel.Mode operandDisplayMode() {
            return _operandDisplayMode;
        }

        public void setOperandDisplayMode(PoolConstantLabel.Mode mode) {
            final boolean needToSave = mode != _operandDisplayMode;
            _operandDisplayMode = mode;
            if (needToSave) {
                _inspection.settings().save();
            }
        }

        @Override
        protected void saveSettings(SaveSettingsEvent saveSettingsEvent) {
            super.saveSettings(saveSettingsEvent);
            saveSettingsEvent.save("operandDisplayMode", _operandDisplayMode.name());
        }

        @Override
        public JPanel getPanel() {
            final JRadioButton javapButton = new JRadioButton("javap style");
            javapButton.setToolTipText("Display bytecode operands in a style similar to the 'javap' tool and the JVM spec book");
            final JRadioButton terseButton = new JRadioButton("terse style");
            terseButton.setToolTipText("Display bytecode operands in a terse style");
            final ButtonGroup group = new ButtonGroup();
            group.add(javapButton);
            group.add(terseButton);

            javapButton.setSelected(_operandDisplayMode == PoolConstantLabel.Mode.JAVAP);
            terseButton.setSelected(_operandDisplayMode == PoolConstantLabel.Mode.TERSE);

            final ActionListener styleActionListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (javapButton.isSelected()) {
                        setOperandDisplayMode(PoolConstantLabel.Mode.JAVAP);
                    } else if (terseButton.isSelected()) {
                        setOperandDisplayMode(PoolConstantLabel.Mode.TERSE);
                    }
                }
            };
            javapButton.addActionListener(styleActionListener);
            terseButton.addActionListener(styleActionListener);

            final JPanel panel2 = new JPanel(new BorderLayout());
            panel2.setOpaque(true);
            panel2.setBackground(_inspection.style().defaultBackgroundColor());
            final JPanel operandStylePanel = new JPanel();
            operandStylePanel.setOpaque(true);
            operandStylePanel.setBackground(_inspection.style().defaultBackgroundColor());
            operandStylePanel.add(new TextLabel(_inspection, "Operand Style:  "));
            operandStylePanel.add(javapButton);
            operandStylePanel.add(terseButton);
            panel2.add(operandStylePanel, BorderLayout.WEST);

            final JPanel panel = new JPanel(new BorderLayout());
            panel.add(super.getPanel(), BorderLayout.NORTH);
            panel.add(operandStylePanel, BorderLayout.SOUTH);
            return panel;
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
    private PoolConstantLabel.Mode _operandDisplayMode;

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

    public JTableBytecodeViewer(Inspection inspection, MethodInspector parent, TeleClassMethodActor teleClassMethodActor, TeleTargetMethod teleTargetMethod) {
        super(inspection, parent, teleClassMethodActor, teleTargetMethod);
        _inspection = inspection;
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
        _operandDisplayMode = globalPreferences(inspection())._operandDisplayMode;

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
        // TODO (mlvdv) implement remaining debugging controls in Bytecode view
        // the disabled ones haven't been adapted for bytecode-based debugging
        JButton button = new JButton(_inspection.inspectionMenus().getToggleBytecodeBreakpointAction());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugToggleBreakpointbuttonIcon());
        button.setEnabled(false);
        toolBar().add(button);

        button = new JButton(_inspection.inspectionMenus().getStepOverAction());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugStepOverButtonIcon());
        button.setEnabled(false);
        toolBar().add(button);

        button = new JButton(_inspection.inspectionMenus().getSingleStepAction());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugStepInButtonIcon());
        button.setEnabled(false);
        toolBar().add(button);

        button = new JButton(_inspection.inspectionMenus().getReturnFromFrameAction());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugStepOutButtonIcon());
        button.setEnabled(haveTargetCodeAddresses());
        toolBar().add(button);

        button = new JButton(_inspection.inspectionMenus().getRunToInstructionAction());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugRunToCursorButtonIcon());
        button.setEnabled(haveTargetCodeAddresses());
        toolBar().add(button);

        button = new JButton(_inspection.inspectionMenus().getResumeAction());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugContinueButtonIcon());
        toolBar().add(button);

        button = new JButton(_inspection.inspectionMenus().getPauseAction());
        button.setToolTipText(button.getText());
        button.setText(null);
        button.setIcon(style().debugPauseButtonIcon());
        toolBar().add(button);

        toolBar().add(Box.createHorizontalGlue());

        toolBar().add(new JLabel("Bytecode"));

        toolBar().add(Box.createHorizontalGlue());

        addActiveRowsButton();

        final JButton viewOptionsButton = new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                new CodeColumnVisibilityPreferences.Dialog<ColumnKind>(inspection(), "Bytecode View Options", _columnModel.preferences(), globalPreferences(inspection()));
            }
        });
        viewOptionsButton.setText("View...");
        viewOptionsButton.setToolTipText("Bytecode view options");
        toolBar().add(viewOptionsButton);

        toolBar().add(Box.createHorizontalGlue());
        addCodeViewCloseButton();

        final JScrollPane scrollPane = new JScrollPane(_table);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(style().defaultBackgroundColor());
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    protected int getSelectedRow() {
        return _table.getSelectedRow();
    }

    @Override
    protected void setFocusAtRow(int row) {
        final int position = bytecodeInstructionAt(row)._position;
        _inspection.focus().setCodeLocation(new TeleCodeLocation(teleVM(), teleClassMethodActor(), position), false);
    }

    /**
     * Global code selection has changed.
     */
    @Override
    public boolean updateCodeFocus(TeleCodeLocation teleCodeLocation) {
        final int oldSelectedRow = _table.getSelectionModel().getMinSelectionIndex();
        if (teleCodeLocation.hasBytecodeLocation()) {
            final BytecodeLocation bytecodeLocation = teleCodeLocation.bytecodeLocation();
            if (bytecodeLocation.classMethodActor() == teleClassMethodActor().classMethodActor()) {
                final int row = positionToRow(bytecodeLocation.position());
                if (row >= 0) {
                    if (row != oldSelectedRow) {
                        _table.getSelectionModel().setSelectionInterval(row, row);
                    }
                    scrollToRows(row, row);
                    return true;
                }
            }
        } else if (teleCodeLocation.hasTargetCodeLocation()) {
            if (teleTargetMethod() != null && teleTargetMethod().targetCodeRegion().contains(teleCodeLocation.targetCodeInstructionAddresss())) {
                final int row = addressToRow(teleCodeLocation.targetCodeInstructionAddresss());
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
        final int rowHeight = _table.getHeight() / bytecodeInstructions().length();
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
            return bytecodeInstructions().length();
        }

        public Object getValueAt(int row, int col) {
            final BytecodeInstruction instruction = bytecodeInstructionAt(row);
            switch (ColumnKind.VALUES.get(col)) {
                case TAG:
                    return null;
                case POSITION:
                    return new Integer(instruction._position);
                case INSTRUCTION:
                    return instruction._opcode;
                case OPERAND1:
                    return instruction._operand1;
                case OPERAND2:
                    return instruction._operand2;
                case SOURCE_LINE:
                    return new BytecodeLocation(teleClassMethodActor().classMethodActor(), instruction._position);
                case BYTES:
                    return instruction._instructionBytes;
                default:
                    throw new RuntimeException("Column out of range: " + col);
            }
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            switch (ColumnKind.VALUES.get(col)) {
                case TAG:
                    return Object.class;
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

    }

    private final class MyTableColumnModel extends DefaultTableColumnModel {

        private final Preferences _preferences;

        MyTableColumnModel() {
            _preferences = new Preferences(JTableBytecodeViewer.globalPreferences(inspection())) {
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
                    refresh(teleVM().teleProcess().epoch());
                }
            };
            createColumn(ColumnKind.TAG, new TagRenderer());
            createColumn(ColumnKind.POSITION, new PositionRenderer());
            createColumn(ColumnKind.INSTRUCTION, new InstructionRenderer());
            createColumn(ColumnKind.OPERAND1, new OperandRenderer());
            createColumn(ColumnKind.OPERAND2, new OperandRenderer());
            createColumn(ColumnKind.SOURCE_LINE, new SourceLineRenderer());
            createColumn(ColumnKind.BYTES, new BytesRenderer());
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

        public Preferences preferences() {
            return _preferences;
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
        return style().bytecodeBackgroundColor();
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
                    final Address targetCodeFirstAddress = bytecodeInstructions().get(selectedRow).targetCodeFirstAddress();
                    _inspection.focus().setCodeLocation(new TeleCodeLocation(teleVM(), targetCodeFirstAddress, teleClassMethodActor(), bytecodeInstructionAt(selectedRow)._position), true);
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
            setBackground(getRowBackgroundColor(row));
            final StringBuilder toolTipText = new StringBuilder(100);
            final StackFrameInfo stackFrameInfo = stackFrameInfo(row);
            if (stackFrameInfo != null) {
                toolTipText.append("Stack ");
                toolTipText.append(stackFrameInfo.position());
                toolTipText.append(":  0x");
                toolTipText.append(stackFrameInfo.frame().instructionPointer().toHexString());
                toolTipText.append(" thread=");
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
            final TeleBytecodeBreakpoint teleBytecodeBreakpoint = getBytecodeBreakpointAtRow(row);
            final Sequence<TeleTargetBreakpoint> teleTargetBreakpoints = getTargetBreakpointsAtRow(row);
            if (teleBytecodeBreakpoint != null) {
                toolTipText.append(teleBytecodeBreakpoint);
                toolTipText.append("; ");
                if (teleBytecodeBreakpoint.enabled()) {
                    setBorder(style().debugEnabledBytecodeBreakpointTagBorder());
                } else {
                    setBorder(style().debugDisabledBytecodeBreakpointTagBorder());
                }
            } else if (teleTargetBreakpoints.length() > 0) {
                boolean enabled = false;
                for (TeleTargetBreakpoint teleTargetBreakpoint : teleTargetBreakpoints) {
                    toolTipText.append(teleTargetBreakpoint);
                    toolTipText.append("; ");
                    enabled = enabled || teleTargetBreakpoint.enabled();
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
    }

    private final class PositionRenderer extends LocationLabel.AsPosition implements TableCellRenderer {
        private int _position;

        public PositionRenderer() {
            super(_inspection, 0);
            _position = 0;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Integer position = (Integer) value;
            if (_position != position) {
                _position = position;
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
            super(_inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Bytecode opcode = (Bytecode) value;
            setValue(opcode);
            setBackground(getRowBackgroundColor(row));
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class OperandRenderer implements  TableCellRenderer {

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
                final TeleConstantPool teleConstantPool = teleClassMethodActor().getTeleHolder().getTeleConstantPool();
                renderer =  PoolConstantLabel.make(inspection(), index, teleConstantPool, _operandDisplayMode);
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
    }

    private final class SourceLineRenderer extends PlainLabel implements TableCellRenderer {
        private BytecodeLocation _lastBytecodeLocation;
        SourceLineRenderer() {
            super(JTableBytecodeViewer.this.inspection(), null);
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
            setText("");
            setToolTipText(null);
            final BytecodeLocation bytecodeLocation = (BytecodeLocation) value;
            final String sourceFileName = bytecodeLocation.sourceFileName();
            final int lineNumber = bytecodeLocation.sourceLineNumber();
            if (sourceFileName != null && lineNumber >= 0) {
                setText(String.valueOf(lineNumber));
                setToolTipText(sourceFileName + ":" + lineNumber);
            } else {
                setText("");
                setToolTipText(null);
            }
            _lastBytecodeLocation = bytecodeLocation;
            return this;
        }
    }

    private final class BytesRenderer extends DataLabel.ByteArrayAsHex implements TableCellRenderer {
        BytesRenderer() {
            super(_inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setBackground(getRowBackgroundColor(row));
            setForeground(getRowTextColor(row));
            setValue((byte[]) value);
            return this;
        }
    }

    public void redisplay() {
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
