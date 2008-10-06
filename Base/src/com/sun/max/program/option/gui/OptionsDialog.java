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
/*VCSID=680dc262-0a16-467f-8f1c-2121ab61505c*/
package com.sun.max.program.option.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;

import com.sun.max.*;
import com.sun.max.collect.*;
import com.sun.max.gui.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;

/**
 * A dialog used to present the {@linkplain Option options} defined by a {@link OptionSet}.
 *
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public class OptionsDialog extends JDialog {

    public static final int TEXT_FIELD_COLUMNS = 40;

    protected abstract static class GUIOption<Value_Type> implements ItemListener {
        protected final Option<Value_Type> _option;
        protected final JCheckBox _guard;
        protected final JLabel _label;
        protected JComponent _input;

        protected GUIOption(Option<Value_Type> opt) {
            this._option = opt;
            _guard = new JCheckBox();
            _label = new JLabel(_option.getName());
            _guard.addItemListener(this);
        }

        public JCheckBox getGuard() {
            return _guard;
        }

        public JLabel getLabel() {
            return _label;
        }

        public JComponent getInputComponent() {
            return _input;
        }

        public void itemStateChanged(ItemEvent e) {
            setInputAndLabelEnabled(_guard.isSelected());
        }

        protected void setEnabledOfContainedComponents(Container container, boolean enabled) {
            for (Component component : container.getComponents()) {
                component.setEnabled(enabled);
                if (component instanceof Container) {
                    setEnabledOfContainedComponents((Container) component, enabled);
                }
            }
        }

        protected void setInputAndLabelEnabled(boolean enabled) {
            _input.setEnabled(enabled);
            setEnabledOfContainedComponents(_input, enabled);
            _label.setEnabled(enabled);
        }

        public abstract Value_Type getValue() throws Option.Error;

        public abstract void setValue(Value_Type value);

        public void commitOption() {
            if (_guard.isSelected()) {
                _option.setValue(getValue());
            }
        }

        public String unparse() {
            return _option.getType().unparseValue(getValue());
        }

        public void parse(String str) {
            _guard.setEnabled(true);
            setValue(_option.getType().parseValue(str));
        }
    }

    protected static class IntegerGUIOption extends GUIOption<Integer> {
        private final JTextField _textField;

        protected IntegerGUIOption(Option<Integer> option) {
            super(option);
            _textField = new JTextField();
            _input = _textField;
            _textField.setColumns(TEXT_FIELD_COLUMNS);
            setValue(option.getValue());
        }

        @Override
        public void setValue(Integer i) {
            _textField.setText(String.valueOf(i));
            _guard.setSelected(i != null);
            setInputAndLabelEnabled(i != null);
        }

        @Override
        public Integer getValue() {
            if (_guard.isSelected()) {
                return _option.getType().parseValue(_textField.getText());
            }
            return null;
        }
    }

    protected static class BooleanGUIOption extends GUIOption<Boolean> {
        private final JLabel _helpText;
        protected BooleanGUIOption(Option<Boolean> option) {
            super(option);
            _helpText = new JLabel("<html>" + option.getHelp() + "</html>");
            _input = _helpText;
            setValue(option.getValue());
        }

        @Override
        public void commitOption() {
            if (_guard.isSelected()) {
                _option.setValue(true);
            } else {
                _option.setValue(false);
            }
        }

        @Override
        public void setValue(Boolean b) {
            _guard.setSelected(b);
            _helpText.setEnabled(b);
            setInputAndLabelEnabled(b);
        }

        @Override
        public Boolean getValue() {
            return _guard.isSelected();
        }
    }

    protected static class StringGUIOption extends GUIOption<String> {
        private final JTextField _textField;

        protected StringGUIOption(Option<String> option) {
            super(option);
            _textField = new JTextField(TEXT_FIELD_COLUMNS);
            _input = _textField;
            setValue(option.getValue());
        }

        @Override
        public void setValue(String i) {
            _textField.setText(String.valueOf(i));
            _guard.setSelected(i != null);
            setInputAndLabelEnabled(i != null);
        }

        @Override
        public String getValue() {
            if (_guard.isSelected()) {
                return _textField.getText();
            }
            return null;
        }
    }

    protected static class ListGUIOption extends GUIOption<List<Object>> {
        private final JTextField _textField;
        private final JList _list;

        protected ListGUIOption(Option<List<Object>> option) {
            super(option);
            if (option.getType() instanceof OptionTypes.EnumListType) {
                _textField = null;
                final OptionTypes.EnumListType elt = (OptionTypes.EnumListType) option.getType();
                final OptionTypes.EnumType et = (OptionTypes.EnumType) elt._elementOptionType;
                _list = new JList(et._values);
                _list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                _list.setVisibleRowCount(Math.min(10, et._values.length));
                _list.setLayoutOrientation(JList.VERTICAL);
                final JScrollPane scrollPane = new JScrollPane(_list);
                _input = scrollPane;
            } else {
                _list = null;
                _textField = new JTextField(TEXT_FIELD_COLUMNS);
                _input = _textField;
            }
            setValue(option.getValue());
        }

        @Override
        public void setValue(List<Object> list) {
            if (list != null) {
                if (_textField == null) {
                    _list.clearSelection();
                    final int[] selectedIndices = new int[list.size()];
                    int i = 0;
                    for (Object value : list) {
                        final Enum enumValue = (Enum) value;
                        selectedIndices[i++] = enumValue.ordinal();
                    }
                    _list.setSelectedIndices(selectedIndices);
                } else {
                    _textField.setText(String.valueOf(_option.getType().unparseValue(list)));
                }
            }
            _guard.setSelected(list != null);
            setInputAndLabelEnabled(list != null);
        }

        @Override
        public List<Object> getValue() {
            if (_guard.isSelected()) {
                if (_textField == null) {
                    final List<Object> list = new LinkedList<Object>();
                    for (Object value : _list.getSelectedValues()) {
                        list.add(value);
                    }
                    return list;
                }
                return _option.getType().parseValue(_textField.getText());
            }
            return null;
        }
    }

    protected static class URLGUIOption extends GUIOption<URL> {
        private final JTextField _textField;

        protected URLGUIOption(Option<URL> option) {
            super(option);
            _textField = new JTextField(TEXT_FIELD_COLUMNS);
            _input = _textField;
            setValue(option.getValue());
        }

        @Override
        public void setValue(URL i) {
            _textField.setText(String.valueOf(i));
            _guard.setSelected(i != null);
            setInputAndLabelEnabled(i != null);
        }

        @Override
        public URL getValue() {
            if (_guard.isSelected()) {
                try {
                    return new URL(_textField.getText());
                } catch (MalformedURLException e) {
                    return null;
                }
            }
            return null;
        }
    }

    protected static class EnumGUIOption extends GUIOption<Object> {
        private final JComboBox _comboBox;

        protected EnumGUIOption(Option<Object> option) {
            super(option);
            final OptionTypes.EnumType et = (OptionTypes.EnumType) option.getType();
            _comboBox = new JComboBox(et._values);
            _input = _comboBox;
            setValue(option.getValue());
        }

        @Override
        public void setValue(Object value) {
            _comboBox.setSelectedItem(value);
            _guard.setSelected(true);
        }

        @Override
        public Object getValue() {
            return _comboBox.getSelectedItem();
        }
    }

    protected static class FileGUIOption extends GUIOption<File> {
        private final JTextField _textField;
        private final JButton _fileChooserButton;
        private JFileChooser _fileChooser;

        protected FileGUIOption(Option<File> option) {
            super(option);
            _textField = new JTextField(TEXT_FIELD_COLUMNS);
            final JPanel inputPanel = new JPanel();
            _input = inputPanel;
            _fileChooserButton = new JButton(new AbstractAction("Select") {
                public void actionPerformed(ActionEvent e) {
                    if (_fileChooser == null) {
                        _fileChooser = new JFileChooser(new File("."));
                        _fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                        _fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                    }

                    final File file = new File(_textField.getText());
                    if (file.exists()) {
                        final File parent = file.getParentFile();
                        if (parent != null && parent.isDirectory()) {
                            _fileChooser.setCurrentDirectory(parent);
                        }
                        _fileChooser.ensureFileIsVisible(file);
                        _fileChooser.setSelectedFile(file);
                    }
                    if (_fileChooser.showDialog(_fileChooserButton, "Select") == JFileChooser.APPROVE_OPTION) {
                        _textField.setText(_fileChooser.getSelectedFile().getAbsolutePath());
                    }
                }
            });
            inputPanel.add(_textField);
            inputPanel.add(_fileChooserButton);
            setValue(option.getValue());
        }

        @Override
        public void setValue(File i) {
            if (i != null) {
                if (_fileChooser != null) {
                    _fileChooser.setSelectedFile(i);
                }
                _textField.setText(i.getAbsolutePath());
            }
            _guard.setSelected(i != null);
            setInputAndLabelEnabled(i != null);
        }

        @Override
        public File getValue() {
            return new File(_textField.getText());
        }

    }

    protected static class PackageGUIOption extends GUIOption<MaxPackage> {
        private final JComboBox _values;

        protected PackageGUIOption(Option<MaxPackage> option) {
            super(option);

            final MaxPackageOptionType type = (MaxPackageOptionType) option.getType();

            final AppendableSequence<MaxPackage> maxPackages = new LinkSequence<MaxPackage>();
            for (MaxPackage maxPackage : type._superPackage.getTransitiveSubPackages(Classpath.fromSystem())) {
                final Class<Scheme> schemeClass = StaticLoophole.cast(type._classType);
                if (maxPackage.schemeTypeToImplementation(schemeClass) != null) {
                    maxPackages.append(maxPackage);
                }
            }

            final MaxPackage[] values = Sequence.Static.toArray(maxPackages, MaxPackage.class);
            Arrays.sort(values, new Comparator<MaxPackage>() {
                public int compare(MaxPackage o1, MaxPackage o2) {
                    return o1.name().compareTo(o2.name());
                }
            });

            _values = new JComboBox(values);
            _input = _values;

            // The combo box must be editable as the prepopulated items are just those packages found from the super package
            _values.setEditable(true);

            setValue(option.getValue());
        }

        @Override
        public MaxPackage getValue() {
            return _option.getType().parseValue(_values.getSelectedItem().toString());
        }

        @Override
        public void setValue(MaxPackage p) {
            if (p != null) {
                _values.setSelectedItem(p);
                _guard.setSelected(true);
            } else {
                _values.setSelectedIndex(-1);
                _guard.setSelected(false);
            }
        }
    }

    private static GUIOption createGUIOption(Option<?> option) {
        final Option.Type<?> type = option.getType();
        if (type == OptionTypes.BOOLEAN_TYPE) {
            final Option<Boolean> opt = OptionTypes.BOOLEAN_TYPE.cast(option);
            return new BooleanGUIOption(opt);
        }
        if (type == OptionTypes.INT_TYPE) {
            final Option<Integer> opt = OptionTypes.INT_TYPE.cast(option);
            return new IntegerGUIOption(opt);
        }
        if (type == OptionTypes.STRING_TYPE) {
            final Option<String> opt = OptionTypes.STRING_TYPE.cast(option);
            return new StringGUIOption(opt);
        }
        if (type == OptionTypes.URL_TYPE) {
            final Option<URL> opt = OptionTypes.URL_TYPE.cast(option);
            return new URLGUIOption(opt);
        }
        if (type == OptionTypes.FILE_TYPE) {
            final Option<File> opt = OptionTypes.FILE_TYPE.cast(option);
            return new FileGUIOption(opt);
        }
        if (type instanceof OptionTypes.EnumType) {
            final Option<Object> opt = StaticLoophole.cast(option);
            return new EnumGUIOption(opt);
        }
        if (type instanceof OptionTypes.ListType) {
            final Option<List<Object>> opt = StaticLoophole.cast(option);
            return new ListGUIOption(opt);
        }
        if (type instanceof MaxPackageOptionType) {
            final Option<MaxPackage> opt = StaticLoophole.cast(option);
            return new PackageGUIOption(opt);
        }
        return null;
    }

    private boolean _cancelled;

    public OptionsDialog(JFrame owner, final OptionSet optionSet) {
        super(owner, "Options Dialog", true);

        final LinkedList<GUIOption<?>> guiOptions = new LinkedList<GUIOption<?>>();

        final Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        // Configure panel of options
        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new SpringLayout());
        optionsPanel.setBorder(BorderFactory.createTitledBorder(""));

        for (final Option<?> programOption : optionSet.getOptions()) {
            final GUIOption guiOption = createGUIOption(programOption);
            if (guiOption != null) {
                guiOptions.add(guiOption);

                final JCheckBox enabled = guiOption.getGuard();
                final JLabel label = guiOption.getLabel();

                label.setToolTipText(programOption.getHelp());

                optionsPanel.add(enabled);
                optionsPanel.add(label);
                optionsPanel.add(guiOption.getInputComponent());
            }
        }

        SpringUtilities.makeCompactGrid(optionsPanel, guiOptions.size(), 3, 0, 0, 5, 5);
        contentPane.add(optionsPanel, BorderLayout.CENTER);

        // Configure buttons
        final JButton ok = new JButton(new AbstractAction("OK") {
            public void actionPerformed(ActionEvent e) {
                for (GUIOption guiOption : guiOptions) {
                    try {
                        guiOption.commitOption();
                    } catch (Option.Error e1) {
                        showErrorDialog(guiOption, e1);
                        return;
                    }
                }
                OptionsDialog.this.setVisible(false);
            }
        });

        final JButton cancel = new JButton(new AbstractAction("Use Defaults") {
            public void actionPerformed(ActionEvent e) {
                OptionsDialog.this.setVisible(false);
                OptionsDialog.this._cancelled = true;
            }
        });

        final JButton exit = new JButton(new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent e) {
                System.exit(1);
            }
        });

        ok.setToolTipText("Apply options as configured in this dialog and ignore the command line arguments.");
        cancel.setToolTipText("Ignore options as configured in this dialog and use the command line arguments instead.");
        exit.setToolTipText("Stop running the program [calls System.exit()].");

        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(ok);
        buttonPanel.add(cancel);
        buttonPanel.add(exit);

        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        pack();

        // Places dialog in the middle of the screen if 'owner == null'
        setLocationRelativeTo(owner);
    }

    private class ErrorDialog extends JDialog {
        ErrorDialog(JDialog owner, GUIOption guiOption, Option.Error error) {
            super(owner, "Option Error", true);
            final JPanel errorPanel = new JPanel();
            errorPanel.setLayout(new SpringLayout());
            errorPanel.setBorder(BorderFactory.createTitledBorder(""));

            final Container contentPane = getContentPane();
            contentPane.setLayout(new BorderLayout());

            // Configure buttons
            final JButton ok = new JButton(new AbstractAction("OK") {
                public void actionPerformed(ActionEvent e) {
                    errorPanel.setVisible(false);
                }
            });
            final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.add(ok);

            final JLabel textPane = new JLabel();
            textPane.setText("Invalid value for " + guiOption._option.getType().getTypeName() + " option \"-" + guiOption._option.getName() + "\"");
            contentPane.add(textPane);

            contentPane.add(buttonPanel, BorderLayout.SOUTH);
            pack();

            // Places dialog in the middle of the screen if 'owner == null'
            setLocationRelativeTo(owner);
        }
    }

    public void showErrorDialog(GUIOption guiOption, Option.Error error) {
        final String typeName = guiOption._option.getType().getTypeName();
        JOptionPane.showMessageDialog(this, "Error in option \"-" + guiOption._option.getName() + "\" of type " + typeName + "\n" + error.getMessage());
//        new ErrorDialog(this, guiOption, error).setVisible(true);
    }

    /**
     * Creates a dialog for displaying a GUI for a given option set.
     *
     * @return true if the user wants to use the options as configured by the dialog, false if the caller of this method
     *         should defer to parsing an argument array instead
     */
    public static boolean show(JFrame owner, OptionSet optionSet) {
        final OptionsDialog programOptionDialog = new OptionsDialog(owner, optionSet);
        programOptionDialog.setVisible(true);
        programOptionDialog.dispose();
        return !programOptionDialog._cancelled;
    }
}
